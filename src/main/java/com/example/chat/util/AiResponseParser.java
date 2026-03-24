package com.example.chat.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AiResponseParser {

    /**
     * 스트리밍용 비표준 JSON 문자열에서 'complete' 상태의 최종 텍스트만 추출합니다.
     * @param rawResponse KDT API로부터 받은 원본 문자열
     * @return 깔끔하게 정제된 최종 텍스트
     */
    public static String extractCleanText(String rawResponse) {
        // 원본 데이터가 비어있는지 확인
        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            return "";
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            // 로그 확인 결과 홑따옴표(')를 사용 중이므로, 이를 허용하도록 설정
            mapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);

            // "}{" 형태로 붙어있는 문자열을 Jackson이 읽을 수 있는 JSON 배열 형태 "[{}, {}]" 로 변환
            String jsonArrayString = "[" + rawResponse.replaceAll("\\}\\s*\\{", "},{") + "]";

            // JSON 배열로 파싱
            JsonNode arrayNode = mapper.readTree(jsonArrayString);

            // 배열을 순회하며 type이 'complete'인 마지막 응답을 찾음
            for (JsonNode node : arrayNode) {
                if (node.has("type") && "complete".equals(node.get("type").asText())) {
                    JsonNode dataNode = node.get("data");
                    if (dataNode != null && dataNode.has("content")) {
                        return dataNode.get("content").asText(); // 최종 텍스트 반환
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("AI 응답 텍스트 파싱 실패: " + e.getMessage());

            // 확실하지 않은 응답 포맷으로 인해 파싱이 실패했을 경우의 안전망(Fallback)
            if (rawResponse.length() > 500) {
                return rawResponse.substring(0, 500) + "\n...(내용이 길어 생략됨)";
            }
        }

        // 파싱 로직을 통과하지 못했다면 원본 반환
        return rawResponse;
    }
}