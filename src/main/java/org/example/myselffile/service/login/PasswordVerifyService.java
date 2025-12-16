package org.example.myselffile.service.login;

import jakarta.annotation.Resource;
import org.example.myselffile.core.exception.BizException;
import org.example.myselffile.core.exception.ErrorCode;
import org.example.myselffile.module.user.model.eneity.User;
import org.example.myselffile.module.user.model.request.UserLoginRequest;
import org.example.myselffile.module.user.service.UserService;
import org.example.myselffile.utils.PasswordUtils;
import org.springframework.stereotype.Service;

@Service
public class PasswordVerifyService implements LoginVerifyService{

    @Resource
    private UserService userService;

    @Override
    public void verify(UserLoginRequest userLoginRequest) {
        User userVerify=userService.getUserByName(userLoginRequest.getUsername());
        if(userVerify == null)
            throw new BizException(ErrorCode.AUTH_USER_NOT_FOUND);
        String dbEncodedPassword = userVerify.getPassword();

        //  使用 PasswordUtils.verify() 方法验证用户输入的明文密码与数据库中的加密密码是否匹配
        // userLoginRequest.getPassword() 应该包含用户输入的明文密码
        boolean isPasswordMatch = PasswordUtils.verify(dbEncodedPassword, userLoginRequest.getPassword());

        //  如果不匹配，则抛出密码无效异常
        if (!isPasswordMatch) {
            throw new BizException(ErrorCode.AUTH_PASSWORD_INVALID);
        }
    }
}