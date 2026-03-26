package com.example.chat.domain.chat;

import com.example.chat.domain.plan.PlanEntity;
import com.example.chat.domain.user.UserEntity;
import com.example.chat.repository.PlanRepository;
import com.example.chat.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenResetScheduler {

    private final UserRepository userRepository;
    private final PlanRepository planRepository;

    /**
     * 매일 자정(00:00:00)에 실행되어 모든 사용자의 토큰을 플랜 한도량으로 리셋
     * 구독 기간이 만료된 사용자를 BASIC 플랜으로 강등합니다.
     * Cron 표현식: "초 분 시 일 월 요일"
     */
    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    public void resetDailyTokens() {
        log.info("[스케줄러] 매일 자정 토큰 초기화 및 구독 만료 확인 작업을 시작합니다.");

        // 모든 사용자 목록 조회 (작음 규모이기에 findAll 사용)
        List<UserEntity> allUsers = userRepository.findAll();

        // 강등시킬 때 사용할 BASIC 플랜 정보를 미리 DB에서 찾음
        PlanEntity basicPlan = planRepository.findByName("BASIC").orElse(null);

        int successCount = 0;
        int downgradeCount = 0;

        for (UserEntity user : allUsers) {
            try {
                // 플랜 만료 여부 확인 및 강등 처리
                if (user.isPlanExpired() && basicPlan != null) {
                    user.downgradeToBasic(basicPlan);
                    log.info("[스케줄러] 유저 ID: {} - 구독 만료로 BASIC 플랜으로 강등되었습니다.", user.getId());
                    downgradeCount++;
                }

                // 각 유저의 엔티티 내부 리셋 로직 호출
                // (만약 위에서 강등되었다면, 자동으로 BASIC 플랜의 한도인 5000으로 리셋)
                user.resetTokens();
                successCount++;

            } catch (Exception e) {
                log.error("[스케줄러] 유저 ID: {} 작업 중 오류 발생: {}", user.getId(), e.getMessage());
            }
        }

        log.info("[스케줄러] 작업 완료. 대상: {}명 / 토큰 초기화 성공: {}명 / 플랜 강등: {}명",
                allUsers.size(), successCount, downgradeCount);
    }
}