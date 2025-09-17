package com.ssafy.myissue.toons.service;

import com.fasterxml.jackson.core.JsonProcessingException;
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

    @Value("${gms.key}")
    private String gmsKey;

    public String summarize(String content) {
        String url = "https://gms.ssafy.io/gmsapi/api.openai.com/v1/chat/completions";

        // ✅ 요청 body
//        Map<String, Object> body = Map.of(
//                "model", "gpt-5-mini",
//                "messages", List.of(
//                        Map.of(
//                                "role", "developer",
//                                "content", "Answer in Korean"
//                        ),
//                        Map.of(
//                                "role", "user",
//                                "content", "다음 기사를 3~4문장으로 요약해줘:\n" + content
//                        )
//                )
//        );

        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-5-mini");
        body.put("temperature", 0.7);
        body.put("max_tokens", 500);
        body.put("messages", List.of(
                Map.of("role","developer","content","answer in korean"),
                Map.of("role","user","content", "다음 기사를 3~4문장으로 요약해줘 :\n" + content)
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(gmsKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {

            // ✅ 요청 URL 로그
            System.out.println("===== GPT 요청 URL =====");
            System.out.println(url);

            // ✅ 요청 헤더 로그
            System.out.println("===== GPT 요청 헤더 =====");
            headers.forEach((key, value) -> System.out.println(key + " : " + value));

            // ✅ 요청 바디 로그 (JSON pretty print)
            ObjectMapper mapper = new ObjectMapper();
            System.out.println("===== GPT 요청 바디 =====");
            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(body));

            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

            if (response == null) {
                throw new RuntimeException("GPT 응답이 null입니다.");
            }

            System.out.println("GPT 응답 전체: " + response); // 🔍 응답 전체 출력

            // ✅ choices 파싱
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

        } catch (HttpClientErrorException e) {
            // ✅ 에러 응답도 찍어서 확인
            System.out.println("GPT 호출 에러 상태코드: " + e.getStatusCode());
            System.out.println("GPT 호출 에러 바디: " + e.getResponseBodyAsString());

            try {
                // JSON이라면 예쁘게 출력
                ObjectMapper mapper = new ObjectMapper();
                Object json = mapper.readValue(e.getResponseBodyAsString(), Object.class);
                String prettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
                System.out.println("응답 바디 (pretty):\n" + prettyJson);
            } catch (Exception parseEx) {
                System.out.println("응답을 JSON으로 파싱할 수 없음: " + parseEx.getMessage());
            }

            throw e;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
