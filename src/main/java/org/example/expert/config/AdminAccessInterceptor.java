package org.example.expert.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.user.enums.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class AdminAccessInterceptor implements HandlerInterceptor {

    // 컨트롤러 실행 전(preHandle) 실행되는 메서드
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // JwtFilter에서 세팅해 둔 request attribute 가져오기
        Object attr = request.getAttribute("authUser");

        // 요청 메서드, URI, 인증 사용자 여부 로깅
        log.info("[ADMIN-TRY] method={}, uri={}, hasAuthUser={}",
                request.getMethod(), request.getRequestURI(), attr != null);

        // 인증 객체(AuthUser)로 캐스팅
        AuthUser authUser = (AuthUser) attr;

        // 1. 로그인 안 된 경우 → 401 Unauthorized
        if (authUser == null) {
            log.warn("[ADMIN-DENY] reason=no-auth, uri={}", request.getRequestURI());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        // 2. 로그인은 됐지만 ADMIN 권한이 아닌 경우 → 403 Forbidden
        if (authUser.getUserRole() != UserRole.ADMIN) {
            log.warn("[ADMIN-DENY] reason=forbidden, userId={}, role={}, uri={}",
                    authUser.getId(), authUser.getUserRole(), request.getRequestURI());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자 권한이 필요합니다.");
        }

        // 3. ADMIN 권한 검증 통과 → 허용
        log.info("[ADMIN-ALLOW] userId={}, role={}, uri={}",
                authUser.getId(), authUser.getUserRole(), request.getRequestURI());
        return true;    // 컨트롤러 실행 계속 진행
    }
}
