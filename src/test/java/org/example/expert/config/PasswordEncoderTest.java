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
        // given
        String rawPassword = "testPassword";  // 원본 비밀번호
        String encodedPassword = passwordEncoder.encode(rawPassword);  // 암호화된 비밀번호

        // when
        // 순서를 수정하여 원본 비밀번호와 암호화된 비밀번호를 비교
        boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);

        // then
        // 두 비밀번호가 일치하면 true를 반환해야 함
        assertTrue(matches);
    }
}
