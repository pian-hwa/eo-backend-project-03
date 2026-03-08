package com.example.chat.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public enum IdRule {
    USER("ac"),
    PLAN("pl"),
    MODEL("ml"),
    PLANMODEL("pm"),
    RESET("rs"),
    CHATSESSION("cs"),
    CHATLOG("cl");

    private final String key;

    public static String generateUUID(IdRule id) {
        return id.getKey() + "-" + UUID.randomUUID();
    }
}