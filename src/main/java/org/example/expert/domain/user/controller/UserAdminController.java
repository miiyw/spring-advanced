package org.example.expert.domain.user.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.example.expert.domain.user.dto.request.UserRoleChangeRequest;
import org.example.expert.domain.user.service.UserAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 어드민 사용자 전용 API
 * - 권한 검증/접근 로깅: Interceptor/AOP
 * - 에러 응답: GlobalExceptionHandler (최소 {code,message})
 */
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/admin/users")
public class UserAdminController {

    private final UserAdminService userAdminService;

    @PatchMapping("/{userId}")
    public ResponseEntity<Void> changeUserRole(@PathVariable @Positive long userId,
                                               @Valid @RequestBody UserRoleChangeRequest userRoleChangeRequest) {
        userAdminService.changeUserRole(userId, userRoleChangeRequest);
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}
