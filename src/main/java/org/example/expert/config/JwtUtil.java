package org.example.expert.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.expert.domain.common.exception.ServerException;
import org.example.expert.domain.user.enums.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Slf4j(topic = "JwtUtil")
@Component
public class JwtUtil {

    public static final String BEARER_PREFIX = "Bearer ";
    private static final long TOKEN_TIME = 60 * 60 * 1000L; // 60분

    @Value("${jwt.secret.key}")
    private String secretKey;   // Base64 인코딩된 키여야 함
    private Key key;
    private final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

    @PostConstruct
    public void init() {
        byte[] bytes = Base64.getDecoder().decode(secretKey);
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    // 반환값에는 Bearer 접두어 포함하지 않음
    public String createToken(Long userId, String email, UserRole userRole) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + TOKEN_TIME);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("email", email)
                .claim("userRole", userRole.name()) // 문자열로 고정 (USER/ADMIN)
                .setIssuedAt(now)
                .setExpiration(exp)                    // 만료 포함
                .signWith(key, signatureAlgorithm)
                .compact();
    }

    /** Authorization 헤더에서 Bearer 토큰만 추출 */
    public String substringToken(String headerValue) {
        if (StringUtils.hasText(headerValue) && headerValue.startsWith(BEARER_PREFIX)) {
            return headerValue.substring(BEARER_PREFIX.length());
        }
        throw new ServerException("Not Found Token");
    }

    /** 만료/서명 검증 포함 (만료 시 ExpiredJwtException 발생) */
    public Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                // .setAllowedClockSkewSeconds(30) // 필요 시 시계 오차 허용
                .build()
                .parseClaimsJws(token)             // 서명 + exp 검증 수행
                .getBody();
    }

    /** 필요 시 명시 검증용 */
    public void validate(String token) {
        extractClaims(token); // 유효하지 않으면 예외 발생
    }
}
