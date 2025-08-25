package org.example.expert.domain.user.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.example.expert.domain.common.annotation.Auth;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.user.dto.request.UserChangePasswordRequest;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 API
 * - 에러 응답은 GlobalExceptionHandler에서 {code, message} 포맷으로 표준화
 * - 인증정보는 @Auth AuthUser로 주입 (JwtFilter → ArgumentResolver)
 */
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable @Positive long userId) {
        UserResponse res = userService.getUser(userId);
        return ResponseEntity.ok(res); // 200 OK
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(@Auth AuthUser authUser,
                                               @Valid @RequestBody UserChangePasswordRequest request) {
        userService.changePassword(authUser.getId(), request);
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}
