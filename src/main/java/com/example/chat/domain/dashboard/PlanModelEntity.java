package com.example.chat.domain.dashboard;

import com.example.chat.domain.IdRule;
import com.example.chat.domain.dashboard.model.ModelEntity;
import com.example.chat.domain.dashboard.plan.PlanEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "available_models")
public class PlanModelEntity {
    @Id
    @Column(name = "id", nullable = false, unique = true)
    private String id = IdRule.generateUUID(IdRule.PLANMODEL);

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private PlanEntity plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id")
    private ModelEntity model;
}
