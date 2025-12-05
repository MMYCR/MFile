package org.example.myselffile.core.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
// Sa-Token 拦截器
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(new SaInterceptor(handle -> {
                    StpUtil.checkLogin();
                }))
                .addPathPatterns("/api/**") // 拦截范围
                .excludePathPatterns(       // 放行范围 (白名单)
                        "/api/login",       // 登录接口必须放行
                        "/api/check-login", // 检查状态接口放行
                        "/api/list",        //  如果让游客也能看列表，就放行这个
                        "/api/share/download",    //  如果让游客也能下载，就放行这个
                        "/api/register"
                );
    }
}