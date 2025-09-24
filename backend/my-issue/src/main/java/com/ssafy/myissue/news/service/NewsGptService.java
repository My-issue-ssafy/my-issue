package com.ssafy.myissue.news.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NewsGptService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gms.key}")
    private String gmsKey;

    public String askAbout(String title, String outlet, String author, String category,
                           String createdAt, String articleText, String question) {

        String url = "https://gms.ssafy.io/gmsapi/api.openai.com/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(gmsKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-5-nano");
        body.put("messages", List.of(
                Map.of("role","developer","content","Answer in Korean"),
                Map.of("role","user","content", makeUserPrompt(title, outlet, author, category, createdAt, articleText, question))
        ));

        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("GPT 응답 파싱 실패", e);
        }
    }

    private String makeUserPrompt(String title, String outlet, String author, String category,
                                  String createdAt, String articleText, String question) {
        // 기사 기반으로만 답하도록 지시 — 사용자 메시지에 포함
        return """
                다음 [기사]만을 근거로 질문에 답해줘. 외부 지식은 사용하지 말고,
                기사에 명시된 사실에서 자연스럽게 **직접 도출되는 간단한 추론**(문맥 연결, 인과/의도/영향)은 허용해.
                답변은 2~4문장으로 간결하게 써.
                정말 기사로부터 추론도 불가하면 그때만 "기사에 없는 내용입니다."라고 답해.
                
                [기사 제목] %s
                [언론사/작성일] %s / %s
                [기자/카테고리] %s / %s
                
                [기사 본문] %s
                [사용자 질문] %s
                """.formatted(nz(title), nz(outlet), nz(createdAt), nz(author), nz(category),
                nz(articleText), nz(question));
    }

    private String nz(Object o) { return (o == null) ? "" : String.valueOf(o); }
}
