package com.example.chat.domain.user;

import com.example.chat.security.CustomUserDetails;
import org.springframework.security.core.userdetails.UserDetails;

public class UserMapper {
    public static UserEntity fromCreateToEntity(UserDto.Create dto) {
        return UserEntity.builder()
                .email(dto.email())
                .password(dto.password())
                .username(dto.username())
                .build();
    }

    public static UserDto.Response fromEntityToResponse(UserEntity entity) {
        return new UserDto.Response(
                entity.getEmail(),
                entity.getUsername(),
                entity.getRole(),
                entity.getPlan().getName(),
                entity.getRemainingTokens()
        );
    }

    public static UserDto.DetailedResponse fromEntityToDetailedResponse(UserEntity entity) {
        return new UserDto.DetailedResponse(
                entity.getId(),
                entity.getEmail(),
                entity.getUsername(),
                entity.getRole(),
                entity.getPlan().getName(),
                entity.getRemainingTokens(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static UserDto fromEntityToDto(UserEntity entity) {
        return UserDto.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .username(entity.getUsername())
                .password(entity.getPassword())
                .role(entity.getRole())
                .planId(entity.getPlan().getName())
                .remainingTokens(entity.getRemainingTokens())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static UserDetails fromDtoToCustomUserDetails(UserDto dto) {
        return new CustomUserDetails(dto);
    }
}
