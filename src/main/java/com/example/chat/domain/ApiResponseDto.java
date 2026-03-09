package com.example.chat.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponseDto<T> {

    private final boolean success;
    private final String message;
    private final T data;

    // 성공 시 응답 (데이터 있음)
    public static <T> ApiResponseDto<T> success(String message, T data) {
        return new ApiResponseDto<>(true, message, data);
    }

    // 성공 시 응답 (데이터 없음)
    public static <T> ApiResponseDto<T> success(String message) {
        return new ApiResponseDto<>(true, message, null);
    }

    // 실패 시 응답
    public static <T> ApiResponseDto<T> fail(String message) {
        return new ApiResponseDto<>(false, message, null);
    }
}