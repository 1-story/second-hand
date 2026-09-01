package com.hdu.secondhand.util;

/**
 * JWT Token 解析服务（预留接口，由登录模块实现）
 *
 * <p>《接口约定规范 v1.0》：登录成功后签发 JWT，请求头 Authorization: Bearer &lt;token&gt;，
 * 载荷含 sub(userId)。本接口由陈思瀚登录模块提供真实实现（解析 JWT + 环境变量 JWT_SECRET），
 * 当前提供 {@link NoopJwtTokenService} 占位（未接入前返回 null，走 X-User-Id 兼容通道）。</p>
 */
public interface JwtTokenService {

    /**
     * 解析 token 返回 userId；token 无效/过期返回 null
     */
    Long parseUserId(String token);
}
