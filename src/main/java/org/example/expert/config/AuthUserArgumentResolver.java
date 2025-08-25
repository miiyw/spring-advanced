package org.example.expert.config;

import jakarta.servlet.http.HttpServletRequest;
import org.example.expert.domain.auth.exception.AuthException;
import org.example.expert.domain.common.annotation.Auth;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.user.enums.UserRole;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class AuthUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        boolean hasAuthAnnotation = parameter.getParameterAnnotation(Auth.class) != null;
        boolean isAuthUserType = parameter.getParameterType().equals(AuthUser.class);

        // @Auth와 AuthUser는 함께 사용
        if (hasAuthAnnotation != isAuthUserType) {
            throw new AuthException("@Auth와 AuthUser 타입은 함께 사용되어야 합니다.");
        }
        return hasAuthAnnotation; // 둘 다 true일 때만 지원
    }

    @Override
    public Object resolveArgument(
            @Nullable MethodParameter parameter,
            @Nullable ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            @Nullable WebDataBinderFactory binderFactory
    ) {
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();

        // 1) 표준 경로: 필터에서 넣어둔 AuthUser 객체를 그대로 사용
        Object attr = request.getAttribute("authUser");
        if (attr instanceof AuthUser) {
            return attr;
        }

        // 2) 레거시 경로(fallback): 개별 속성으로부터 조립
        Object idAttr = request.getAttribute("userId");
        Object emailAttr = request.getAttribute("email");
        Object roleAttr = request.getAttribute("userRole");

        if (idAttr == null || emailAttr == null || roleAttr == null) {
            throw new AuthException("로그인이 필요합니다.");
        }

        // userId 파싱
        final Long userId;
        try {
            userId = (idAttr instanceof Long) ? (Long) idAttr : Long.valueOf(String.valueOf(idAttr));
        } catch (Exception e) {
            throw new AuthException("유효하지 않은 사용자 식별자입니다.");
        }

        final String email = String.valueOf(emailAttr);

        // role 파싱: 필터에서 대문자/ROLE_ 제거까지 정규화했다는 가정하에 valueOf 사용
        final String roleStr = String.valueOf(roleAttr);
        final UserRole userRole;
        try {
            userRole = UserRole.valueOf(roleStr); // "USER"/"ADMIN"
        } catch (IllegalArgumentException e) {
            throw new AuthException("유효하지 않은 UserRole");
        }

        return new AuthUser(userId, email, userRole);
    }
}
