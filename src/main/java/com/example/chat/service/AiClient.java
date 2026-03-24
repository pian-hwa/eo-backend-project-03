package com.example.chat.service;

import com.example.chat.domain.chat.ChatType;
import com.example.chat.domain.chat.ai.AiDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.thoroldvix.api.TranscriptApiFactory;
import io.github.thoroldvix.api.TranscriptContent;
import io.github.thoroldvix.api.YoutubeTranscriptApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
public class AiClient {

    private final WebClient alanWebClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${alan.client-id}") // application-secret.yml의 client-id와 매칭
    private String clientId;

    // WebClient 빈 주입 에러 방지용 직접 초기화
    public AiClient(@Value("${alan.base-url:https://kdt-api-function.azurewebsites.net/api/v1}") String baseUrl) {
        this.alanWebClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    /**
     * [ChatService 연동 핵심 메서드]
     */
    public AiDto.Response getAiAnswer(String content, String modelName, ChatType chatType) {
        try {
            // CHAT(일반 대화)일 경우 파싱 에러를 방지하기 위해 스트리밍 전용 처리 메서드로 바로 반환합니다.
            if (chatType == ChatType.CHAT) {
                return askQuestionWithStream(content, modelName);
            }

            // 요약 기능 등은 기존 방식(단발성) 사용
            String rawResponse = switch (chatType) {
                case SUMMARY -> summarizePage(content);
                case YOUTUBE -> askYoutubeSummary(content);
                default -> "";
            };

            return parseResponse(rawResponse);

        } catch (Exception e) {
            log.error("AI 호출 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("[" + chatType.getDescription() + "] API 호출 실패: " + e.getMessage());
        }
    }

    /**
     * 새로운 세션이 생성될 때 호출하여 이전 문맥을 비웁니다.
     */
    public void clearAiHistory() {
        log.info("앨런 AI 상태 초기화 요청 (client_id: {})", clientId);
        try {
            alanWebClient.method(HttpMethod.DELETE)
                    .uri("/reset-state")
                    .bodyValue(Map.of("client_id", clientId))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            log.warn("상태 초기화 중 오류 발생 (무시): {}", e.getMessage());
        }
    }

    // =========================================================================
    // 외부 API(Alan AI) 통신 헬퍼 메서드
    // =========================================================================

    /**
     * 일반 질문
     */
    private AiDto.Response askQuestionWithStream(String content, String modelName) {
        log.info("앨런 SSE 스트리밍 질문 요청 (모델: {}): {}", modelName, content);

        // WebClient가 한글이나 특수문자, 띄어쓰기를 URL에 맞게 자동으로 인코딩해 줍니다.
        Flux<String> responseStream = alanWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/question/sse-streaming")
                        .queryParam("client_id", "{clientId}")
                        .queryParam("content", "{content}")
                        .queryParam("model", "{modelName}")
                        .build(clientId, content, modelName)
                )
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnError(e -> log.error("streamChat 오류 = {}", e.getMessage()));

        List<String> chunks = responseStream.collectList().block();

        if (chunks == null || chunks.isEmpty()) {
            return new AiDto.Response("AI 응답을 가져오지 못했습니다.", 10);
        }

        StringBuilder finalAnswer = new StringBuilder();
        int totalTokens = 10;

        for (String chunk : chunks) {
            try {
                if (chunk == null || chunk.trim().isEmpty()) continue;

                JsonNode root = objectMapper.readTree(chunk);

                if (root.has("answer")) {
                    finalAnswer.append(root.get("answer").asText());
                } else if (root.has("content")) {
                    finalAnswer.append(root.get("content").asText());
                } else {
                    finalAnswer.append(chunk);
                }

                if (root.has("used_tokens")) {
                    totalTokens = root.get("used_tokens").asInt();
                }
            } catch (Exception e) {
                finalAnswer.append(chunk);
            }
        }

        return new AiDto.Response(finalAnswer.toString(), totalTokens);
    }



    /**
     * 페이지 요약 (POST /api/v1/chrome/page/summary)
     */
    private String summarizePage(String content) {
        log.info("앨런 페이지 요약 요청 (길이: {})", content.length());
        return alanWebClient.post()
                .uri("/chrome/page/summary")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("content", content))
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    /**
     * 유튜브 요약 (POST /api/v1/summary-youtube)
     */
    private String askYoutubeSummary(String videoUrl) {
        log.info("앨런 유튜브 요약 요청 URL: {}", videoUrl);

        String videoId = extractVideoId(videoUrl);
        List<Map<String, Object>> subtitleData = fetchYoutubeSubtitle(videoId);

        return alanWebClient.post()
                .uri("/summary-youtube")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("subtitle", subtitleData))
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    // --- JSON 응답 파싱 (단발성 응답용 안전 장치) ---
    private AiDto.Response parseResponse(String rawBody) {
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            String answer = root.has("answer") ? root.get("answer").asText() :
                    root.has("content") ? root.get("content").asText() : rawBody;

            int tokens = root.has("used_tokens") ? root.get("used_tokens").asInt() : 10;
            return new AiDto.Response(answer, tokens);
        } catch (Exception e) {
            return new AiDto.Response(rawBody, 10);
        }
    }

    // --- 유튜브 자막 추출 헬퍼 ---
    private String extractVideoId(String text) {
        if (text == null) return "";

        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(?:v=|youtu\\.be\\/)([a-zA-Z0-9_-]{11})").matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }

        String url = text;
        if (url.contains("v=")) {
            url = url.split("v=")[1].split("&")[0];
        } else if (url.contains("youtu.be/")) {
            url = url.split("youtu.be/")[1].split("\\?")[0];
        }

        return url.replaceAll("[^a-zA-Z0-9_-]", "");
    }

    private List<Map<String, Object>> fetchYoutubeSubtitle(String videoId) {
        try {
            YoutubeTranscriptApi api = TranscriptApiFactory.createDefault();
            TranscriptContent content = api.getTranscript(videoId, "ko", "en");

            List<Map<String, String>> textList = content.getContent().stream()
                    .map(f -> {
                        Map<String, String> item = new HashMap<>();
                        item.put("timestamp", LocalTime.ofSecondOfDay((long) f.getStart() % 86400).format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                        item.put("content", f.getText().replace("\n", " "));
                        return item;
                    }).toList();

            return List.of(Map.of(
                    "chapter_idx", 1,
                    "chapter_title", "동영상 내용",
                    "text", textList
            ));
        } catch (Exception e) {
            log.error("유튜브 자막 추출 실패 (videoId: {}): {}", videoId, e.getMessage());
            return Collections.emptyList();
        }
    }
}