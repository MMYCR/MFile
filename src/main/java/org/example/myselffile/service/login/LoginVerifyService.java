package org.example.myselffile.service.login;

import org.example.myselffile.module.user.model.request.UserLoginRequest;

public interface LoginVerifyService {
    public abstract void verify(UserLoginRequest userLoginRequest);
}
