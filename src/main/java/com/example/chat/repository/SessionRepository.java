package com.example.chat.repository;

import com.example.chat.domain.chat.session.SessionEntity;
import com.example.chat.domain.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionRepository extends JpaRepository<SessionEntity, String> {

    // 특정 유저의 모든 채팅방을 조회 (내림차순)
    List<SessionEntity> findAllByUserOrderByCreatedAtDesc(UserEntity user);
}
