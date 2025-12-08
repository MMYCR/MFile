package org.example.myselffile.module.user.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserLoginRequest {

    //  用户名
    @NotBlank(message = "用户名不能为空")
    private String username;

    //  密码
    @NotBlank(message = "密码不能为空")
    private String password;
}