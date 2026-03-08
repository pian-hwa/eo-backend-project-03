package com.example.chat.domain.user.auth;

import org.springframework.stereotype.Component;

@Component
public class AuthTokenMapper {
    public AuthTokenDto.Token fromEntityToToken(AuthTokenEntity entity) {
        return new AuthTokenDto.Token(entity.getId());
    }

    public AuthTokenDto.Response fromEntityToResponse(AuthTokenEntity entity) {
        return new AuthTokenDto.Response(
                entity.getId(),
                entity.getUser().getId(),
                entity.getExpiresAt()
        );
    }
}
