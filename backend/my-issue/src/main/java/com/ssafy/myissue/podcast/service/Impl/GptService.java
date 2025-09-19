package com.ssafy.myissue.podcast.service.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service("podcastGptService")
public class GptService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gms.key}")
    private String gmsKey;

    private String GPT_URL = "https://gms.ssafy.io/gmsapi/api.openai.com/v1/chat/completions";

    // 공통 GPT 호출 메서드
    private String callGpt(String model, String systemPrompt, String userPrompt) {
        try {
            // 요청 헤더
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(gmsKey);

            // 요청 바디
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("messages", List.of(
                    Map.of("role", "developer", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            ));

            String jsonBody = objectMapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(GPT_URL, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            throw new RuntimeException("GPT 호출 실패", e);
        }
    }
    // 팟캐스트 대본 생성
    public List<List<String>> generateScript(String news) {
        String systemPrompt = "너는 2명의 진행자가 대화하는 팟캐스트 대본 작성 전문가다. 반드시 JSON 배열 형식으로만 출력한다.";
        String userPrompt =
                "너는 2명의 진행자가 대화하는 팟캐스트 대본을 작성해야 한다.\n" +
                        "아래는 어제의 주요 뉴스 목록이다. 각 뉴스는 제목과 요약으로 구성되어 있다:\n\n" +
                        news + "\n\n" +
                        "조건:\n" +
                        "- 두 명의 진행자는 [1], [2]로 구분한다.\n" +
                        "- 각 발화는 뉴스 내용을 자연스럽게 풀어서 대화하는 형식이다.\n" +
                        "- 최소 10분 분량의 분량으로 작성한다.\n" +
                        "- 출력은 JSON 배열로 반환한다. 예시:\n" +
                        "[[1, \"안녕하세요, 오늘은 어제 있었던 주요 뉴스를 같이 이야기해보죠.\"], " +
                        "[2, \"네, 첫 번째 뉴스는 엑스페릭스의 AI 비만 치료제 발표 소식이었죠.\"], [1, \"맞습니다...\"]]";

        String content = callGpt("gpt-5-nano", systemPrompt, userPrompt);

        try {
            return objectMapper.readValue(content, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("GPT 대본 응답 파싱 실패", e);
        }
    }

    // 키워드 추출 메서드
    public List<String> extractKeywords(String scripts) {
        String systemPrompt = "너는 텍스트에서 핵심 키워드를 추출하는 전문가다. 반드시 JSON 배열 형식으로만 출력한다.";
        String userPrompt =
                "아래 텍스트에서 핵심 키워드 3~5개를 뽑아줘.\n" +
                        "출력은 반드시 JSON 배열 형식으로만 해.\n\n" +
                        scripts;

        String content = callGpt("gpt-5-nano", systemPrompt, userPrompt);

        try {
            return objectMapper.readValue(content, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("GPT 키워드 응답 파싱 실패", e);
        }

    }
}
