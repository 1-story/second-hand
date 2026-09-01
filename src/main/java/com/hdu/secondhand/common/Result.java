package com.hdu.secondhand.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回体：{ code, message, data }
 * 对齐《接口约定规范 v1.0》：code=0 表示成功；失败为业务错误码；data 无数据返回 null。
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private int code;
    private String message;
    private T data;

    public static <T> Result<T> ok() {
        return build(ResultCode.SUCCESS, null);
    }

    public static <T> Result<T> ok(T data) {
        return build(ResultCode.SUCCESS, data);
    }

    public static <T> Result<T> fail(ResultCode resultCode) {
        return build(resultCode, null);
    }

    public static <T> Result<T> fail(ResultCode resultCode, String message) {
        Result<T> result = build(resultCode, null);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    private static <T> Result<T> build(ResultCode resultCode, T data) {
        Result<T> result = new Result<>();
        result.setCode(resultCode.getCode());
        result.setMessage(resultCode.getMessage());
        result.setData(data);
        return result;
    }
}
