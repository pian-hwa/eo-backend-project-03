package com.example.chat.repository;

import com.example.chat.domain.payment.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<PaymentEntity, String> {

    // 특정 유저의 결제 내역 조회
    List<PaymentEntity> findAllByUserIdOrderByCreatedAtDesc(String user);
}
