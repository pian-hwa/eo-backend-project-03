package com.example.chat.domain.user;

import com.example.chat.domain.BaseTimeEntity;
import com.example.chat.domain.plan.PlanEntity;
import com.example.chat.domain.ChatId;
import com.example.chat.domain.user.user_enum.UserProvider;
import com.example.chat.domain.user.user_enum.UserRole;
import com.example.chat.domain.user.user_enum.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "users")
public class UserEntity extends BaseTimeEntity {

    @Id
    @Builder.Default
    @Column(name = "id", nullable = false, unique = true, updatable = false)
    private String id = ChatId.generateUUID(ChatId.USER);

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status;

    // AI 포털 핵심: 플랜 및 토큰 관리
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private PlanEntity plan;

    // 로그인 방식
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    @Builder.Default
    private UserProvider provider = UserProvider.LOCAL;

    // 잔여 토큰량
    @Column(name = "remainingTokens", nullable = false)
    private int remainingTokens;

    // 플랜 만료일
    private LocalDateTime planEndDate;

    // 비밀번호 재설정용 6자리 번호와 만료 시간 로직
    private String resetCode;
    private LocalDateTime resetCodeExpiry;


    // ==========================================
    // 비즈니스 로직
    // ==========================================

    // 비밀번호 변경 적용
    public void updatePassword(String newPassword) {
        this.password = newPassword;
    }

    // 비밀번호 재설정용 6자리 인증번호 발급 및 5분 만료 세팅
    public void generateResetCode(String code) {
        this.resetCode = code;
        this.resetCodeExpiry = LocalDateTime.now().plusMinutes(5);
    }

    // 비밀번호 변경 완료 후 인증번호 및 만료 시간 폐기
    public void clearResetCode() {
        this.resetCode = null;
        this.resetCodeExpiry = null;
    }

    // 인증번호가 만료되었는지 확인하는 메서드
    public boolean isResetCodeExpired() {
        return this.resetCodeExpiry == null || LocalDateTime.now().isAfter(this.resetCodeExpiry);
    }

    // 토큰 차감 메서드
    public void decreaseTokens(int usedTokens) {
        if (this.remainingTokens < usedTokens) {
            throw new IllegalArgumentException("잔여 토큰이 부족합니다.");
        }
        this.remainingTokens -= usedTokens;
    }

    // 정보 수정
    public void updateProfile(String username, String encodePassword) {
        if (username != null && !username.isBlank()) {
            this.username = username;
        }
        if (encodePassword != null && !encodePassword.isBlank()) {
            this.password = encodePassword;
        }
    }

    // 회원 탈퇴
    public void withdraw() {
        if (this.status == UserStatus.WITHDRAWN) {
            throw new IllegalArgumentException("탈퇴 처리된 계정입니다.");
        }
        this.status = UserStatus.WITHDRAWN;
    }

    // 관리자 전용 계정 상태 변경
    public void updateStatus(UserStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("유효하지 않는 상태입니다.");
        }
        this.status = status;
    }

    // 시간에 맞춰 초기화되는 토큰량
    public void resetTokens() {
        if (this.plan != null) {
            this.remainingTokens = this.plan.getLimitTokens();
        } else {
            this.remainingTokens = 5000;
        }
    }

    // 새로운 플랜으로 업그레이드시 토큰 충전 및 만료일 세팅
    public void upgradePlan(PlanEntity newPlan) {
        this.plan = newPlan;
        this.remainingTokens = newPlan.getLimitTokens();

        if ("BASIC".equals(newPlan.getName())) {
            this.planEndDate = null;
        } else {
            this.planEndDate = LocalDateTime.now().plusDays(30);
        }
    }

    // 현재 유저의 유료 플랜이 만료되었는지 확인
    public boolean isPlanExpired() {
        return this.planEndDate != null && LocalDateTime.now().isAfter(this.planEndDate);
    }

    // 기간 만료 시 다시 BASIC 플랜으로 강등
    public void downgradeToBasic(PlanEntity basicPlan) {
        if (basicPlan == null || !"BASIC".equals(basicPlan.getName())) {
            throw new IllegalArgumentException("유효하지 않은 기본(BASIC) 플랜 정보입니다.");
        }
        this.plan = basicPlan;
        this.planEndDate = null;
    }
}