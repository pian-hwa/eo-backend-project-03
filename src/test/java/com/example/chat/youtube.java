package com.example.chat;

import io.github.thoroldvix.api.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@SpringBootTest
class youtube {

    @Test
    void canDoTest() {
        String videoId = "gnmJcJOOhZQ";

        try {
            YoutubeTranscriptApi api = TranscriptApiFactory.createDefault();
            // 자막 데이터를 가져옵니다.
            TranscriptContent content = api.getTranscript(videoId, "ko", "en");

            // 1. 원본 확인용 (선택 사항)
            System.out.println("--- 원본 데이터 개수: " + content.getContent().size() + " ---");

            // 2. FastAPI 규격에 맞게 변환 및 래핑
            String finalPayload = wrapForFastApi(content);

            System.out.println("\n--- FastAPI 전송용 최종 구조 (정제됨) ---");
            System.out.println(finalPayload);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String wrapForFastApi(TranscriptContent content) {
        // getContent()를 사용하여 Fragment 리스트에 접근합니다.
        String formattedItems = content.getContent().stream()
                .map(f -> String.format("{\"timestamp\": \"%s\", \"content\": \"%s\"}",
                        formatTime(f.getStart()),
                        escapeJson(f.getText())))
                .collect(Collectors.joining(",\n        "));

        // 전체 구조 래핑 (SummarySubtitleRequest 규격)
        return "{\n" +
                "  \"subtitle\": [\n" +
                "    {\n" +
                "      \"chapter_idx\": 1,\n" +
                "      \"chapter_title\": \"Auto Generated\",\n" +
                "      \"text\": [\n" +
                "        " + formattedItems + "\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

    // 초(double)를 HH:MM:SS 포맷으로 변환
    private static String formatTime(double seconds) {
        // 24시간을 넘어가는 영상일 경우를 대비해 86400초(하루)로 나머지 연산을 수행합니다.
        return LocalTime.ofSecondOfDay((long) seconds % 86400)
                .format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    // JSON 문자열 내 특수문자 처리
    private static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", "");
    }
}