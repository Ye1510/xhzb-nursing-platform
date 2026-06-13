package com.xhzb.framework.interceptor;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import com.xhzb.common.exception.base.BaseException;
import com.xhzb.common.utils.StringUtils;
import com.xhzb.common.utils.UserThreadLocal;
import com.xhzb.framework.web.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

import static com.xhzb.common.constant.Constants.JWT_USERID;

@Component
public class MemberInterceptor implements HandlerInterceptor {

    @Autowired
    private TokenService tokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //获取token
        String token = request.getHeader("authorization");
        if(StringUtils.isEmpty(token)){
            throw new BaseException("认证失败");
        }
        //解析token
        Map<String, Object> claims =  tokenService.parseToken(token);
        if(ObjectUtil.isEmpty(claims)){
            throw new BaseException("认证失败");
        }
        Long userId = MapUtil.get(claims, JWT_USERID, Long.class);
        if(ObjectUtil.isEmpty(userId)){
            throw new BaseException("认证失败");
        }
        //把数据存储到线程中
        UserThreadLocal.set(userId);
        return true;

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserThreadLocal.remove();
    }
}
