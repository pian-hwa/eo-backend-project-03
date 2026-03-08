package com.example.chat.domain.user.auth;

import java.time.Duration;
import java.time.LocalDateTime;

public class AuthTokenDto {
    public record Create(
            String userId,
            Duration duration
    ) {}

    public record Extend(
            String userId,
            Duration duration
    ) {}

    public record Token(
            String token
    ) {}

    public record Response(
            String token,
            String userId,
            LocalDateTime expiresAt
    ) {}
}
