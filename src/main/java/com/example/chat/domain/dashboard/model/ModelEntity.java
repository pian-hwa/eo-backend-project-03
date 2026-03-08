package com.example.chat.domain.dashboard.model;

import com.example.chat.domain.dashboard.PlanModelEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "models")
public class ModelEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "prompt", nullable = false)
    private String prompt;

    @OneToMany(mappedBy = "model", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlanModelEntity> planList = new ArrayList<>();

    @Builder
    public ModelEntity(String name, String prompt) {
        this.name = name;
        this.prompt = prompt;
    }

    public ModelEntity updateName(String name) {
        this.name = name;
        
        return this;
    }

    public ModelEntity updatePrompt(String prompt) {
        this.prompt = prompt;

        return this;
    }

    public ModelEntity update(String name, String prompt) {
        this.name = name;
        this.prompt = prompt;

        return this;
    }
}
