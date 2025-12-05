package org.example.myselffile.controller;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import org.example.myselffile.module.model.result.AjaxJson;
import org.example.myselffile.module.user.model.eneity.User;
import org.example.myselffile.module.user.model.request.UpdateUserPasswordRequest;
import org.example.myselffile.module.user.model.request.UserLoginRequest;
import org.example.myselffile.module.user.service.UserService;
import org.example.myselffile.service.login.PasswordVerifyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class LoginController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordVerifyService passwordVerifyService;

    /**
     * 登录接口
     */
    @PostMapping("/login")
    public AjaxJson<String> login(@RequestBody @Validated UserLoginRequest loginRequest) {

        passwordVerifyService.verify(loginRequest);
        User user = userService.getUserByName(loginRequest.getUsername());
        StpUtil.login(user.getId());
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        return AjaxJson.getSuccessData(tokenInfo.getTokenValue());
    }

    /**
     * 检查当前是否登录
     */
    @GetMapping("/check-login")
    public AjaxJson<Boolean> checkLogin() {
        return AjaxJson.getSuccessData(StpUtil.isLogin());
    }

    /**
     * 注销登录
     */
    @PostMapping("/logout")
    public AjaxJson<String> logout() {
        StpUtil.logout(); // Sa-Token 注销，清理 Redis
        return AjaxJson.getSuccess("注销成功");
    }

    /**
     * 注册接口
     * 请求路径: POST /api/register
     */
    @PostMapping("/register")
    public AjaxJson<Void> register(@RequestBody @Validated UserLoginRequest req) {
        // 复用 UserLoginRequest 接参数，或者你也可以新建一个 UserRegisterRequest
        userService.register(req.getUsername(), req.getPassword());
        return AjaxJson.getSuccess("注册成功");
    }

    /**
     * 修改密码接口
     * 请求路径: POST /api/user/update-pwd
     */
    @PostMapping("/user/update-pwd")
    public AjaxJson<Void> updatePassword(@RequestBody @Validated UpdateUserPasswordRequest req) {
        // 1. 获取当前登录用户 ID (Sa-Token)
        long userId = StpUtil.getLoginIdAsLong();

        // 2. 调用 Service
        userService.updatePwById(userId, req);

        return AjaxJson.getSuccess("密码修改成功");
    }
}