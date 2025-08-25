package org.example.expert.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {

    @NotBlank(message = "이메일을 입력하세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "비밀번호를 입력하세요.")
    @Size(min = 8, max = 64, message = "비밀번호는 8~64자여야 합니다.")
    @Pattern(regexp = ".*\\d.*", message = "비밀번호에는 숫자가 최소 1개 포함되어야 합니다.")
    @Pattern(regexp = ".*[A-Z].*", message = "비밀번호에는 대문자가 최소 1개 포함되어야 합니다.")
    private String password;

    @NotBlank(message = "userRole을 입력하세요.")
    // ADMIN 또는 USER만 허용, 대소문자 무시
    @Pattern(regexp = "(?i)ADMIN|USER", message = "userRole은 ADMIN 또는 USER여야 합니다.")
    private String userRole;
}
