package org.example.expert.domain.auth.service;

import org.example.expert.config.JwtUtil;
import org.example.expert.config.PasswordEncoder;
import org.example.expert.domain.auth.dto.request.SigninRequest;
import org.example.expert.domain.auth.dto.request.SignupRequest;
import org.example.expert.domain.auth.dto.response.SigninResponse;
import org.example.expert.domain.auth.dto.response.SignupResponse;
import org.example.expert.domain.auth.exception.AuthException;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    // ========== signup() ==========

    @Test
    void signup_중복이메일이면_예외가_발생하고_encode_save_JWT가_호출되지_않는다() {
        // given: 이미 존재하는 이메일로 회원가입 요청
        SignupRequest req = new SignupRequest("dup@ex.com", "raw", "USER");
        given(userRepository.existsByEmail("dup@ex.com")).willReturn(true);

        // when & then: InvalidRequestException 발생 + 부가 동작 없음
        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> authService.signup(req));
        assertEquals("이미 존재하는 이메일입니다.", ex.getMessage());

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
        verify(jwtUtil, never()).createToken(any(), any(), any());
    }

    @Test
    void signup_정상처리시_encode_save_JWT가_호출되고_Bearer토큰이_반환된다() {
        // given: 신규 이메일 + 정상 흐름
        SignupRequest req = new SignupRequest("new@ex.com", "rawPass", "USER");

        given(userRepository.existsByEmail("new@ex.com")).willReturn(false);
        given(passwordEncoder.encode("rawPass")).willReturn("ENC");
        // save될 User를 반환(아이디 필요)
        User saved = new User("new@ex.com", "ENC", UserRole.USER);
        ReflectionTestUtils.setField(saved, "id", 10L);
        given(userRepository.save(any(User.class))).willReturn(saved);
        // JWT는 접두어 없이 준다고 가정 → 서비스에서 Bearer 붙임
        given(jwtUtil.createToken(10L, "new@ex.com", UserRole.USER)).willReturn("jwt-token");

        // when
        SignupResponse res = authService.signup(req);

        // then: Bearer 접두어가 붙은 토큰 반환 + encode/save/JWT 호출 확인
        assertNotNull(res);
        assertTrue(res.getBearerToken().startsWith(JwtUtil.BEARER_PREFIX));
        assertEquals("Bearer jwt-token", res.getBearerToken());

        verify(passwordEncoder, times(1)).encode("rawPass");
        verify(userRepository, times(1)).save(any(User.class));
        verify(jwtUtil, times(1)).createToken(10L, "new@ex.com", UserRole.USER);
    }

    @Test
    void signup_JWT가_Bearer로_시작하면_중복접두어를_붙이지_않는다() {
        // given: JWT 자체가 이미 Bearer 접두어 포함
        SignupRequest req = new SignupRequest("bearer@ex.com", "pw", "USER");
        given(userRepository.existsByEmail("bearer@ex.com")).willReturn(false);
        given(passwordEncoder.encode("pw")).willReturn("ENC");
        User saved = new User("bearer@ex.com", "ENC", UserRole.USER);
        ReflectionTestUtils.setField(saved, "id", 20L);
        given(userRepository.save(any(User.class))).willReturn(saved);
        given(jwtUtil.createToken(20L, "bearer@ex.com", UserRole.USER)).willReturn("Bearer abc.def.ghi");

        // when
        SignupResponse res = authService.signup(req);

        // then: 접두어 중복 없이 그대로 반환
        assertEquals("Bearer abc.def.ghi", res.getBearerToken()); // 중복 없음
    }

    // ========== signin() ==========

    @Test
    void signin_가입되지않은_이메일이면_InvalidRequestException이_발생한다() {
        // given: 등록되지 않은 이메일
        SigninRequest req = new SigninRequest("none@ex.com", "pw");
        given(userRepository.findByEmail("none@ex.com")).willReturn(Optional.empty());

        // when & then: 가입되지 않은 유저 예외 발생
        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> authService.signin(req));
        assertEquals("가입되지 않은 유저입니다.", ex.getMessage());

        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtUtil, never()).createToken(any(), any(), any());
    }

    @Test
    void signin_비밀번호가_일치하지않으면_AuthException이_발생한다() {
        // given: 이메일 존재하나 비밀번호 불일치
        User user = new User("u@ex.com", "ENC", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 30L);

        SigninRequest req = new SigninRequest("u@ex.com", "wrong");
        given(userRepository.findByEmail("u@ex.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong", "ENC")).willReturn(false);

        // when & then: 비밀번호 불일치 예외 발생
        AuthException ex = assertThrows(AuthException.class, () -> authService.signin(req));
        assertEquals("잘못된 비밀번호입니다.", ex.getMessage());

        verify(jwtUtil, never()).createToken(any(), any(), any());
    }

    @Test
    void signin_정상처리시_JWT가_생성되고_Bearer토큰이_반환된다() {
        // given: 이메일 존재 + 비밀번호 일치
        User user = new User("ok@ex.com", "ENC", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 40L);

        SigninRequest req = new SigninRequest("ok@ex.com", "raw");
        given(userRepository.findByEmail("ok@ex.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("raw", "ENC")).willReturn(true);
        given(jwtUtil.createToken(40L, "ok@ex.com", UserRole.USER)).willReturn("jwt-ok");

        // when
        SigninResponse res = authService.signin(req);

        // then: Bearer 토큰 반환 + JWT 생성 호출 확인
        assertEquals("Bearer jwt-ok", res.getBearerToken());
        verify(jwtUtil, times(1)).createToken(40L, "ok@ex.com", UserRole.USER);
    }

    @Test
    void signin_JWT가_Bearer로_시작하면_중복접두어를_붙이지_않는다() {
        // given: JWT 자체가 이미 Bearer 접두어 포함
        User user = new User("b@ex.com", "ENC", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 50L);

        SigninRequest req = new SigninRequest("b@ex.com", "pw");
        given(userRepository.findByEmail("b@ex.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("pw", "ENC")).willReturn(true);
        given(jwtUtil.createToken(50L, "b@ex.com", UserRole.USER)).willReturn("Bearer token-b");

        // when
        SigninResponse res = authService.signin(req);

        // then: 접두어 중복 없이 그대로 반환
        assertEquals("Bearer token-b", res.getBearerToken()); // 중복 없음
    }
}
