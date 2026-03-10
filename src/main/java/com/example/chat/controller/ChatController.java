package com.example.chat.controller;

import com.example.chat.domain.ApiResponseDto;
import com.example.chat.domain.chat.message.MessageDto;
import com.example.chat.domain.chat.session.SessionDto;
import com.example.chat.security.CustomUserDetails;
import com.example.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 내 채팅방 목록 조회 API
     * GET /api/chat/sessions
     */
    @GetMapping("/sessions")
    public ResponseEntity<ApiResponseDto<List<SessionDto.Response>>> getSessions(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponseDto.success(
                "채팅 목록 조회 성공", chatService.getMySessions(userDetails.getId())));
    }

    /**
     * 특정 채팅방 메시지 내역 조회 API
     * GET /api/chat/sessions/{sessionId}/messages
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<ApiResponseDto<List<MessageDto.Response>>> getMessages(
            @PathVariable String sessionId) {
        return ResponseEntity.ok(ApiResponseDto.success(
                "대화 내역 조회 성공", chatService.getMessagesBySession(sessionId)));
    }

    /**
     * 채팅방 삭제 API
     * DELETE /api/chat/sessions/{sessionId}
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponseDto<Void>> deleteSession(@PathVariable String sessionId) {
        chatService.deleteSession(sessionId);
        return ResponseEntity.ok(ApiResponseDto.success("채팅방이 삭제되었습니다."));
    }
}