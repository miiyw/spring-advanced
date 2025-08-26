package org.example.expert.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
class PasswordEncoderTest {

    @InjectMocks
    private PasswordEncoder passwordEncoder;

    @Test
    void matches_메서드가_정상적으로_동작한다() {
        // given: 원본 비밀번호와 이를 암호화한 값 준비
        String rawPassword = "testPassword";  // 원본 비밀번호
        String encodedPassword = passwordEncoder.encode(rawPassword);  // 암호화된 비밀번호

        // when: matches() 호출로 원본과 암호화된 비밀번호 비교
        boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);

        // then: 원본과 암호화된 비밀번호가 일치하면 true 반환
        assertTrue(matches);
    }
}
