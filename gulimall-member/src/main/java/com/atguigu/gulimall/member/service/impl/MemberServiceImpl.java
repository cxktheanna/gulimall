package com.atguigu.gulimall.member.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.common.constant.member.MemberConstant;
import com.atguigu.common.to.member.MemberUserLoginTO;
import com.atguigu.common.to.member.MemberUserRegisterTO;
import com.atguigu.common.to.member.WBSocialUserTO;
import com.atguigu.common.utils.HttpUtils;
import com.atguigu.gulimall.member.entity.MemberLevelEntity;
import com.atguigu.gulimall.member.exception.PhoneException;
import com.atguigu.gulimall.member.exception.UsernameException;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.member.dao.MemberDao;
import com.atguigu.gulimall.member.entity.MemberEntity;
import com.atguigu.gulimall.member.service.MemberService;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    MemberLevelServiceImpl memberLevelService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 注册
     */
    @Override
    public void regist(MemberUserRegisterTO user) throws InterruptedException {
        // 1.加锁
        RLock lock = redissonClient.getLock(MemberConstant.LOCK_KEY_REGIST_PRE + user.getPhone());
        try {
            lock.tryLock(30L, TimeUnit.SECONDS);
            // 2.校验
            // 校验手机号唯一、用户名唯一
            checkPhoneUnique(user.getPhone());
            checkUserNameUnique(user.getUserName());
            // 3.封装保存
            MemberEntity entity = new MemberEntity();
            entity.setUsername(user.getUserName());
            entity.setMobile(user.getPhone());
            entity.setNickname(user.getUserName());
            // 3.1.设置默认等级信息
            MemberLevelEntity level = memberLevelService.getDefaultLevel();
            entity.setLevelId(level.getId());
            // 3.2.设置密码加密存储
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            String encode = passwordEncoder.encode(user.getPassword());
            entity.setPassword(encode);
            entity.setCreateTime(new Date());
            this.baseMapper.insert(entity);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 校验手机号是否唯一
     */
    public void checkPhoneUnique(String phone) throws PhoneException {
        Integer count = baseMapper.selectCount(new QueryWrapper<MemberEntity>()
                .eq("mobile", phone));
        if (count > 0) {
            throw new PhoneException();
        }
    }

    /**
     * 校验用户名是否唯一
     */
    public void checkUserNameUnique(String userName) throws UsernameException {
        Integer count = baseMapper.selectCount(new QueryWrapper<MemberEntity>()
                .eq("username", userName));
        if (count > 0) {
            throw new UsernameException();
        }
    }

    /**
     * 登录
     */
    @Override
    public MemberEntity login(MemberUserLoginTO user) {
        String loginacct = user.getLoginacct();
        String password = user.getPassword();// 明文

        // 1.查询MD5密文
        MemberEntity entity = baseMapper.selectOne(new QueryWrapper<MemberEntity>()
                .eq("username", loginacct)
                .or()
                .eq("mobile", loginacct));
        if (entity != null) {
            // 2.获取password密文进行校验
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            if (passwordEncoder.matches(password, entity.getPassword())) {
                // 登录成功
                return entity;
            }
        }
        // 3.登录失败
        return null;
    }

    /**
     * 微博社交登录（登录和注册功能合并）
     */
    @Override
    public MemberEntity login(WBSocialUserTO user) throws Exception {
        String giteeUserId = null;
        String name = null;
        String avatarUrl = null;

        try {
            // 获取 Gitee 用户信息（唯一能拿到 id 的地方）
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put("access_token", user.getAccessToken());

            HttpResponse response = HttpUtils.doGet(
                    "https://gitee.com",
                    "/api/v5/user",
                    new HashMap<>(),
                    queryMap
            );

            if (response.getStatusLine().getStatusCode() == 200) {
                String json = EntityUtils.toString(response.getEntity());
                JSONObject jsonObject = JSON.parseObject(json);

                // 核心字段
                giteeUserId = jsonObject.getString("id");
                name = jsonObject.getString("name");
                avatarUrl = jsonObject.getString("avatar_url");
            }
        } catch (Exception e) {
            throw new Exception("获取Gitee用户信息失败", e);
        }

        // 查询是否已注册
        MemberEntity dbMember = baseMapper.selectOne(
                new QueryWrapper<MemberEntity>().eq("weibo_uid", giteeUserId)
        );

        if (dbMember != null) {
            dbMember.setAccessToken(user.getAccessToken());
            dbMember.setExpiresIn(user.getExpiresIn());
            baseMapper.updateById(dbMember);
            return dbMember;
        }

        // 新用户注册
        MemberEntity newMember = new MemberEntity();
        newMember.setNickname(name);
        newMember.setHeader(avatarUrl);
        newMember.setGender(0);
        newMember.setCreateTime(new Date());
        newMember.setWeiboUid(giteeUserId);
        newMember.setAccessToken(user.getAccessToken());
        newMember.setExpiresIn(user.getExpiresIn());

        baseMapper.insert(newMember);
        return newMember;
    }

}