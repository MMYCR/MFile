package org.example.myselffile.core.config;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.strategy.SaStrategy;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.lang.reflect.Method;

// Sa-Token 拦截器
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器
        registry.addInterceptor(new SaInterceptor(handle -> {

            if (handle instanceof HandlerMethod) {
                HandlerMethod handlerMethod = (HandlerMethod) handle;
                Method method = handlerMethod.getMethod();

                // 检查方法上是否有 @SaIgnore
                boolean isIgnore = SaStrategy.instance.isAnnotationPresent.apply(method, SaIgnore.class);

                if (isIgnore) {
                    return; // 如果有，直接放行
                }
            }
            SaRouter.match("/api/share/download/**").stop();

            //  其他 /api/ 下的请求，执行登录校验
            SaRouter.match("/api/**")
                    .check(r -> StpUtil.checkLogin());

        })).addPathPatterns("/**");
    }
}