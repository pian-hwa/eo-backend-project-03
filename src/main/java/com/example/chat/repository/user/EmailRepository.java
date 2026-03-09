package com.example.chat.repository.user;

import com.example.chat.domain.user.EmailVerificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailRepository extends JpaRepository<EmailVerificationEntity, String> {
}
