package com.hdu.secondhand.util;

import org.springframework.stereotype.Component;

/**
 * JWT 解析占位实现：登录模块（陈思瀚）接入前返回 null。
 * 接入后替换本类（或改为直接实现 {@link JwtTokenService}），其余代码无需改动。
 */
@Component
public class NoopJwtTokenService implements JwtTokenService {

    @Override
    public Long parseUserId(String token) {
        // TODO 待登录模块接入：JWT_SECRET 环境变量 + JJWT 解析 sub
        return null;
    }
}
