package com.ssafy.myissue.toons.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Base64;

/**
 * OpenAI gpt-image-1을 직접 호출하여 1024x1024 네컷만화 이미지를 생성
 */
@RequiredArgsConstructor
@Service
public class ImageService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.api-key}")
    private String openaiKey;

    public record ImageResult(byte[] data, String mimeType) {}

    public ImageResult generateToonImage(String summary) {
        String url = "https://api.openai.com/v1/images/generations";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openaiKey);

        // (한글 프롬프트)
        String promptTemplate = """
요약: REPLACE_ME

다음 조건을 모두 지켜 '네 컷 만화' 한 장을 생성해줘.

- 한 장의 정사각형(1024×1024) 이미지 안에 4개의 패널이 **명확히 구분**되어 있을 것.
- 각 패널에는 **캐릭터와 배경**이 모두 포함될 것.
- 어떤 언어의 **문자/숫자/자막/말풍선도 절대 넣지 말 것**. (텍스트 완전 금지)
- **실존 인물**은 닮은 느낌의 **가상 캐릭터**로 대체할 것. (실제 인물 초상권 회피)
- **선명한 색감**의 **단순한 코믹 스타일**.
- 위 '요약'의 내용을 **글 없이 시각적으로만** 전달할 것.
""";

        if (summary == null) summary = "";
        String prompt = promptTemplate.replace("REPLACE_ME", summary.trim());

        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-image-1");
        body.put("prompt", prompt);
        body.put("size", "1024x1024");
        body.put("n", 1);
        body.put("quality", "medium"); // 비용 절감

        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);

            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode dataArr = root.path("data");
            if (!dataArr.isArray() || dataArr.isEmpty()) {
                throw new RuntimeException("OpenAI 응답에 data 배열이 없습니다: " + resp.getBody());
            }

            JsonNode first = dataArr.get(0);
            String b64 = first.path("b64_json").asText(null);

            if (b64 == null || b64.isBlank()) {
                // [ADDED] URL 폴백
                String urlField = first.path("url").asText(null);
                if (urlField == null || urlField.isBlank()) {
                    throw new RuntimeException("OpenAI 응답에 b64_json/url 필드가 없습니다: " + resp.getBody());
                }
                ResponseEntity<byte[]> imgResp = restTemplate.getForEntity(urlField, byte[].class);
                byte[] imgBytes = imgResp.getBody();
                if (imgBytes == null || imgBytes.length == 0) {
                    throw new RuntimeException("이미지 URL에서 바이트를 받지 못했습니다: " + urlField);
                }
                return new ImageResult(imgBytes, "image/png");
            }

            byte[] bytes = Base64.getDecoder().decode(b64);
            return new ImageResult(bytes, "image/png");

        } catch (HttpClientErrorException e) {
            // [ADDED] 에러 코드/메시지 파싱 및 조직 미인증 식별
            String bodyStr = e.getResponseBodyAsString(StandardCharsets.UTF_8);
            String msg  = extractErrorMessage(bodyStr);

            // 403 & 'must be verified' → 조직 미인증
            if (e.getStatusCode() == HttpStatus.FORBIDDEN &&
                    msg != null && msg.toLowerCase().contains("must be verified")) {
                throw new RuntimeException("ORG_NOT_VERIFIED: " + msg, e); // [ADDED]
            }

            throw new RuntimeException("OpenAI Images API 요청 실패: " + e.getStatusCode() +
                    " msg=" + msg, e);
        } catch (Exception e) {
            throw new RuntimeException("OpenAI 이미지 생성 실패", e);
        }
    }

    // [ADDED] 에러 JSON에서 message 추출
    private String extractErrorMessage(String body) {
        try {
            JsonNode n = objectMapper.readTree(body);
            return n.path("error").path("message").asText(null);
        } catch (Exception ignored) { return body; }
    }
}
