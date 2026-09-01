package com.hdu.secondhand.common;

import lombok.Getter;

/**
 * 统一响应码
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权操作"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源状态冲突"),
    SERVER_ERROR(500, "服务器内部错误"),

    // 业务码 1000+
    USER_NOT_FOUND(1001, "用户不存在"),
    PRODUCT_NOT_FOUND(1002, "商品不存在或已下架"),
    PRODUCT_NOT_OWNER(1003, "只能操作自己发布的商品"),
    PRODUCT_STATUS_INVALID(1004, "商品状态不允许该操作"),
    CATEGORY_NOT_FOUND(1005, "商品分类不存在"),
    FAVORITE_EXISTS(1006, "已收藏该商品"),
    FAVORITE_NOT_EXISTS(1007, "未收藏该商品"),
    AI_ESTIMATE_FAILED(1101, "AI 估价失败"),
    AI_LLM_NOT_CONFIGURED(1102, "大模型服务未配置或未启用"),
    AI_DRAFT_NOT_FOUND(1103, "AI 草稿不存在"),
    AI_DRAFT_STATUS_INVALID(1104, "AI 草稿状态不允许发布"),
    PARAM_ERROR(1200, "参数校验失败");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
