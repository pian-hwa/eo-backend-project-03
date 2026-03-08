package com.example.chat.domain.user.auth;

import com.example.chat.domain.IdRule;
import com.example.chat.domain.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "reset_tokens")
public class AuthTokenEntity {
    @Id
    @Column(name = "id", nullable = false, unique = true)
    private String id = IdRule.generateUUID(IdRule.RESET);

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Builder
    public AuthTokenEntity(UserEntity user, Duration duration) {
        this.user = user;
        this.expiresAt = LocalDateTime.now().plus(duration);
    }

    public void extendExpiry(Duration duration) {
        this.expiresAt = LocalDateTime.now().plus(duration);
    }
}
