package org.example.myselffile.module.user.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateUserPasswordRequest {
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).+$", message = "新密码必须包含字母和数字")
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).+$", message = "确认密码必须包含字母和数字")
    private String newPasswordConfig;
}