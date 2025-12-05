package org.example.myselffile.service.login;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.myselffile.module.user.model.request.UserLoginRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class LoginService {
    @Resource
    private List<LoginVerifyService> loginVerifyServices;

    public void verify(UserLoginRequest userLoginRequest)
    {
        loginVerifyServices.forEach(loginVerifyService -> loginVerifyService.verify(userLoginRequest));
    }
}