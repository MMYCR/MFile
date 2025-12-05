package org.example.myselffile.core.exception;


import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // ---------- 通用错误 (1000~1999) ----------
    PARAMS_ERROR(1000, "参数错误", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(1001, "未授权访问", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(1002, "拒绝访问", HttpStatus.FORBIDDEN),
    NOT_FOUND(1003, "资源不存在", HttpStatus.NOT_FOUND),
    INTERNAL_ERROR(1004, "系统内部错误", HttpStatus.INTERNAL_SERVER_ERROR),

    // ---------- 认证模块 (2000~2999) ----------
    AUTH_USER_NOT_FOUND(2000, "用户不存在", HttpStatus.UNAUTHORIZED),
    AUTH_PASSWORD_INVALID(2001, "密码错误", HttpStatus.UNAUTHORIZED), // 旧密码错误也用此
    AUTH_ACCOUNT_LOCKED(2002, "账户已锁定", HttpStatus.LOCKED),
    AUTH_CAPTCHA_INVALID(2003, "验证码错误", HttpStatus.BAD_REQUEST),


    // ---------- 业务模块错误（3000~3999） ----------
    ORDER_OUT_OF_STOCK(3000, "库存不足", HttpStatus.BAD_REQUEST),
    PAYMENT_FAILED(3001, "支付失败", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_STORAGE_SOURCE(3002, "无效的存储源", HttpStatus.BAD_REQUEST),

    // ---------- 用户管理错误 (4000~4999) ----------
    BIZ_OLD_PASSWORD_CANNOT_BE_EMPTY(4000, "旧密码不能为空", HttpStatus.BAD_REQUEST), // 统一到用户管理错误码范围
    BIZ_PASSWORD_NOT_SAME(4001, "新密码和确认密码不一致", HttpStatus.BAD_REQUEST),
    BIZ_NEW_PASSWORD_CANNOT_BE_SAME_AS_OLD(4002, "新密码不能与旧密码相同", HttpStatus.BAD_REQUEST),
    BIZ_USERNAME_ALREADY_EXISTS(4003, "用户名已存在", HttpStatus.CONFLICT), // 预留或根据需要启用
    BIZ_USER_NOT_EXIST(4004, "用户不存在或无权操作", HttpStatus.NOT_FOUND),// 移到用户管理错误码范围
    BIZ_UPLOAD_FILE_NOT_EMPTY(4005,"文件不能为空",HttpStatus.BAD_REQUEST),
    BIZ_UPLOAD_FILE_SIZE_EXCEEDED(4006,"文件不能过大",HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
    private final ErrorType type;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this(code, message, httpStatus, ErrorType.BUSINESS);
    }

    ErrorCode(int code, String message, HttpStatus httpStatus, ErrorType type) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
        this.type = type;
    }

    // Getters ...

    public enum ErrorType {
        CLIENT,    // 客户端错误（如参数错误）
        BUSINESS,  // 业务逻辑错误
        SYSTEM     // 系统级错误（如数据库连接失败）
    }
}
