package com.atguigu.gulimall.member.service;

import com.atguigu.common.to.member.MemberUserLoginTO;
import com.atguigu.common.to.member.MemberUserRegisterTO;
import com.atguigu.common.to.member.WBSocialUserTO;
import com.atguigu.gulimall.member.exception.PhoneException;
import com.atguigu.gulimall.member.exception.UsernameException;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.member.entity.MemberEntity;

import java.util.Map;

/**
 * 会员
 *
 * @author dcq
 * @email 1193882675@qq.com
 * @date 2026-01-13 19:40:50
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void regist(MemberUserRegisterTO user) throws InterruptedException;

    /**
     * 校验手机号是否唯一
     */
    void checkPhoneUnique(String phone) throws PhoneException;

    /**
     * 校验用户名是否唯一
     */
    void checkUserNameUnique(String userName) throws UsernameException;

    MemberEntity login(MemberUserLoginTO user);

    /**
     * 微博社交登录（登录和注册功能合并）
     */
    MemberEntity login(WBSocialUserTO user) throws Exception;
}

