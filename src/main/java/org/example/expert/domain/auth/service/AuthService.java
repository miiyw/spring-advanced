package org.example.expert.domain.auth.service;

import lombok.RequiredArgsConstructor;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public SignupResponse signup(SignupRequest signupRequest) {

        // 1) 이메일 중복 검사 (Early Return) → 중복이면 이후 로직(encode 등) 수행 안 함
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            throw new InvalidRequestException("이미 존재하는 이메일입니다.");
        }

        // 2) 비밀번호 암호화 (중복 검사를 통과한 경우에만)
        String encodedPassword = passwordEncoder.encode(signupRequest.getPassword());

        // 3) 역할 파싱
        UserRole userRole = UserRole.of(signupRequest.getUserRole());

        // 4) 사용자 생성 & 저장
        User newUser = new User(
                signupRequest.getEmail(),
                encodedPassword,
                userRole
        );
        User savedUser = userRepository.save(newUser);

        // 5) JWT 생성(순수 토큰) → 응답에만 Bearer 접두어 부여
        String token = jwtUtil.createToken(savedUser.getId(), savedUser.getEmail(), userRole);
        String authorization = token.startsWith(JwtUtil.BEARER_PREFIX)
                ? token
                : JwtUtil.BEARER_PREFIX + token;

        return new SignupResponse(authorization); // "Bearer <JWT>" 단일 필드
    }

    @Transactional(readOnly = true)
    public SigninResponse signin(SigninRequest signinRequest) {
        User user = userRepository.findByEmail(signinRequest.getEmail()).orElseThrow(
                () -> new InvalidRequestException("가입되지 않은 유저입니다."));

        // 비밀번호 불일치 → 401
        if (!passwordEncoder.matches(signinRequest.getPassword(), user.getPassword())) {
            throw new AuthException("잘못된 비밀번호입니다.");
        }

        // JWT 생성(순수 토큰) → 응답에만 Bearer 접두어 부여
        String token = jwtUtil.createToken(user.getId(), user.getEmail(), user.getUserRole());
        String authorization = token.startsWith(JwtUtil.BEARER_PREFIX)
                ? token
                : JwtUtil.BEARER_PREFIX + token;

        return new SigninResponse(authorization); // "Bearer <JWT>" 단일 필드
    }
}
