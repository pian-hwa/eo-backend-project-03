package com.example.chat.domain.ChatLog;


import com.example.chat.domain.IdRule;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "chat_logs")
public class ChatLogEntity {
    @Id
    @Column(name = "id", nullable = false, unique = true)
    private String id = IdRule.generateUUID(IdRule.CHATLOG);

    @Column(name = "role", nullable = false)
    private ChatRole role;

    @Column(name = "content", nullable = false)
    private String content;
}
