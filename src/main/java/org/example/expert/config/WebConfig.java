package org.example.expert.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Component
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    // 관리자 접근 권한을 검증하는 인터셉터
    private final AdminAccessInterceptor adminAccessInterceptor;

    // ArgumentResolver 등록
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new AuthUserArgumentResolver());
    }

    /**
     * 관리자 전용 API 요청 시 인터셉터 동작 추가
     * - /admin/comments/**
     * - /admin/users/**
     * 위 경로에 대해서만 adminAccessInterceptor 적용
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAccessInterceptor)
                .addPathPatterns("/admin/comments/**", "/admin/users/**"); // 어드민 API만 적용
    }
}
