package com.datn.TheCasualWear.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Class riêng, không cần gộp vào config có sẵn — Spring cho phép nhiều bean
// WebMvcConfigurer cùng lúc, tất cả đều được áp dụng.
@Configuration
@RequiredArgsConstructor
public class ShiftWebConfig implements WebMvcConfigurer {

    private final ShiftAccessInterceptor shiftAccessInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(shiftAccessInterceptor)
                .addPathPatterns("/cashier/**")
                .excludePathPatterns("/cashier/shift/**");
    }
}
