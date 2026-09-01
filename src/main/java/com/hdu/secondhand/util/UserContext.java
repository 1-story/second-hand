package com.hdu.secondhand.util;

import com.hdu.secondhand.common.BizException;
import com.hdu.secondhand.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 当前用户解析工具
 *
 * <p>集成约定：登录/权限模块（陈思瀚）就绪前，前端在请求头携带
 * {@code X-User-Id} 标识当前用户；登录模块接入后替换为 Token 解析，
 * 其余代码无需改动。</p>
 */
public final class UserContext {

    /** 请求头名称：当前用户 ID（临时方案，待登录模块替换） */
    public static final String HEADER_USER_ID = "X-User-Id";

    /** 未登录时使用的默认测试用户（种子数据 id=1 田博） */
    public static final long DEFAULT_USER_ID = 1L;

    private UserContext() {
    }

    /**
     * 获取当前用户 ID；未携带请求头时返回默认测试用户。
     * 登录模块接入后此处应改为 401 拦截。
     */
    public static long currentUserId() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return DEFAULT_USER_ID;
        }
        HttpServletRequest request = attrs.getRequest();
        String header = request.getHeader(HEADER_USER_ID);
        if (header == null || header.isBlank()) {
            return DEFAULT_USER_ID;
        }
        try {
            long id = Long.parseLong(header.trim());
            if (id <= 0) {
                throw new NumberFormatException();
            }
            return id;
        } catch (NumberFormatException e) {
            throw new BizException(ResultCode.UNAUTHORIZED, "无效的用户标识");
        }
    }
}
