package com.example.chat.repository;

import com.example.chat.domain.chat.message.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<MessageEntity, String> {

    // 특정 채팅방의 모든 메시지를 조회
    List<MessageEntity> findAllBySessionIdOrderByCreatedAtAsc(Long sessionId);
}
