package org.example.myselffile.core.exception;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {
    private ErrorCode errorCode;
    private Object[] args;

    public BizException(String message) {
        super(message);
    }
    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    public BizException(ErrorCode errorCode, Object... args) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.args = args;
    }

}