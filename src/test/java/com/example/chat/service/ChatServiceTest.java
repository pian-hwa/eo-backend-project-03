package com.example.chat.service;

import com.example.chat.domain.chat.message.MessageEntity;
import com.example.chat.domain.chat.session.SessionEntity;
import com.example.chat.domain.plan.PlanEntity;
import com.example.chat.domain.user.UserEntity;
import com.example.chat.repository.MessageRepository;
import com.example.chat.repository.SessionRepository;
import com.example.chat.repository.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @InjectMocks
    private ChatService chatService;

    @Mock private SessionRepository sessionRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private UserRepository userRepository;


    @Test
    @DisplayName("채팅 준비 성공 - 새 세션과 메시지가 저장된다")
    void prepareChat_success_newSession() {
        // given
        String userId = "user-1";
        PlanEntity plan = PlanEntity.builder().name("BASIC").build();
        UserEntity user = UserEntity.builder()
                .id(userId)
                .remainingTokens(100)
                .plan(plan)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // 세션 저장 시 self-return
        when(sessionRepository.save(any(SessionEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        // when
        SessionEntity result = chatService.prepareChat(userId, null, "안녕하세요 AI님!", "gpt-3.5");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("안녕하세요 AI님!");
        verify(sessionRepository, times(1)).save(any(SessionEntity.class));
        verify(messageRepository, times(1)).save(any(MessageEntity.class));
    }

    @Test
    @DisplayName("채팅 준비 실패 - 토큰이 부족하면 예외가 발생한다")
    void prepareChat_fail_outOfTokens() {
        // given
        String userId = "user-1";
        UserEntity user = UserEntity.builder()
                .id(userId)
                // 토큰 없음
                .remainingTokens(0)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> chatService.prepareChat(userId, null, "질문있어요", "gpt-3.5"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("토큰이 부족합니다");
    }

    @Test
    @DisplayName("채팅 준비 실패 - BASIC 플랜은 GPT-4를 사용할 수 없다")
    void prepareChat_fail_planRestriction() {
        // given
        String userId = "user-1";
        PlanEntity basicPlan = PlanEntity.builder().name("BASIC").build();
        UserEntity user = UserEntity.builder()
                .id(userId)
                .remainingTokens(100)
                .plan(basicPlan)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> chatService.prepareChat(userId, null, "어려운 질문", "gpt-4"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("지원하지 않는 모델입니다");
    }

    @Test
    @DisplayName("채팅 완료 성공 - AI 답변이 저장되고 토큰이 차감된다")
    void completeChat_success() {
        // given
        String userId = "user-1";
        UserEntity user = spy(UserEntity.builder()
                .id(userId)
                .remainingTokens(100)
                .build());

        SessionEntity session = SessionEntity.builder().id("session-1").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when
        chatService.completeChat(userId, session, "네, 무엇을 도와드릴까요?", "gpt-3.5", 10);

        // then
        verify(messageRepository, times(1)).save(any(MessageEntity.class));
        // UserEntity의 토큰 차감 메서드가 호출되었는지 확인
        assertThat(user.getRemainingTokens()).isEqualTo(90);
    }

    @Test
    @DisplayName("세션 삭제 성공 - 존재하는 세션을 삭제하면 repository.delete가 호출된다")
    void deleteSession_success() {
        // given
        String sessionId = "session-123";
        SessionEntity session = SessionEntity.builder().id(sessionId).build();

        // 존재 여부 확인 및 조회
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        // when
        chatService.deleteSession(sessionId);

        // then
        verify(sessionRepository, times(1)).delete(session);
    }

    @Test
    @DisplayName("세션 삭제 실패 - 존재하지 않는 세션 ID인 경우 예외 발생")
    void deleteSession_fail_notFound() {
        // given
        String sessionId = "non-existent-id";
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatService.deleteSession(sessionId))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("존재하지 않습니다");

        // 삭제 로직이 실행되지 않았어야 함
        verify(sessionRepository, never()).delete(any());
    }
}