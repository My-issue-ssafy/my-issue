//package com.ssafy.myissue.toons.service;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.*;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.HttpClientErrorException;
//import org.springframework.web.client.RestTemplate;
//
//import java.nio.charset.StandardCharsets;
//import java.util.*;
//
//@Service
//@RequiredArgsConstructor
//public class ImageService {
//
//    private final RestTemplate restTemplate = new RestTemplate();
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    @Value("${gms.key}")
//    private String gmsKey;
//
//    public record ImageResult(byte[] data, String mimeType) {}
//
//    public ImageResult generateToonImage(String summary) {
//        String url = "https://gms.ssafy.io/gmsapi/generativelanguage.googleapis.com/v1beta/models/"
//                + "imagen-3.0-generate-002:predict?key=" + gmsKey;
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
//
//        // ✅ 요약 내용을 중심에 두고, 네컷 만화 규칙은 보조 지침으로 제공
//        String promptTemplate = """
//        Create a 4-panel comic illustration based on the following news summary:
//
//        SUMMARY: %s
//
//        Requirements:
//        - The comic must clearly visualize the events, characters, or situations described in the summary.
//        - Divide into 4 separate panels in one image.
//        - Each panel should represent a logical progression of the story (beginning → development → climax → ending).
//        - Each panel must include characters and background.
//        - Style: colorful, simple characters, comic/cartoon style.
//        - Absolutely NO text, NO words, NO captions, NO letters, NO numbers.
//        - No speech bubbles, labels, or subtitles.
//        - Only visual storytelling, no writing at all.
//        """;
//
//        if (summary == null) summary = "";
//        String prompt = String.format(promptTemplate, summary.trim());
//
//        Map<String, Object> body = Map.of(
//                "instances", List.of(Map.of("prompt", prompt)),
//                "parameters", Map.of("sampleCount", 1) // 뉴스 1개 → 이미지 1장
//        );
//
//        try {
//            ResponseEntity<String> resp = restTemplate.exchange(
//                    url, HttpMethod.POST,
//                    new HttpEntity<>(body, headers),
//                    String.class
//            );
//
//            JsonNode root = objectMapper.readTree(resp.getBody());
//            JsonNode prediction = root.path("predictions").get(0);
//
//            if (prediction != null) {
//                // bytesBase64Encoded 우선 시도
//                JsonNode base64Node = prediction.get("bytesBase64Encoded");
//                if (base64Node != null && !base64Node.asText().isBlank()) {
//                    byte[] bytes = Base64.getDecoder().decode(base64Node.asText());
//                    return new ImageResult(bytes, "image/png");
//                }
//
//                // 다른 구조 (data[0].b64 or b64_string) 시도
//                JsonNode dataNode = prediction.path("data");
//                if (dataNode.isArray() && dataNode.size() > 0) {
//                    JsonNode first = dataNode.get(0);
//                    JsonNode b64 = first.get("b64");
//                    if (b64 == null) b64 = first.get("b64_string");
//                    if (b64 != null && !b64.asText().isBlank()) {
//                        byte[] bytes = Base64.getDecoder().decode(b64.asText());
//                        return new ImageResult(bytes, "image/png");
//                    }
//                }
//            }
//
//            throw new RuntimeException("Imagen 응답에 이미지 데이터가 없습니다: " + resp.getBody());
//        } catch (HttpClientErrorException e) {
//            String err = e.getResponseBodyAsString(StandardCharsets.UTF_8);
//            throw new RuntimeException("Imagen API 요청 실패: " + e.getStatusCode() + " body=" + err, e);
//        } catch (Exception e) {
//            throw new RuntimeException("Imagen 이미지 생성 실패", e);
//        }
//    }
//}

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


@RequiredArgsConstructor
@Service
public class ImageService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gms.key}")
    private String gmsKey;

    public record ImageResult(byte[] data, String mimeType) {}

    public ImageResult generateToonImage(String summary) {
        String url = "https://gms.ssafy.io/gmsapi/api.openai.com/v1/images/generations";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(gmsKey);

        String promptTemplate = """
🚫 ABSOLUTE BAN ON TEXT (HIGHEST PRIORITY) 🚫
- Under no circumstances render ANY text, letters, numbers, symbols, captions, panel titles, speech bubbles, or gibberish writing.
- Do NOT include decorative fake text, random alphabets, or UI-like labels.
- Do NOT simulate comic headers like "Introduction", "Resolution", etc.
- Do NOT draw signs, posters, documents, billboards, charts, or screens with writing.
- If the summary implies text (charts, rankings, announcements, banners, presentations), replace it ONLY with:
  → abstract shapes, colors, icons, character gestures, arrows, scenery elements.
- All storytelling must be 100% visual and symbolic. No written language in any form.

TASK:
Create a 4-panel illustration (NOT comic style with captions) based on the following news summary:

SUMMARY: REPLACE_ME

VISUAL RULES:
- Exactly 4 distinct panels arranged left-to-right or top-to-bottom in one combined image.
- Panels must flow logically: introduction → development → climax → resolution.
- Each panel must visually depict the story events from the summary without needing words.
- Include characters and backgrounds in every panel.
- Style: colorful, expressive cartoon/illustration (avoid UI/game/comic text conventions).
- Ensure clarity through visual storytelling alone.
""";

        if (summary == null) summary = "";
        String prompt = promptTemplate.replace("REPLACE_ME", summary.trim());

        Map<String, Object> body = new HashMap<>();
        body.put("model", "dall-e-3");
        body.put("prompt", prompt);
        body.put("size", "1024x1024");
        body.put("response_format", "b64_json"); // 🔑 URL 대신 base64

        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);

            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode b64Node = root.path("data").get(0).path("b64_json");

            if (b64Node.isMissingNode() || b64Node.asText().isBlank()) {
                throw new RuntimeException("DALL·E 응답에 base64 이미지 데이터가 없습니다: " + resp.getBody());
            }

            byte[] bytes = Base64.getDecoder().decode(b64Node.asText());
            return new ImageResult(bytes, "image/png");

        } catch (HttpClientErrorException e) {
            throw new RuntimeException("DALL·E API 요청 실패: " + e.getStatusCode() +
                    " body=" + e.getResponseBodyAsString(StandardCharsets.UTF_8), e);
        } catch (Exception e) {
            throw new RuntimeException("DALL·E 이미지 생성 실패", e);
        }
    }
}