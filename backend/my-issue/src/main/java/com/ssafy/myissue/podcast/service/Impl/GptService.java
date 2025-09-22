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
        String systemPrompt =
            "너는 2명의 진행자가 뉴스를 주제로 티키타카 대화를 나누는 팟캐스트 대본 전문가다. " +
                "대본은 반드시 JSON 배열 형식으로만 출력하며, 실제 라디오처럼 자연스럽고 생동감 있는 대화를 만든다.";

        String userPrompt =
            "아래는 어제의 주요 뉴스 목록이다. 각 뉴스는 제목과 요약으로 구성되어 있다:\n\n" +
                news + "\n\n" +
                "조건:\n" +
                "- 두 명의 진행자는 [1], [2]로 구분한다.\n" +
                "- 각 발화는 2~3문장, 10~15초 분량으로 작성한다.\n" +
                "- 절대 뉴스 기사처럼 나열하거나 에세이식으로 설명하지 말고, 반드시 서로 주고받는 대화로 작성한다.\n" +
                "- 한 명이 설명하면 다른 한 명은 질문·맞장구·농담·짧은 의견을 덧붙이며 티키타카 대화를 이어간다.\n" +
                "- 진행자는 항상 서로의 말을 받아서 연결한다. 반박이나 감탄도 섞어서 실제 대화처럼 만든다.\n" +
                "- 각 뉴스는 최소 6~10턴 이상 대화로 풀어낸다.\n" +
                "- 뉴스 전환 시 '다음은'이라고만 하지 말고, 앞 뉴스와 연결되도록 자연스럽게 이어간다.\n" +
                "- 전체 대본은 최소 15분 이상 분량이 되도록 충분히 길게 작성한다.\n" +
                "- 시작은 항상 [1, \"안녕하세요, 어제 하루 동안 있었던 중요한 소식들을 함께 살펴보겠습니다.\"] 로 시작한다.\n" +
                "- 반드시 JSON 배열 형식으로만 출력한다.\n" +
                "예시:\n" +
                "[[1, \"안녕하세요, 어제 하루 동안 있었던 중요한 소식들을 함께 살펴보겠습니다.\"], " +
                "[2, \"네, 첫 번째 뉴스는 군인 미순직 재조사 소식이었습니다.\"], " +
                "[1, \"맞습니다, 숫자가 3만8천 명이 넘는다니 충격적입니다.\"], " +
                "[2, \"특히 유가족 입장에서는 시급한 보상이 필요한 문제일 수밖에 없습니다.\"]]";


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
