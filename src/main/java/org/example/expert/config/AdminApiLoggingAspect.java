package org.example.expert.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.Annotation;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AdminApiLoggingAspect {

    private final ObjectMapper objectMapper;    // JSON 직렬화를 위한 Jackson ObjectMapper

    /**
     * 두 관리자 API 메서드를 대상으로 Around advice 실행
     * - 메서드 실행 전: 요청 정보 로깅
     * - 메서드 실행 후: 응답 정보 로깅
     * - 예외 발생 시: 에러 정보 로깅
     */
    @Around(
            "execution(* org.example.expert.domain.comment.controller.CommentAdminController.deleteComment(..)) || " +
                    "execution(* org.example.expert.domain.user.controller.UserAdminController.changeUserRole(..))"
    )
    public Object logAdminApi(ProceedingJoinPoint pjp) throws Throwable {
        long startedAt = System.currentTimeMillis();    // 실행 시작 시각(ms)

        // 현재 요청 객체 가져오기
        HttpServletRequest request = getCurrentRequest();

        String requestTime = isoNow();  // 요청 시각(UTC ISO 포맷)
        String httpMethod = request != null ? request.getMethod() : "N/A";
        String url = request != null
                ? (request.getRequestURI() + (request.getQueryString() != null ? "?" + request.getQueryString() : ""))
                : "N/A";
        Object userIdAttr = request != null ? request.getAttribute("userId") : null; // JwtFilter가 넣어둔 값 사용

        // 요청 본문 추출(@RequestBody 파라미터만 JSON 직렬화)
        Object requestBody = extractRequestBody(pjp);

        // 공통 로그 데이터 (userId, url, method 등)
        Map<String, Object> base = new HashMap<>();
        base.put("userId", userIdAttr);
        base.put("requestTime", requestTime);
        base.put("httpMethod", httpMethod);
        base.put("url", url);

        // 1) 요청 로그
        Map<String, Object> before = new HashMap<>(base);
        before.put("requestBody", safeJson(requestBody));
        log.info("[ADMIN-API][REQUEST] {}", safeJson(before));

        try {
            // 실제 메서드 실행
            Object result = pjp.proceed();

            long elapsed = System.currentTimeMillis() - startedAt;

            // 2) 응답 로그
            Map<String, Object> after = new HashMap<>(base);
            after.put("elapsedMs", elapsed);
            after.put("responseBody", result == null ? "no-body" : safeJson(result));
            log.info("[ADMIN-API][RESPONSE] {}", safeJson(after));

            return result;
        } catch (Throwable ex) {
            long elapsed = System.currentTimeMillis() - startedAt;

            // 3) 예외 로그
            Map<String, Object> error = new HashMap<>(base);
            error.put("elapsedMs", elapsed);
            error.put("errorType", ex.getClass().getSimpleName());
            error.put("errorMessage", ex.getMessage());
            log.warn("[ADMIN-API][ERROR] {}", safeJson(error), ex);
            throw ex; // 예외는 다시 던져 컨트롤러 레벨에서 처리되도록 함
        }
    }

    // 현재 스레드의 HttpServletRequest 가져오기
    private HttpServletRequest getCurrentRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            return servletAttrs.getRequest();
        }
        return null;
    }

    // 현재 시각을 UTC ISO-8601 문자열로 반환
    private String isoNow() {
        return DateTimeFormatter.ISO_INSTANT.format(Instant.now().atOffset(ZoneOffset.UTC));
    }

    // 메서드 파라미터 중 @RequestBody 달린 객체만 찾아 반환
    private Object extractRequestBody(ProceedingJoinPoint pjp) {
        Signature sig = pjp.getSignature();
        if (!(sig instanceof MethodSignature ms)) return null;

        Annotation[][] paramAnnos = ms.getMethod().getParameterAnnotations();
        Object[] args = pjp.getArgs();

        for (int i = 0; i < paramAnnos.length; i++) {
            for (Annotation a : paramAnnos[i]) {
                // @RequestBody 어노테이션 달린 인자만 선택
                if (a.annotationType().getName().equals("org.springframework.web.bind.annotation.RequestBody")) {
                    return args[i];
                }
            }
        }
        return null;
    }

    /**
     * 객체를 JSON 문자열로 변환
     * 실패 시 toString() 값 반환
     */
    private String safeJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return String.valueOf(obj);
        }
    }
}
