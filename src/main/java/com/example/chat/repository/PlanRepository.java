package com.example.chat.repository;

import com.example.chat.domain.plan.PlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlanRepository extends JpaRepository<PlanEntity, String> {

    // 어떤 플랜인지 이름으로 찾기 ("BASIC", "PRO")
    Optional<PlanEntity> findByName(String name);
}
