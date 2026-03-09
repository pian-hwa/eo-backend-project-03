package com.example.chat.repository.user;

import com.example.chat.domain.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, String> {

    // 로그인시 이메일로 유저 찾기
    Optional<UserEntity> findByEmail(String email);

    // 이메일 중복확인
    boolean existsByEmail(String email);

    // 닉네임 중복확인
    boolean existsByUsername(String username);

    // 비밀번호 재설정 토큰으로 유저 찾기
    Optional<UserEntity> findByResetToken(String resetToken);
}
