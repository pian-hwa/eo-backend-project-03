package com.example.chat.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "email_verifications")
public class EmailVerificationEntity {

    @Id
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    // 6자리 인증 번호
    @Column(name = "verificationCode", nullable = false)
    private String verificationCode;

    // 인증 성공 여부 (초기 false, 맞추면 true)
    @Column(name = "isVerified", nullable = false)
    private boolean isVerified;

    // 인증 만료 시간
    @Column(name = "expiryDate", nullable = false)
    private LocalDateTime expiryDate;

    // 인증 성공 처리 메서드
    public void verifySuccess() {
        this.isVerified = true;
    }

    // 인증 번호 재발송 시 업데이트 메서드
    public void updateCode(String newCode, LocalDateTime newExpiryDate) {
        this.verificationCode = newCode;
        this.expiryDate = newExpiryDate;
        this.isVerified = false;
    }
}
