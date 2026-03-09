package com.example.chat.domain.plan;

import java.time.LocalDateTime;

public class PlanDto {

    // 플랜 변경(결제) 요청
    public record Request(
            Long planId
    ) {}

    // 결제 내역 응답
    public record Response(
            String id,
            Long planId,
            String planName,
            int amount,
            String status,
            LocalDateTime createdAt
    ) {}
}