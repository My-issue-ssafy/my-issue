package com.ssafy.myissue.toons.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class GptService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${gms.key}")
    private String gmsKey;

    public String summarize(String content) {
        String url = "https://gms.ssafy.io/gmsapi/api.openai.com/v1/chat/completions";

        Map<String, Object> body = Map.of(
                "model", "gpt-5-nano",
                "messages", List.of(
                        Map.of("role", "developer", "content", "Answer in Korean"),
                        Map.of("role", "user", "content", "다음 기사를 3~4문장으로 요약해줘:\n" + content)
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(gmsKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("GPT 응답에 choices가 없습니다: " + response);
        }

        Map<String, Object> choice = choices.get(0);
        Map<String, Object> message = (Map<String, Object>) choice.get("message");

        if (message == null || message.get("content") == null) {
            throw new RuntimeException("GPT 응답에 message/content가 없습니다: " + response);
        }

        return message.get("content").toString().trim();
    }
}
