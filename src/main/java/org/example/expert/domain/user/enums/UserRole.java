package org.example.expert.domain.user.enums;

import org.example.expert.domain.common.exception.InvalidRequestException;

public enum UserRole {
    ADMIN, USER;

    public static UserRole of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidRequestException("유효하지 않은 UserRole");
        }
        String s = raw.trim().toUpperCase();
        if (s.startsWith("ROLE_")) {
            s = s.substring(5); // ROLE_ADMIN -> ADMIN
        }
        try {
            return UserRole.valueOf(s); // ADMIN / USER
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("유효하지 않은 UserRole");
        }
    }
}
