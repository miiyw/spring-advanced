package org.example.expert.domain.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ErrorResponse {
    private final String code;        // 예: VALIDATION_ERROR, AUTH_REQUIRED, NOT_FOUND...
    private final String message;     // 사용자에게 보여 줄 메시지

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final List<FieldError> errors; // 검증 실패 시에만 포함

    @Getter
    @AllArgsConstructor
    public static class FieldError {
        private final String field;
        private final String reason;
    }
}
