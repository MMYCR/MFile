package org.example.myselffile.module.model.result;

import lombok.Data;
import java.io.Serializable;
@Data
    //统一JSON响应结果
public class AjaxJson<T> implements Serializable {
    //请求是否成功
    private Boolean success = true; // 修改为Boolean包装类型

    //状态码
    private int code = 200;

    //返回消息
    private String message = "操作成功!";

    //返回数据
    private T data;

    //跟踪 ID
    private String traceId;


    public static <T> AjaxJson<T> getSuccess() {
        return new AjaxJson<>();
    }

    public static <T> AjaxJson<T> getSuccess(String message) {
        AjaxJson<T> ajaxJson = new AjaxJson<>();
        ajaxJson.setMessage(message);
        return ajaxJson;
    }

    public static <T> AjaxJson<T> getSuccess(String message, T data) {
        AjaxJson<T> ajaxJson = new AjaxJson<>();
        ajaxJson.setMessage(message);
        ajaxJson.setData(data);
        return ajaxJson;
    }

    public static <T> AjaxJson<T> getSuccessData(T data) {
        AjaxJson<T> ajaxJson = new AjaxJson<>();
        ajaxJson.setData(data);
        return ajaxJson;
    }

    public static <T> AjaxJson<T> getError(String message) {
        AjaxJson<T> ajaxJson = new AjaxJson<>();
        ajaxJson.setSuccess(false); // 仍然使用 setSuccess
        ajaxJson.setMessage(message);
        ajaxJson.setCode(500); // 默认错误码
        return ajaxJson;
    }

    public static <T> AjaxJson<T> getError(int code, String message) {
        AjaxJson<T> ajaxJson = new AjaxJson<>();
        ajaxJson.setSuccess(false); // 仍然使用 setSuccess
        ajaxJson.setMessage(message);
        ajaxJson.setCode(code);
        return ajaxJson;
    }
}