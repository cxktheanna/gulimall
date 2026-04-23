package org.atguigu.gulimall.cart.interceptor;

import com.atguigu.common.constant.auth.AuthConstant;
import com.atguigu.common.constant.cart.CartConstant;

import com.atguigu.common.to.cart.UserInfoTO;
import com.atguigu.common.vo.auth.MemberResponseVO;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.UUID;

import static com.atguigu.common.constant.auth.AuthConstant.LOGIN_USER;
import static com.atguigu.common.constant.cart.CartConstant.TEMP_USER_COOKIE_NAME;
import static com.atguigu.common.constant.cart.CartConstant.TEMP_USER_COOKIE_TIMEOUT;

/**
 * 判断用户登录状态，并封装用户信息传递给controller
 *
 * @Author: wanzenghui
 * @Date: 2021/12/5 0:32
 */
public class CartInterceptor implements HandlerInterceptor {
    public static ThreadLocal<UserInfoTO> toThreadLocal = new ThreadLocal<>();

    /***
     * 目标方法执行之前
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        UserInfoTO userInfoTo = new UserInfoTO();

        HttpSession session = request.getSession();
        //获得当前登录用户的信息
        MemberResponseVO memberResponseVo = (MemberResponseVO) session.getAttribute(LOGIN_USER);

        if (memberResponseVo != null) {
            //用户登录了
            userInfoTo.setUserId(memberResponseVo.getId());
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                //user-key
                String name = cookie.getName();
                if (name.equals(TEMP_USER_COOKIE_NAME)) {
                    userInfoTo.setUserKey(cookie.getValue());
                    //标记为已是临时用户
                    userInfoTo.setTempUser(true);
                }
            }
        }

        //如果没有临时用户一定分配一个临时用户
        if (StringUtils.isEmpty(userInfoTo.getUserKey())) {
            String uuid = UUID.randomUUID().toString();
            userInfoTo.setUserKey(uuid);
        }

        //目标方法执行之前
        toThreadLocal.set(userInfoTo);
        return true;
    }


    /**
     * 业务执行之后，分配临时用户来浏览器保存
     * @param request
     * @param response
     * @param handler
     * @param modelAndView
     * @throws Exception
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

        //获取当前用户的值
        UserInfoTO userInfoTo = toThreadLocal.get();

        //如果没有临时用户一定保存一个临时用户
        if (!userInfoTo.isTempUser()) {
            //创建一个cookie
            Cookie cookie = new Cookie(TEMP_USER_COOKIE_NAME, userInfoTo.getUserKey());
            //扩大作用域
            cookie.setDomain("gulimall.com");
            //设置过期时间
            cookie.setMaxAge(TEMP_USER_COOKIE_TIMEOUT);
            response.addCookie(cookie);
        }

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

    }
}