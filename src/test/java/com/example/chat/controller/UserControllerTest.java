package com.example.chat.controller;

import com.example.chat.domain.plan.PlanEntity;
import com.example.chat.domain.user.UserEntity;
import com.example.chat.domain.user.dto.UserDto;
import com.example.chat.domain.user.user_enum.UserRole;
import com.example.chat.domain.user.user_enum.UserStatus;
import com.example.chat.repository.PlanRepository;
import com.example.chat.repository.user.UserRepository;
import com.example.chat.security.CustomUserDetails;
import com.example.chat.service.user.MailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "jwt.secret-key=7kQ43HmV4oMHcZgyNfsxbVoYDSdN9WBvJCn8KKM3k5v7kQ43HmV4oMHcZgyNfsxbVoYDSdN9WBvJCn8KKM3k5v",
        "jwt.access-token-expiration=1h",
        "jwt.refresh-token-expiration=7d"
})
@AutoConfigureMockMvc
@Transactional
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PlanRepository planRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    // 🚨 ObjectMapper 주입 시 발생하는 NPE 방지를 위해 직접 초기화합니다.
    private ObjectMapper objectMapper;

    // 실제 메일 발송이 일어나지 않도록 MailService만 가짜(Mock) 객체로 주입합니다.
    @MockitoBean
    private MailService mailService;

    private UserEntity testUser;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        // ObjectMapper 수동 초기화 (NPE 방지 및 시간 포맷 지원)
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());

        // 1. 기본 플랜 세팅
        PlanEntity basicPlan = planRepository.findByName("BASIC")
                .orElseGet(() -> planRepository.save(PlanEntity.builder()
                        .name("BASIC")
                        .price(0)
                        .limitTokens(5000)
                        .availableModels("gpt-3.5-turbo,gpt-4o-mini")
                        .build()));

        // 2. 테스트용 실제 유저 DB 저장
        testUser = userRepository.findByEmail("test@gmail.com").orElseGet(() ->
                userRepository.save(UserEntity.builder()
                        .email("test@gmail.com")
                        .username("tester")
                        .password(passwordEncoder.encode("Password123!"))
                        .role(UserRole.USER)
                        .status(UserStatus.ACTIVE)
                        .plan(basicPlan)
                        .remainingTokens(5000)
                        .build())
        );

        // 3. Security 인증 객체 세팅 (로그인이 필요한 API 테스트용)
        userDetails = mock(CustomUserDetails.class);
        given(userDetails.getId()).willReturn(testUser.getId());
        given(userDetails.getUsername()).willReturn(testUser.getEmail());
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(userDetails).getAuthorities();
    }

    @Test
    @DisplayName("로그인 성공 - 토큰 2개가 담긴 HTTP-Only 쿠키가 정상적으로 발급된다")
    void login_success_cookieCreated() throws Exception {
        // given: 실제 DB에 저장된 testUser의 정보로 로그인 시도
        UserDto.LoginRequest request = new UserDto.LoginRequest("test@gmail.com", "Password123!");

        // when & then
        mockMvc.perform(post("/api/users/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                // 🚨 로그인 성공 시 쿠키가 정상적으로 구워지는지 확인
                .andExpect(cookie().exists("accessToken"))
                .andExpect(cookie().httpOnly("accessToken", true))
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true));
    }

    @Test
    @DisplayName("비밀번호 재설정 성공 - 응답 쿠키의 Max-Age가 0이 되어 강제 로그아웃 처리된다")
    void resetPassword_success_cookieDeleted() throws Exception {
        // given: 실제 DB의 테스트 유저에게 재설정 인증번호를 세팅 (6자리 숫자로 변경)
        String validCode = "123456";
        testUser.generateResetCode(validCode);
        userRepository.saveAndFlush(testUser);

        // PasswordResetRequest DTO의 필드명도 resetCode로 일치시킴
        UserDto.PasswordResetRequest request = new UserDto.PasswordResetRequest(validCode, "newPassword123!");

        // when & then
        mockMvc.perform(post("/api/users/password/reset")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                // 🚨 비밀번호 변경 직후 기존 쿠키(출입증) 폐기 명령이 떨어지는지 확인
                .andExpect(cookie().maxAge("accessToken", 0))
                .andExpect(cookie().maxAge("refreshToken", 0));
    }

    @Test
    @DisplayName("로그아웃 성공 - 응답 쿠키의 Max-Age가 0이 되어 삭제된다")
    void logout_success_cookieDeleted() throws Exception {
        // when & then
        mockMvc.perform(post("/api/users/logout")
                        .with(csrf())
                        .with(user(userDetails))) // 🚨 401 해결: 로그인된 유저임을 인증 객체로 전달!
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                // 🚨 단순 로그아웃 시에도 쿠키가 올바르게 폐기되는지 확인
                .andExpect(cookie().maxAge("accessToken", 0))
                .andExpect(cookie().maxAge("refreshToken", 0));
    }

    @Test
    @DisplayName("내 정보 조회 성공 - 인증된 사용자 정보 반환")
    void getMyInfo_success() throws Exception {
        // when & then: 인증된 유저(userDetails)로 보안 API 요청
        mockMvc.perform(get("/api/users/me")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@gmail.com"))
                .andExpect(jsonPath("$.data.username").value("tester"));
    }
}