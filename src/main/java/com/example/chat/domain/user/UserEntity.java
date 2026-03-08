package com.example.chat.domain.user;

import com.example.chat.domain.BaseTimeEntity;
import com.example.chat.domain.IdRule;
import com.example.chat.domain.dashboard.plan.PlanEntity;
import com.example.chat.domain.user.auth.AuthTokenEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
@AllArgsConstructor
public class UserEntity extends BaseTimeEntity {
    @Id
    @Column(name = "id", nullable = false, unique = true)
    private String id = IdRule.generateUUID(IdRule.USER);;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "role", nullable = false)
    private UserRole role = UserRole.DEFAULT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private PlanEntity plan = null;

    @Column(name = "remaining_tokens", nullable = false)
    private Long remainingTokens = 0L;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auth_token")
    private AuthTokenEntity authToken = null;

    @Builder
    public UserEntity(String email, String password, String username) {
        this.email = email;
        this.password = password;
        this.username = username;
    }
}
