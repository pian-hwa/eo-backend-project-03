package com.example.chat.service;

import com.example.chat.domain.chat.message.MessageDto;
import com.example.chat.domain.chat.message.MessageEntity;
import com.example.chat.domain.chat.message.MessageRole;
import com.example.chat.domain.chat.session.SessionDto;
import com.example.chat.domain.chat.session.SessionEntity;
import com.example.chat.domain.user.UserEntity;
import com.example.chat.repository.MessageRepository;
import com.example.chat.repository.SessionRepository;
import com.example.chat.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final SessionRepository sessionRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    // AI에게 질문하기 전
    @Transactional
    public SessionEntity prepareChat(String userId, String sessionId, String content, String modelName) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));

        // 토큰 체크
        if (user.getRemainingTokens() <= 0) {
            throw new IllegalStateException("토큰이 부족합니다. 플랜을 업그레이드하거나 충전해주세요.");
        }

        // 모델 제한 (PlanService와 연동 후 보완)
        if (user.getPlan().getName().equals("BASIC") && modelName.contains("gpt-4")) {
            throw new IllegalArgumentException("현재 플랜에서 지원하지 않는 모델입니다.");
        }

        // 세션 생성 또는 조회
        SessionEntity session;
        if (sessionId == null || sessionId.isBlank()) {
            // 첫 질문이면 세션 생성 (첫 20자를 제목으로 언제든 수정가능)
            String title = content.length() > 20 ? content.substring(0, 20) : content;
            session = SessionEntity.builder()
                    .user(user)
                    .title(title)
                    .build();
            session = sessionRepository.save(session);
        } else {
            session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new NoSuchElementException("존재하지 않는 채팅 세션입니다."));
        }

        // 사용자 질문 메시지 저장
        MessageEntity userMessage = MessageEntity.builder()
                .session(session)
                .role(MessageRole.USER)
                .content(content)
                .modelName(modelName)
                .usedTokens(0)
                .build();
        messageRepository.save(userMessage);

        return session;
    }

    // AI 답변 완료 후
    @Transactional
    public void completeChat(String userId, SessionEntity session, String aiContent, String modelName, int usedTokens) {
        UserEntity user = userRepository.findById(userId).orElseThrow();

        // 답변 메시지 저장
        MessageEntity aiMessage = MessageEntity.builder()
                .session(session)
                .role(MessageRole.ASSISTANT)
                .content(aiContent)
                .modelName(modelName)
                .usedTokens(usedTokens)
                .build();
        messageRepository.save(aiMessage);

        // 토큰 차감
        user.decreaseTokens(usedTokens);
    }

    // 내 채팅 목록 조회
    @Transactional(readOnly = true)
    public List<SessionDto.Response> getMySessions(String userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));

        return sessionRepository.findAllByUserOrderByCreatedAtDesc(user).stream()
                .map(SessionDto.Response::fromEntity)
                .toList();
    }

    // 채팅방 상세 대화 내역 조회
    @Transactional(readOnly = true)
    public List<MessageDto.Response> getMessagesBySession(String sessionId) {
        SessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("세션을 찾을 수 없습니다."));

        return messageRepository.findAllBySessionOrderByCreatedAtAsc(session).stream()
                .map(MessageDto.Response::fromEntity)
                .toList();
    }

    // 채팅방 삭제
    @Transactional
    public void deleteSession(String sessionId) {
        SessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("삭제할 세션이 존재하지 않습니다."));
        sessionRepository.delete(session);
    }
}