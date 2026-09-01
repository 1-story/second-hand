package com.hdu.secondhand.common;

import lombok.Getter;

/**
 * 业务异常：由全局异常处理器转换为 { code, message, data:null }，并携带对应 HTTP 状态码。
 */
@Getter
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;
    private final int httpStatus;

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
        this.httpStatus = resultCode.getHttpStatus();
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
        this.httpStatus = resultCode.getHttpStatus();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatusOf(code);
    }

    /** 按错误码段推断 HTTP 状态码：400xx→400, 401xx→401, 403xx→403, 404xx→404, 429xx→429, 500xx→500, 504xx→504 */
    private static int httpStatusOf(int code) {
        if (code == 0) {
            return 200;
        }
        if (code >= 50400) {
            return 504;
        }
        if (code >= 50000) {
            return 500;
        }
        if (code >= 42900) {
            return 429;
        }
        if (code >= 40400) {
            return 404;
        }
        if (code >= 40300) {
            return 403;
        }
        if (code >= 40100) {
            return 401;
        }
        return 400;
    }
}
