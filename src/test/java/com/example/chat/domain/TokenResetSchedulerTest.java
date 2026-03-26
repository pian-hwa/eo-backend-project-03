package com.example.chat.domain;

import com.example.chat.domain.chat.TokenResetScheduler;
import com.example.chat.domain.plan.PlanEntity;
import com.example.chat.domain.user.UserEntity;
import com.example.chat.domain.user.dto.UserDto;
import com.example.chat.domain.user.user_enum.UserRole;
import com.example.chat.domain.user.user_enum.UserStatus;
import com.example.chat.repository.PlanRepository;
import com.example.chat.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TokenResetSchedulerTest {

    @InjectMocks
    private TokenResetScheduler tokenResetScheduler;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PlanRepository planRepository;

    private PlanEntity basicPlan;
    private PlanEntity proPlan;
    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        // 가짜 플랜 데이터 세팅
        basicPlan = PlanEntity.builder().name("BASIC").limitTokens(5000).build();
        proPlan = PlanEntity.builder().name("PRO").limitTokens(50000).build();

        // 가짜 유저 세팅 (처음엔 BASIC)
        testUser = UserEntity.builder()
                .id("test-user-id")
                .email("test@gmail.com")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .plan(basicPlan)
                .remainingTokens(5000)
                .build();
    }

    @Test
    @DisplayName("검증 1: 유저가 PRO 플랜 결제 시 30일 만료일이 세팅되고 UserDto에 정상 표시된다")
    void testUserDtoPlanEndDate() {
        // Given: 유저가 PRO 플랜으로 업그레이드 함
        testUser.upgradePlan(proPlan);

        // When: 프론트엔드에 보낼 DTO로 변환
        UserDto.Response response = UserDto.Response.fromEntity(testUser);

        // Then: DTO에 만료일이 정확히 세팅되어 있어야 함
        assertThat(response.planName()).isEqualTo("PRO");
        assertThat(response.planEndDate()).isNotNull();

        // 현재 시간으로부터 대략 30일 뒤인지 검증 (오차 감안하여 미래 시간인지 확인)
        assertThat(response.planEndDate()).isAfter(LocalDateTime.now().plusDays(29));
        System.out.println("✅ 화면에 전달될 만료일: " + response.planEndDate());
    }

    @Test
    @DisplayName("검증 2: 구독 만료일이 지난 유저는 스케줄러에 의해 BASIC으로 강등된다")
    void testSchedulerDowngradesExpiredUser() {
        // Given
        testUser.upgradePlan(proPlan);

        // 강제로 만료일을 어제(과거)로 조작 (ReflectionTestUtils 사용)
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        ReflectionTestUtils.setField(testUser, "planEndDate", yesterday);

        // DB에서 유저와 BASIC 플랜을 꺼내오도록 Mocking
        given(userRepository.findAll()).willReturn(List.of(testUser));
        given(planRepository.findByName("BASIC")).willReturn(Optional.of(basicPlan));

        // When: 밤 12시 정각이 되어 스케줄러 작동
        tokenResetScheduler.resetDailyTokens();

        // Then
        // 1. 유저의 플랜이 BASIC으로 강등되었는지 확인
        assertThat(testUser.getPlan().getName()).isEqualTo("BASIC");

        // 2. 만료일이 다시 삭제(null) 되었는지 확인
        assertThat(testUser.getPlanEndDate()).isNull();

        // 3. 토큰이 PRO의 50,000개가 아니라 BASIC의 5,000개로 깎였는지 확인
        assertThat(testUser.getRemainingTokens()).isEqualTo(5000);

        System.out.println("✅ 구독이 만료된 유저가 " + testUser.getPlan().getName() + " 플랜으로 강등되었습니다.");
    }

    @Test
    @DisplayName("검증 3: 구독 기간이 아직 남은 유저는 강등되지 않고 토큰만 충전된다")
    void testSchedulerKeepsActiveUser() {
        // Given
        testUser.upgradePlan(proPlan);

        // 만료일이 15일 남은 상황으로 조작
        LocalDateTime futureDate = LocalDateTime.now().plusDays(15);
        ReflectionTestUtils.setField(testUser, "planEndDate", futureDate);

        // 오늘 토큰을 10개만 남기고 다 썼다고 가정
        ReflectionTestUtils.setField(testUser, "remainingTokens", 10);

        given(userRepository.findAll()).willReturn(List.of(testUser));
        given(planRepository.findByName("BASIC")).willReturn(Optional.of(basicPlan));

        // When: 밤 12시 스케줄러 작동
        tokenResetScheduler.resetDailyTokens();

        // Then
        // 1. 플랜은 여전히 PRO 유지
        assertThat(testUser.getPlan().getName()).isEqualTo("PRO");

        // 2. 만료일도 그대로 유지됨
        assertThat(testUser.getPlanEndDate()).isNotNull();

        // 3. 다 썼던 토큰이 PRO 한도(50,000개)로 꽉 충전되었는지 확인
        assertThat(testUser.getRemainingTokens()).isEqualTo(50000);

        System.out.println("✅ 만료일이 남은 유저는 강등되지 않고 " + testUser.getRemainingTokens() + " 토큰이 충전되었습니다.");
    }
}