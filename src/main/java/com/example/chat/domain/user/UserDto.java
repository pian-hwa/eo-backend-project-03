package com.example.chat.domain.user;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserDto {
    private String id;
    private String email;
    private String password;
    private String username;
    private UserRole role;
    private String planId;
    private Long remainingTokens;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    public UserDto(String id, String email, String password, String username, UserRole role, String planId, Long remainingTokens, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.username = username;
        this.role = role;
        this.planId = planId;
        this.remainingTokens = remainingTokens;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public record Create(
            String email,
            String password,
            String username
    ) {}

    public record Login(
            String email,
            String password
    ) {}

    public record UpdatePassword(
            String password
    ) {}

    public record UpdateInfo(
            String username
    ) {}

    public record UpdateRole(
            UserRole role
    ) {}

    public record UpdatePlan(
            String planId
    ) {}

    public record Response(
            String email,
            String username,
            UserRole role,
            String planId,
            Long remainingTokens
    ) {}

    public record DetailedResponse(
            String id,
            String email,
            String username,
            UserRole role,
            String planId,
            Long remainingTokens,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
}
