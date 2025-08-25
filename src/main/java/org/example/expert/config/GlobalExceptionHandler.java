package org.example.expert.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.example.expert.domain.auth.exception.AuthException;
import org.example.expert.domain.common.dto.ErrorResponse;
import org.example.expert.domain.common.exception.ErrorCode;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.common.exception.ServerException;
import org.slf4j.MDC;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ===== 커스텀 예외 =====
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException ex, HttpServletRequest req) {
        return build(ErrorCode.VALIDATION_ERROR, ex.getMessage(), req, null);
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthException ex, HttpServletRequest req) {
        return build(ErrorCode.AUTH_REQUIRED, ex.getMessage(), req, null);
    }

    @ExceptionHandler(ServerException.class)
    public ResponseEntity<ErrorResponse> handleServer(ServerException ex, HttpServletRequest req) {
        log.error("ServerException", ex);
        return build(ErrorCode.SERVER_ERROR, ex.getMessage(), req, null);
    }

    // ===== Bean Validation 바인딩 실패 (@Valid) =====
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                      HttpServletRequest req) {
        List<ErrorResponse.FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .collect(toList());
        return build(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.defaultMessage, req, fields);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBind(BindException ex, HttpServletRequest req) {
        List<ErrorResponse.FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .collect(toList());
        return build(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.defaultMessage, req, fields);
    }

    // ===== 파라미터/본문 오류 등 400류 =====
    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex, HttpServletRequest req) {
        return build(ErrorCode.VALIDATION_ERROR, "요청 값이 올바르지 않습니다.", req, null);
    }

    // ===== 존재하지 않는 리소스 → 404 =====
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(Exception ex, HttpServletRequest req) {
        return build(ErrorCode.NOT_FOUND, "리소스를 찾을 수 없습니다.", req, null);
    }

    // 데이터 삭제/조회 시 결과 없음 → 404
    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<ErrorResponse> handleEmptyResult(EmptyResultDataAccessException ex, HttpServletRequest req) {
        return build(ErrorCode.NOT_FOUND, "리소스를 찾을 수 없습니다.", req, null);
    }

    // 잘못된 인자 → 400
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArg(IllegalArgumentException ex, HttpServletRequest req) {
        return build(ErrorCode.VALIDATION_ERROR, ex.getMessage(), req, null);
    }

    // @Validated(Path/Query) 검증 실패 → 400
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        return build(ErrorCode.VALIDATION_ERROR, "요청 값이 올바르지 않습니다.", req, null);
    }

    // ResponseStatusException은 상태 그대로 반영 (최소화: code는 상태명)
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleRSE(ResponseStatusException ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());

        // 상태코드 → 표준 ErrorCode 매핑
        ErrorCode code =
                (status == HttpStatus.UNAUTHORIZED) ? ErrorCode.AUTH_REQUIRED :
                        (status == HttpStatus.FORBIDDEN)    ? ErrorCode.FORBIDDEN     :
                                (status == HttpStatus.NOT_FOUND)    ? ErrorCode.NOT_FOUND     :
                                        (status.is4xxClientError())         ? ErrorCode.VALIDATION_ERROR
                                                : ErrorCode.SERVER_ERROR;

        // reason(예: "Comment not found")이나 "404 NOT_FOUND" 같은 문자열은 쓰지 않고
        // ErrorCode의 기본 메시지를 사용해 최소 포맷으로 통일
        ErrorResponse body = ErrorResponse.builder()
                .code(code.name())
                .message(code.defaultMessage)
                .build();

        return withHeaders(ResponseEntity.status(status), req).body(body);
    }

    // 그 외 전부 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOthers(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception", ex);
        return build(ErrorCode.SERVER_ERROR, ErrorCode.SERVER_ERROR.defaultMessage, req, null);
    }

    // ===== 공통 빌더 =====
    private ResponseEntity<ErrorResponse> build(ErrorCode code, String msg, HttpServletRequest req,
                                                List<ErrorResponse.FieldError> fields) {
        ErrorResponse body = ErrorResponse.builder()
                .code(code.name())
                .message(Optional.ofNullable(msg).orElse(code.defaultMessage))
                .errors(fields)
                .build();
        return withHeaders(ResponseEntity.status(code.status), req).body(body);
    }

    // 메타 데이터는 헤더로만 최소 노출
    private ResponseEntity.BodyBuilder withHeaders(ResponseEntity.BodyBuilder builder, HttpServletRequest req) {
        String traceId = MDC.get("traceId");
        if (traceId != null) builder.header("X-Trace-Id", traceId);
        return builder;
    }
}
