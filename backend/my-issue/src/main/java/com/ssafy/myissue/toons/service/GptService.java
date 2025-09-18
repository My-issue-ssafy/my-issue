package com.ssafy.myissue.toons.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GptService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gms.key}")
    private String gmsKey;

    public String summarize(String content) throws JsonProcessingException {
        String url = "https://gms.ssafy.io/gmsapi/api.openai.com/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(gmsKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-5-nano");
        body.put("messages", List.of(
                Map.of("role","developer","content","Answer in Korean"),
                Map.of("role","user","content", "다음 기사를 3~4문장으로 요약해줘 :\n" + content)
        ));

        // HttpEntity는 Jackson이 직렬화
        // writeValueAsString은 명확한 Json 문자열 생성
        String jsonBody = objectMapper.writeValueAsString(body); // 이것만 추가함
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            return root
                    .path("choices").get(0)
                    .path("message")
                    .path("content")
                    .asText();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("GPT 응답 파싱 실패", e);
        }
    }
}
