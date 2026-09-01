package com.hdu.secondhand.util;

import com.hdu.secondhand.common.BizException;
import com.hdu.secondhand.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 当前用户解析工具
 *
 * <p>对齐《接口约定规范 v1.1》4.2：身份唯一来源为 {@code Authorization: Bearer <JWT>}，
 * 由登录模块拦截器解析注入（{@link JwtTokenService}），<b>禁止使用 X-User-Id 等自定义头传身份（防伪造）</b>。</p>
 *
 * <p>当前状态：JWT 拦截器（陈思瀚）接入前，无 Token 时返回默认测试用户（种子数据 id=1 田博），
 * 便于开发联调；拦截器接入后，需登录的接口在未携带合法 Token 时应返回 40100。</p>
 */
@Component
public class UserContext {

    /** 未登录时使用的默认测试用户（种子数据 id=1 田博；开发期联调用，拦截器接入后收紧） */
    public static final long DEFAULT_USER_ID = 1L;

    private static JwtTokenService jwtTokenService;

    @Autowired
    public void setJwtTokenService(JwtTokenService service) {
        UserContext.jwtTokenService = service;
    }

    /**
     * 获取当前用户 ID：Bearer Token（JWT）→ 默认测试用户（开发期）。
     * 登录模块拦截器接入后，未认证请求应在此返回 40100。
     */
    public static long currentUserId() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return DEFAULT_USER_ID;
        }
        HttpServletRequest request = attrs.getRequest();

        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7).trim();
            if (!token.isBlank() && jwtTokenService != null) {
                Long userId = jwtTokenService.parseUserId(token);
                if (userId != null && userId > 0) {
                    return userId;
                }
                throw new BizException(ResultCode.TOKEN_INVALID);
            }
        }
        // 开发期兜底（无拦截器）；拦截器接入后改为抛 40100（未登录）
        return DEFAULT_USER_ID;
    }
}
