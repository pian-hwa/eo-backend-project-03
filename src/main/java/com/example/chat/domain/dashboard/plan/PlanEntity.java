package com.example.chat.domain.dashboard.plan;

import com.example.chat.domain.BaseTimeEntity;
import com.example.chat.domain.IdRule;
import com.example.chat.domain.dashboard.PlanModelEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "plans")
public class PlanEntity extends BaseTimeEntity {
    @Id
    @Column(name = "id", nullable = false)
    private String id = IdRule.generateUUID(IdRule.PLAN);

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "limit_tokens", nullable = false)
    private Long limitTokens;

    @OneToMany(mappedBy = "plan")
    private List<PlanModelEntity> availableModelList = new ArrayList<>();

    @Column(name = "price", nullable = false)
    private Integer price;

    @Builder
    public PlanEntity(String name, Long limitTokens, List<PlanModelEntity> availableModelList, Integer price) {
        this.name = name;
        this.limitTokens = limitTokens;
        this.availableModelList = availableModelList;
        this.price = price;
    }

    public PlanEntity updateName(String name) {
        this.name = name;

        return this;
    }

    public PlanEntity updateLimitTokens(Long limitTokens) {
        this.limitTokens = limitTokens;

        return this;
    }

    public PlanEntity updateAvailableModelList(List<PlanModelEntity> availableModelList) {
        this.availableModelList = availableModelList;

        return this;
    }

    public PlanEntity updatePrice(Integer price) {
        this.price = price;

        return this;
    }

    public PlanEntity update(String name, Long limitTokens, List<PlanModelEntity> availableModelList, Integer price) {
        this.name = name;
        this.limitTokens = limitTokens;
        this.availableModelList = availableModelList;
        this.price = price;

        return this;
    }
}
