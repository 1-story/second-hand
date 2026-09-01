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
 * <p>对齐《接口约定规范 v1.0》第 4 节：
 * <ol>
 *   <li>优先解析 {@code Authorization: Bearer &lt;token&gt;}（JWT，登录模块接入后生效）；</li>
 *   <li>开发期兼容 {@code X-User-Id} 请求头；</li>
 *   <li>两者都无时返回默认测试用户（种子数据 id=1 田博）。</li>
 * </ol>
 * 登录模块就绪后替换 {@link NoopJwtTokenService} 为真实 JWT 解析即可。</p>
 */
@Component
public class UserContext {

    /** 请求头名称：当前用户 ID（开发期兼容，登录模块接入后移除） */
    public static final String HEADER_USER_ID = "X-User-Id";

    /** 未登录时使用的默认测试用户（种子数据 id=1 田博） */
    public static final long DEFAULT_USER_ID = 1L;

    private static JwtTokenService jwtTokenService;

    @Autowired
    public void setJwtTokenService(JwtTokenService service) {
        UserContext.jwtTokenService = service;
    }

    /**
     * 获取当前用户 ID：Bearer Token → X-User-Id → 默认测试用户。
     * 登录模块接入后，未认证请求应在此返回 40100。
     */
    public static long currentUserId() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return DEFAULT_USER_ID;
        }
        HttpServletRequest request = attrs.getRequest();

        // 1. Bearer Token（JWT）
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7).trim();
            if (!token.isBlank() && jwtTokenService != null) {
                Long userId = jwtTokenService.parseUserId(token);
                if (userId != null && userId > 0) {
                    return userId;
                }
            }
        }

        // 2. 开发期兼容 X-User-Id
        String header = request.getHeader(HEADER_USER_ID);
        if (header != null && !header.isBlank()) {
            try {
                long id = Long.parseLong(header.trim());
                if (id > 0) {
                    return id;
                }
            } catch (NumberFormatException ignored) {
                // 无效头，落到默认用户
            }
        }
        return DEFAULT_USER_ID;
    }
}
