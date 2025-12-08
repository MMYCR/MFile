package org.example.myselffile.service;

import cn.dev33.satoken.stp.StpUtil;
import org.example.myselffile.module.user.model.UserIds;
import org.example.myselffile.module.user.model.eneity.User;
import org.example.myselffile.module.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    @Autowired
    private UserService userService;
    public User getCurrentUser()
    {
        Long loginId = StpUtil.getLoginId(UserIds.ANONYMOUS_ID);
        if (loginId.equals(UserIds.ANONYMOUS_ID)) {
            return null;
        }
        // 优先从Sa-Token会话中获取，减少数据库查询
        User user = (User) StpUtil.getSession().get("user");
        if (user == null) {
            user = userService.getUserById(loginId);
            if (user != null) {
                StpUtil.getSession().set("user", user); // 存入会话，下次直接获取
            }
        }
        return user;
    }
    public Long getCurrentUserId() {
        return StpUtil.getLoginId(UserIds.ANONYMOUS_ID);
    }

    public boolean isLogin() {
        return StpUtil.isLogin();
    }

    public boolean hasRole(String role) {
        return StpUtil.hasRole(role);
    }
}
