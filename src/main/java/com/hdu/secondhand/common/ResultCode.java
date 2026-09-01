package com.hdu.secondhand.common;

import lombok.Getter;

/**
 * 统一响应码（对齐《接口约定规范 v1.0》：成功 0，失败按段位 400xx/401xx/403xx/404xx/429xx/500xx）
 * 每个枚举同时携带对应的 HTTP 状态码，供全局异常处理器返回真实 HTTP 状态。
 */
@Getter
public enum ResultCode {

    SUCCESS(0, "success", 200),

    // ---- 400xx 通用参数与业务校验 ----
    BAD_REQUEST(40000, "参数错误", 400),
    PARAM_ERROR(40000, "参数错误", 400),
    PARAM_MISSING(40001, "必填参数缺失", 400),
    PARAM_FORMAT(40002, "参数格式错误", 400),
    ALREADY_EXISTS(40003, "数据已存在", 400),
    DATA_NOT_EXIST(40004, "数据不存在", 400),
    STATUS_CONFLICT(40005, "当前状态不允许该操作", 400),
    REAL_NAME_FAILED(40010, "学号、姓名或专业信息不一致，实名认证失败", 400),
    NOT_SCHOOL_MEMBER(40011, "非本校师生，无法注册", 400),
    PASSWORD_MISMATCH(40012, "两次输入的密码不一致", 400),

    // ---- 401xx 认证错误 ----
    UNAUTHORIZED(40100, "未登录或登录已失效", 401),
    TOKEN_INVALID(40101, "Token 无效或已过期", 401),
    LOGIN_FAILED(40102, "账号或密码错误", 401),
    ACCOUNT_DISABLED(40103, "账号已被禁用", 401),

    // ---- 403xx 授权 / 资格错误 ----
    FORBIDDEN(40300, "无权限执行该操作", 403),
    REAL_NAME_REQUIRED(40301, "请先完成学号实名认证", 403),
    CREDIT_NOT_ENOUGH(40302, "信用分不足，暂时无法发布/交易", 403),

    // ---- 404xx 资源不存在 ----
    NOT_FOUND(40400, "资源不存在", 404),
    PRODUCT_NOT_FOUND(40401, "商品不存在或已下架", 404),
    ORDER_NOT_FOUND(40402, "订单不存在", 404),

    // ---- 429xx 限流 ----
    RATE_LIMITED(42900, "请求过于频繁，请稍后再试", 429),
    AI_BUSY(42901, "AI 服务繁忙，请稍后再试", 429),

    // ---- 500xx 服务端错误 ----
    SERVER_ERROR(50000, "服务器开小差了，请稍后再试", 500),
    DB_ERROR(50001, "数据库操作失败", 500),
    AI_FALLBACK(50002, "AI 服务暂不可用，已使用规则引擎结果", 500),
    IMAGE_PARSE_FAILED(50003, "图片解析失败", 500),

    // ---- 504 超时 ----
    TIMEOUT(50400, "服务处理超时", 504),

    // ---- 业务别名（语义化，值对齐规范段位） ----
    USER_NOT_FOUND(40400, "用户不存在", 404),
    PRODUCT_NOT_OWNER(40300, "只能操作自己发布的商品", 403),
    PRODUCT_STATUS_INVALID(40005, "商品状态不允许该操作", 400),
    CATEGORY_NOT_FOUND(40400, "商品分类不存在", 404),
    FAVORITE_EXISTS(40000, "已收藏该商品", 400),
    FAVORITE_NOT_EXISTS(40000, "未收藏该商品", 400),
    AI_ESTIMATE_FAILED(50002, "AI 估价失败，已使用规则引擎结果", 500),
    AI_LLM_NOT_CONFIGURED(50002, "AI 服务暂不可用，已使用规则引擎结果", 500),
    AI_DRAFT_NOT_FOUND(40400, "AI 草稿不存在", 404),
    AI_DRAFT_STATUS_INVALID(40005, "AI 草稿状态不允许发布", 400);

    private final int code;
    private final String message;
    private final int httpStatus;

    ResultCode(int code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
