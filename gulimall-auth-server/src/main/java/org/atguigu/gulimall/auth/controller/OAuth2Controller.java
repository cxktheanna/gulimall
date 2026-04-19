package org.atguigu.gulimall.auth.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.constant.auth.AuthConstant;
import com.atguigu.common.utils.HttpUtils;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.auth.MemberResponseVO;
import com.atguigu.common.vo.auth.WBSocialUserVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.atguigu.gulimall.auth.agent.MemberAgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 社交登录
 *
 * @Author: wanzenghui
 * @Date: 2021/11/26 22:26
 */
@Slf4j
@Controller
public class OAuth2Controller {

    @Autowired
    MemberAgentService memberAgentService;

    /**
     * 授权回调页
     *
     * @param code 根据code换取Access Token，且code只能兑换一次Access Token
     */
    @GetMapping("/oauth2.0/gitee/success")
    public String gitee(@RequestParam("code") String code, HttpSession session, HttpServletResponse servletResponse) throws Exception {
        // 1. 根据code换取Gitee Access Token
        Map<String, String> headers = new HashMap<>();
        Map<String, String> querys = new HashMap<>();
        Map<String, String> map = new HashMap<>();

        map.put("client_id", "313e222e54d55ecb3c8a439853a430a20ef6a922c9eba4ce11e9668ed4381098");
        map.put("client_secret", "5a9174fd116be6b37d85e49a4499f1227fc728331394e9ec40b760e9e5c55de1");
        map.put("grant_type", "authorization_code");
        map.put("redirect_uri", "http://auth.gulimall.com/oauth2.0/gitee/success");
        map.put("code", code);

        // ======================= 只改了这里 =======================
        HttpResponse response = HttpUtils.doPost(
                "https://gitee.com",   // 换成gitee
                "/oauth/token",       // 换成gitee
                headers,
                querys,
                map
        );
        // ==========================================================

        // 2.处理请求返回 —— 完全和你原来微博代码一样！
        if (response.getStatusLine().getStatusCode() == 200) {
            String jsonString = EntityUtils.toString(response.getEntity());

            // ======================= 关键修复 =======================
            // 先用Map接收Gitee返回，再手动设置给你的VO
            Map<String, Object> giteeMap = JSON.parseObject(jsonString, Map.class);
            WBSocialUserVO user = new WBSocialUserVO();
            user.setAccess_token((String) giteeMap.get("access_token"));
            user.setExpires_in((Long) giteeMap.get("expires_in"));
            user.setUid(giteeMap.get("id").toString()); // gitee的用户id叫id，赋值给uid
            // ==========================================================

            // ======================= 以下完全不动，原样保留 =======================
            R r = memberAgentService.oauthLogin(user);
            if (r.getCode() == 0) {
                MemberResponseVO loginUser = r.getData(new TypeReference<MemberResponseVO>() {});
                session.setAttribute(AuthConstant.LOGIN_USER, loginUser);
                return "redirect:http://gulimall.com";
            } else {
                return "redirect:http://auth.gulimall.com/login.html";
            }
        } else {
            return "redirect:http://auth.gulimall.com/login.html";
        }
    }

}