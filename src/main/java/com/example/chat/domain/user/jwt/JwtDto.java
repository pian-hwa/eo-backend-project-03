package com.example.chat.domain.user.jwt;

public class JwtDto {
    public record Response(
            String grantType,
            String accessToken,
            String refreshToken
    ) {}
}
