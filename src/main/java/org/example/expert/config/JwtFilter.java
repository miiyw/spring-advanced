package org.example.expert.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.*;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.dto.ErrorResponse;
import org.example.expert.domain.common.exception.ErrorCode;
import org.example.expert.domain.user.enums.UserRole;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtFilter implements Filter {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper(); // JSON 직렬화용

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest  = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String url = httpRequest.getRequestURI();

        // 인증 불필요 경로는 통과
        if (url.startsWith("/auth")) {
            chain.doFilter(request, response);
            return;
        }

        String header = httpRequest.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            writeError(httpResponse, HttpServletResponse.SC_UNAUTHORIZED,
                    ErrorCode.AUTH_REQUIRED, "JWT 토큰이 필요합니다.");
            return;
        }

        final String jwt;
        try {
            jwt = jwtUtil.substringToken(header); // "Bearer ..."에서 토큰만 추출
        } catch (Exception e) {
            writeError(httpResponse, HttpServletResponse.SC_UNAUTHORIZED,
                    ErrorCode.AUTH_REQUIRED, "JWT 형식이 올바르지 않습니다.");
            return;
        }

        try {
            Claims claims = jwtUtil.extractClaims(jwt);
            if (claims == null) {
                writeError(httpResponse, HttpServletResponse.SC_UNAUTHORIZED,
                        ErrorCode.AUTH_REQUIRED, "유효하지 않은 JWT 토큰입니다.");
                return;
            }

            // subject → userId
            final Long userId;
            try {
                userId = Long.parseLong(claims.getSubject());
            } catch (Exception e) {
                writeError(httpResponse, HttpServletResponse.SC_UNAUTHORIZED,
                        ErrorCode.AUTH_REQUIRED, "유효하지 않은 사용자 식별자입니다.");
                return;
            }

            // email
            String email = claims.get("email", String.class);
            if (email == null || email.isBlank()) {
                writeError(httpResponse, HttpServletResponse.SC_UNAUTHORIZED,
                        ErrorCode.AUTH_REQUIRED, "토큰에 이메일 정보가 없습니다.");
                return;
            }

            // role: userRole 또는 role 키 허용 + 정규화
            String roleStr = claims.get("userRole", String.class);
            if (roleStr == null) roleStr = claims.get("role", String.class);

            final UserRole role;
            try {
                role = toUserRole(roleStr);
            } catch (IllegalArgumentException e) {
                writeError(httpResponse, HttpServletResponse.SC_UNAUTHORIZED,
                        ErrorCode.AUTH_REQUIRED, "유효하지 않은 UserRole");
                return;
            }

            // 컨트롤러/인터셉터에서 일관되게 쓰도록 통합 컨텍스트만 세팅
            httpRequest.setAttribute("authUser", new AuthUser(userId, email, role));

            // 레거시 호환: 예전 코드가 기대하던 개별 속성도 세팅
            httpRequest.setAttribute("userId", userId);
            httpRequest.setAttribute("email", email);
            httpRequest.setAttribute("userRole", role.name()); // "USER" / "ADMIN"

            chain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            log.debug("Expired JWT token", e);
            httpResponse.setHeader("WWW-Authenticate", "Bearer error=\"invalid_token\", error_description=\"expired\"");
            writeError(httpResponse, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.AUTH_REQUIRED, "만료된 JWT 토큰입니다.");
        } catch (MalformedJwtException e) {
            log.debug("Malformed JWT", e);
            writeError(httpResponse, HttpServletResponse.SC_UNAUTHORIZED,
                    ErrorCode.AUTH_REQUIRED, "유효하지 않은 JWT 서명입니다.");
        } catch (UnsupportedJwtException e) {
            log.debug("Unsupported JWT", e);
            writeError(httpResponse, HttpServletResponse.SC_UNAUTHORIZED,
                    ErrorCode.AUTH_REQUIRED, "지원되지 않는 JWT 토큰입니다.");
        } catch (SecurityException e) {
            log.debug("Invalid JWT signature", e);
            writeError(httpResponse, HttpServletResponse.SC_UNAUTHORIZED,
                    ErrorCode.AUTH_REQUIRED, "유효하지 않은 JWT 서명입니다.");
        } catch (Exception e) {
            log.error("JWT 처리 중 오류", e);
            writeError(httpResponse, HttpServletResponse.SC_UNAUTHORIZED,
                    ErrorCode.AUTH_REQUIRED, "유효하지 않은 JWT 토큰입니다.");
        }
    }

    private UserRole toUserRole(String raw) {
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("role is blank");
        String s = raw.trim().toUpperCase();
        if (s.startsWith("ROLE_")) s = s.substring(5); // ROLE_ADMIN -> ADMIN
        return UserRole.valueOf(s); // USER/ADMIN 등
    }

    private void writeError(HttpServletResponse resp, int httpStatus,
                            ErrorCode code, String message) throws IOException {
        resp.setStatus(httpStatus);
        resp.setContentType("application/json;charset=UTF-8");
        var body = ErrorResponse.builder()
                .code(code.name())
                .message(message)
                .build();
        resp.getWriter().write(objectMapper.writeValueAsString(body));
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
