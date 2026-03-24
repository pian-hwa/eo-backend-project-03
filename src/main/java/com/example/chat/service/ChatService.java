package com.example.chat.service;

import com.example.chat.domain.chat.ChatType;
import com.example.chat.domain.chat.ai.AiDto;
import com.example.chat.domain.chat.message.MessageDto;
import com.example.chat.domain.chat.message.MessageEntity;
import com.example.chat.domain.chat.message.MessageRole;
import com.example.chat.domain.chat.session.SessionDto;
import com.example.chat.domain.chat.session.SessionEntity;
import com.example.chat.domain.user.UserEntity;
import com.example.chat.repository.MessageRepository;
import com.example.chat.repository.SessionRepository;
import com.example.chat.repository.user.UserRepository;
import com.example.chat.util.AiResponseParser;
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
    private final AiClient aiClient;

    /**
     * AI에게 질문하기 핵심 로직
     */
    @Transactional
    public MessageDto.Response askAi(String userId, String sessionId, MessageDto.Request request, ChatType type) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));

        // 플랜 등급별 기능 제한 로직
        checkPlanPermission(user, type);

        // 방 번호가 없으면 가장 최근 방을 알아서 찾아주는 똑똑한 세션 준비
        SessionEntity session = prepareChat(user, sessionId, request.content(), request.modelName(), type);

        // 핵심: 외부 API 기억력에 의존하지 않고, 우리 DB에서 이전 대화 내역을 꺼내서 주입
        String optimizedPrompt = buildPromptWithHistory(session, request.content(), type);

        // AI 모델 이름 처리
        String model = (request.modelName() == null || request.modelName().isBlank())
                ? "gpt-4o-mini" : request.modelName();

        if (user.getPlan().getName().equals("BASIC") && model.equalsIgnoreCase("gpt-4")) {
            throw new IllegalArgumentException("BASIC 플랜에서는 gpt-4 모델을 사용할 수 없습니다.");
        }

        AiDto.Response aiResponse = aiClient.getAiAnswer(optimizedPrompt, model, type);

        // AI 답변 완료 후 저장 및 토큰 차감
        MessageEntity aiMessage = completeChat(user, session, aiResponse, model);

        return MessageDto.Response.fromEntity(aiMessage);
    }

    /**
     * [자동 기억 핵심 로직] DB에서 이전 대화 내용을 긁어와서 AI 프롬프트에 몰래 끼워 넣습니다.
     */
    private String buildPromptWithHistory(SessionEntity session, String currentContent, ChatType type) {
        // 요약 기능 등은 이전 대화 내역이 필요 없으므로 기존 프롬프트 사용
        if (type != ChatType.CHAT) {
            return optimizePrompt(currentContent, type);
        }

        // DB에서 현재 채팅방의 모든 대화 내역을 과거순으로 가져옵니다.
        List<MessageEntity> history = messageRepository.findAllBySessionOrderByCreatedAtAsc(session);

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("너는 '33Chat'의 똑똑한 어시스턴트야. 아래의 [이전 대화 내역]을 반드시 기억하고 문맥에 맞게 대답해줘.\n\n");

        // 이전 대화가 있다면 프롬프트에 이어 붙임
        if (!history.isEmpty()) {
            promptBuilder.append("--- [이전 대화 내역] ---\n");

            // 토큰 절약을 위해 너무 옛날 대화 말고 최근 6개(사용자-AI 3번 주고받은 분량)만 가져옵니다.
            int startIndex = Math.max(0, history.size() - 6);
            for (int i = startIndex; i < history.size(); i++) {
                MessageEntity msg = history.get(i);
                String role = msg.getRole() == MessageRole.USER ? "사용자" : "AI";
                promptBuilder.append(role).append(": ").append(msg.getContent()).append("\n");
            }
            promptBuilder.append("----------------------\n\n");
        }

        // 마지막으로 사용자의 최신 질문 추가
        promptBuilder.append("사용자의 새로운 질문: ").append(currentContent);
        return promptBuilder.toString();
    }

    private void checkPlanPermission(UserEntity user, ChatType type) {
        String planName = user.getPlan().getName();
        if (type == ChatType.SUMMARY && planName.equals("BASIC")) {
            throw new IllegalArgumentException("웹 페이지 요약 및 번역 기능은 PRO 플랜 이상부터 이용 가능합니다.");
        }
        if (type == ChatType.YOUTUBE && !planName.equals("PREMIUM")) {
            throw new IllegalArgumentException("유튜브 영상 요약 기능은 PREMIUM 플랜 전용 서비스입니다.");
        }
    }

    private String optimizePrompt(String content, ChatType type) {
        return switch (type) {
            case SUMMARY -> String.format(
                    "너는 웹 콘텐츠 요약 및 번역 전문가야. 아래의 내용을 핵심 위주로 요약하고 한국어로 번역해줘.\n\"\"\"\n%s\n\"\"\"", content);
            case YOUTUBE -> String.format(
                    "너는 유튜브 분석가야. 영상 스크립트 내용을 바탕으로 시간대별 주요 내용을 요약해줘.\n\"\"\"\n%s\n\"\"\"", content);
            default -> String.format("너는 '33Chat'의 지식인 어시스턴트야. 질문에 대해 단계별로 생각해서 답변해줘.\n\"\"\"\n%s\n\"\"\"", content);
        };
    }

    // AI에게 질문하기 전 세션 관리
    @Transactional
    public SessionEntity prepareChat(UserEntity user, String sessionId, String content, String modelName, ChatType type) {

        if (user.getRemainingTokens() <= 0) {
            throw new IllegalStateException("토큰이 부족합니다. 플랜을 업그레이드하거나 충전해주세요.");
        }

        String targetModel = (modelName == null || modelName.isBlank()) ? "gpt-4o-mini" : modelName;

        if (user.getPlan().getName().equals("BASIC") && targetModel.equalsIgnoreCase("gpt-4")) {
            throw new IllegalArgumentException("현재 플랜에서 지원하지 않는 모델입니다.");
        }

        // 세션 생성 로직 (프론트엔드 표준 방식)
        SessionEntity session;
        if (sessionId == null || sessionId.isBlank()) {
            // 프론트엔드가 방 번호를 안 보냈음 -> "새 채팅을 하겠다는 뜻"
            session = createNewSession(user, content, type);

            // 새 대화가 시작되었으므로 앨런 AI 서버의 이전 기억도 깔끔하게 리셋
            aiClient.clearAiHistory();
        } else {
            // 프론트엔드가 기존 방 번호를 보냈음 -> "이어서 대화하겠다는 뜻"
            session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new NoSuchElementException("존재하지 않는 채팅 세션입니다."));
        }

        MessageEntity userMessage = MessageEntity.builder()
                .session(session)
                .role(MessageRole.USER)
                .content(content)
                .modelName(targetModel)
                .usedTokens(0)
                .build();
        messageRepository.save(userMessage);

        return session;
    }

    // 새 세션 생성 헬퍼 메서드
    private SessionEntity createNewSession(UserEntity user, String content, ChatType type) {
        String title = content.length() > 20 ? content.substring(0, 20) : content;
        SessionEntity newSession = SessionEntity.builder()
                .user(user)
                .title(title)
                .chatType(type)
                .build();
        return sessionRepository.save(newSession);
    }

    // AI 답변 완료 후
    @Transactional
    public MessageEntity completeChat(UserEntity user, SessionEntity session, AiDto.Response aiResponse, String model) {

        // -------------------------------------------------------------------
        // [핵심 변경 사항] DB에 저장하기 전에 지저분한 JSON을 깔끔한 텍스트로 파싱합니다.
        // -------------------------------------------------------------------
        String cleanContent = AiResponseParser.extractCleanText(aiResponse.answer());

        MessageEntity aiMessage = MessageEntity.builder()
                .session(session)
                .role(MessageRole.ASSISTANT)
                .content(cleanContent)
                .modelName(model)
                .usedTokens(aiResponse.used_tokens())
                .build();

        MessageEntity savedMessage = messageRepository.save(aiMessage);
        user.decreaseTokens(aiResponse.used_tokens());
        return savedMessage;
    }

    // 내 채팅 목록 조회
    @Transactional(readOnly = true)
    public List<SessionDto.Response> getMySessions(String userId) {
        UserEntity user = userRepository.findById(userId).orElseThrow();
        return sessionRepository.findAllByUserOrderByCreatedAtDesc(user).stream()
                .map(SessionDto.Response::fromEntity).toList();
    }

    // 채팅방 상세 대화 내역 조회
    @Transactional(readOnly = true)
    public List<MessageDto.Response> getMessagesBySession(String sessionId) {
        SessionEntity session = sessionRepository.findById(sessionId).orElseThrow();
        return messageRepository.findAllBySessionOrderByCreatedAtAsc(session).stream()
                .map(MessageDto.Response::fromEntity).toList();
    }

    // 채팅방 삭제
    @Transactional
    public void deleteSession(String sessionId) {
        SessionEntity session = sessionRepository.findById(sessionId).orElseThrow();
        sessionRepository.delete(session);
    }
}