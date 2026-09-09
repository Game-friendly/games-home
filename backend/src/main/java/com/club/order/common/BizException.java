package com.club.order.common;

import lombok.Getter;

/** 业务异常，带业务错误码，由 GlobalExceptionHandler 统一转成 ApiResponse。 */
@Getter
public class BizException extends RuntimeException {
    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static BizException of(int code, String message) {
        return new BizException(code, message);
    }
}
