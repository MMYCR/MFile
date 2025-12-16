package org.example.myselffile.core.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import lombok.extern.slf4j.Slf4j;
import org.example.myselffile.module.model.result.AjaxJson;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.text.MessageFormat;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 1. 捕获自定义业务异常 (BizException)
     */
    @ExceptionHandler(BizException.class)
    public AjaxJson<Void> handleBizException(BizException e) {
        log.warn("业务异常: {}", e.getMessage());

        // 如果异常里包含 ErrorCode 枚举
        if (e.getErrorCode() != null) {
            String message = e.getErrorCode().getMessage();

            // 如果有动态参数 (args)，使用 MessageFormat 进行格式化
            if (e.getArgs() != null && e.getArgs().length > 0) {
                try {
                    message = MessageFormat.format(message, e.getArgs());
                } catch (Exception ex) {
                    log.error("消息格式化失败", ex);
                }
            }
            return AjaxJson.getError(e.getErrorCode().getCode(), message);
        }

        // 如果只是普通的 BizException(msg)
        return AjaxJson.getError(e.getMessage());
    }

    /**
     * 2. 捕获 Sa-Token 未登录异常
     * 当拦截器发现没有 Token 时抛出
     */
    @ExceptionHandler(NotLoginException.class)
    public AjaxJson<Void> handleNotLoginException(NotLoginException nle) {
        // 返回 401 状态码，前端拦截器会识别并跳转登录页
        return AjaxJson.getError(401, "未登录，请先登录");
    }

    /**
     * 3. 捕获 Sa-Token 无权限异常 (如果你用了角色权限控制)
     */
    @ExceptionHandler(NotPermissionException.class)
    public AjaxJson<Void> handleNotPermissionException(NotPermissionException npe) {
        return AjaxJson.getError(403, "无权访问该资源");
    }

    /**
     * 4. 捕获参数校验异常 (@Validated / @NotBlank)
     * 比如注册时密码为空，Spring 会抛出这个异常
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public AjaxJson<Void> handleValidationException(Exception e) {
        String msg = "参数校验失败";
        ObjectError objectError = null;

        if (e instanceof MethodArgumentNotValidException) {
            objectError = ((MethodArgumentNotValidException) e).getBindingResult().getAllErrors().get(0);
        } else if (e instanceof BindException) {
            objectError = ((BindException) e).getBindingResult().getAllErrors().get(0);
        }

        if (objectError != null) {
            msg = objectError.getDefaultMessage(); // 获取注解上的 message，例如 "新密码不能为空"
        }

        // 返回错误码 1000 (PARAMS_ERROR)
        return AjaxJson.getError(ErrorCode.PARAMS_ERROR.getCode(), msg);
    }

    /**
     * 5. 捕获所有其他未知异常 (兜底)
     */
    @ExceptionHandler(Exception.class)
    public AjaxJson<Void> handleException(Exception e) {
        log.error("系统内部异常", e); // 打印堆栈到控制台，方便调试
        return AjaxJson.getError(ErrorCode.INTERNAL_ERROR.getCode(), "系统繁忙，请稍后重试");
    }
}