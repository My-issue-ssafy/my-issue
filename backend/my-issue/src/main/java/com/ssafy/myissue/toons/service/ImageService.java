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

/**
 * 방어적으로 여러 요청 포맷을 시도하여 이미지(inline_data)를 찾아 반환하는 ImageService
 */
@Service
@RequiredArgsConstructor
public class ImageService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gms.key}")
    private String gmsKey;

    public record ImageResult(byte[] data, String mimeType) {}

    public ImageResult generateToonImage(String summary) {
        String url = "https://gms.ssafy.io/gmsapi/generativelanguage.googleapis.com/v1beta/models/"
                + "imagen-3.0-generate-002:predict?key=" + gmsKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        // 사용자가 보낸 그대로의 영어 형식 프롬프트 (요구하신 포맷)
        String promptTemplate = """
        Create a 4-panel comic.
        - Each panel must include characters and background.
        - Absolutely NO text, NO words, NO captions, NO letters, NO numbers.
        - No speech bubbles or labels.
        - Only images, no writing at all.
        - Style: colorful, simple characters, comic style, 4 separated panels in one image.
        Avoid: text, captions, subtitles, speech bubbles, letters, numbers, words.
        """;

        // Summary가 null일 수 있으니 안전하게 처리 (불필요한 길이는 자르지 않음 — 원하시면 조절)
        if (summary == null) summary = "";
        String prompt = promptTemplate + "\nSummary: " + summary.trim();

        Map<String, Object> body = Map.of(
                "instances", List.of(Map.of("prompt", prompt)),
                "parameters", Map.of("sampleCount", 1) // 한 장만 생성
        );

        try {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode prediction = root.path("predictions").get(0);

            if (prediction != null) {
                // 이미지 데이터 위치: bytesBase64Encoded 또는 imageBytes 등 API 문서에 맞춰 확인
                // 여기서는 docs 예시대로 bytesBase64Encoded 사용
                JsonNode base64Node = prediction.get("bytesBase64Encoded");
                if (base64Node != null && !base64Node.asText().isBlank()) {
                    byte[] bytes = Base64.getDecoder().decode(base64Node.asText());
                    return new ImageResult(bytes, "image/png");
                }

                // 일부 환경에서는 predictions[].data[0].b64_string 같은 구조일 수 있으므로 안전 체크
                JsonNode dataNode = prediction.path("data");
                if (dataNode.isArray() && dataNode.size() > 0) {
                    JsonNode first = dataNode.get(0);
                    JsonNode b64 = first.get("b64");
                    if (b64 == null) b64 = first.get("b64_string");
                    if (b64 != null && !b64.asText().isBlank()) {
                        byte[] bytes = Base64.getDecoder().decode(b64.asText());
                        return new ImageResult(bytes, "image/png");
                    }
                }
            }

            throw new RuntimeException("Imagen 응답에 이미지 데이터가 없습니다: " + resp.getBody());
        } catch (HttpClientErrorException e) {
            String err = e.getResponseBodyAsString(StandardCharsets.UTF_8);
            throw new RuntimeException("Imagen API 요청 실패: " + e.getStatusCode() + " body=" + err, e);
        } catch (Exception e) {
            throw new RuntimeException("Imagen 이미지 생성 실패", e);
        }
    }


//    public ImageResult generateToonImage(String summary) {
//        String url = "https://gms.ssafy.io/gmsapi/generativelanguage.googleapis.com/v1beta/models/"
//                + "imagen-3.0-generate-002:predict?key=" + gmsKey;
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
//
//        String prompt = """
//            Create a 4-panel comic.
//            - Each panel must include characters and background.
//            - Absolutely NO text, NO words, NO captions, NO letters, NO numbers.
//            - No speech bubbles or labels.
//            - Only images, no writing at all.
//            - Style: colorful, simple characters, comic style, 4 separated panels in one image.
//            Avoid: text, captions, subtitles, speech bubbles, letters, numbers, words.
//            """ + summary;
//
//        Map<String, Object> body = Map.of(
//                "instances", List.of(Map.of("prompt", prompt)),
//                "parameters", Map.of("sampleCount", 1) // 여기서 1개만 생성
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
//            if (prediction != null && prediction.has("bytesBase64Encoded")) {
//                String base64 = prediction.get("bytesBase64Encoded").asText();
//                byte[] bytes = Base64.getDecoder().decode(base64);
//                String mimeType = "image/png"; // Imagen 기본은 PNG
//                return new ImageResult(bytes, mimeType);
//            }
//
//            throw new RuntimeException("Imagen 응답에 이미지 데이터가 없음: " + resp.getBody());
//
//        } catch (Exception e) {
//            throw new RuntimeException("Imagen 이미지 생성 실패", e);
//        }
//    }


    /**
     * 요약(summary)을 바탕으로 Gemini에 이미지 생성 요청을 보내고
     * inline base64 이미지를 찾아 ImageResult로 반환한다.
     *
     * 여러 요청 포맷을 시도함(순서에 따라 프록시/모델의 수용 여부가 다름).
     */
//    public ImageResult generateToonImage(String summary) {
//        String url = "https://gms.ssafy.io/gmsapi/generativelanguage.googleapis.com/v1beta/models/"
//                + "gemini-2.0-flash-exp-image-generation:generateContent?key=" + gmsKey;
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
//
//        String prompt = """
//                Create a 4-panel comic.
//                - Each panel must include characters and background.
//                - Absolutely NO text, NO words, NO captions, NO letters, NO numbers.
//                - No speech bubbles or labels.
//                - Only images, no writing at all.
//                - Style: colorful, simple characters, comic style, 4 separated panels in one image.
//                Avoid: text, captions, subtitles, speech bubbles, letters, numbers, words.
//                """ + summary;
//
//        // 시도할 바디 케이스들 (방어적)
//        List<Map<String, Object>> candidateBodies = List.of(
//                // 1) responseModalities: ["Image","Text"]
//                Map.of(
//                        "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
//                        "generationConfig", Map.of("responseModalities", List.of("Text", "Image"))
//                ),
//                // 2) responseModalities: ["Text","Image"]
//                Map.of(
//                        "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
//                        "generationConfig", Map.of("responseModalities", List.of("Text", "Image"))
//                ),
//                // 3) no generationConfig (image-only endpoint may auto-respond with image)
//                Map.of(
//                        "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
//                )
//        );
//
//        List<String> attemptLogs = new ArrayList<>();
//
//        for (int attempt = 0; attempt < candidateBodies.size(); attempt++) {
//            Map<String, Object> body = candidateBodies.get(attempt);
//            String attemptLabel = "attempt#" + (attempt + 1) + " - " + bodySummary(body);
//            try {
//                ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST,
//                        new HttpEntity<>(body, headers), String.class);
//
//                String raw = resp.getBody();
////                System.out.println("Gemini Raw Response (" + attemptLabel + "): " + raw);
//
//                // parse & try to extract image part
//                JsonNode root = objectMapper.readTree(raw);
//                JsonNode imagePart = findFirstImagePart(root);
//                if (imagePart != null) {
//                    JsonNode inlineData = imagePart.has("inline_data") ? imagePart.get("inline_data")
//                            : imagePart.get("inlineData");
//
//                    String mimeType = inlineData.path("mime_type").asText("image/png"); // fallback png
//                    String base64 = inlineData.path("data").asText();
//
//                    if (base64 == null || base64.isBlank()) {
//                        attemptLogs.add(attemptLabel + " => inline_data.data is empty");
//                        // try next attempt
//                    } else {
//                        byte[] bytes = Base64.getDecoder().decode(base64);
//                        if (bytes.length < 128) { // 임계값: 128 bytes (너무 작으면 이미지 아님)
//                            attemptLogs.add(attemptLabel + " => decoded image too small: " + bytes.length);
//                        } else {
//                            // 성공!
//                            return new ImageResult(bytes, mimeType);
//                        }
//                    }
//                } else {
//                    // 이미지 파트 없음 -> 텍스트 파트 수집해서 로그에 남김
//                    List<String> texts = collectTextParts(root);
//                    attemptLogs.add(attemptLabel + " => text-only response: " + texts);
//                }
//
//            } catch (HttpClientErrorException e) {
//                String errBody = e.getResponseBodyAsString(StandardCharsets.UTF_8);
//                String msg = attemptLabel + " => HTTP error: " + e.getStatusCode() + " body: " + errBody;
//                attemptLogs.add(msg);
//                System.out.println(msg);
//                // 계속 다음 시도
//            } catch (Exception e) {
//                String msg = attemptLabel + " => parse/other error: " + e.getMessage();
//                attemptLogs.add(msg);
//                System.out.println(msg);
//                // 계속 다음 시도
//            }
//        }
//
//        // 모든 시도 실패: 상세 로그 포함해서 예외 던짐
//        String joined = String.join(" | ", attemptLogs);
//        throw new RuntimeException("모든 Gemini 이미지 생성 시도 실패. 시도 로그: " + joined);
//    }
//
//    /**
//     * candidates[*].content.parts[*] 순회하며 이미지 파트(inline_data/inlineData) 찾기
//     */
//    private JsonNode findFirstImagePart(JsonNode root) {
//        JsonNode candidates = root.path("candidates");
//        if (!candidates.isArray()) return null;
//        for (JsonNode cand : candidates) {
//            JsonNode parts = cand.path("content").path("parts");
//            if (!parts.isArray()) continue;
//            for (JsonNode part : parts) {
//                if (part.has("inline_data") || part.has("inlineData")) {
//                    JsonNode dataNode = part.has("inline_data") ? part.get("inline_data") : part.get("inlineData");
//                    if (dataNode != null && dataNode.hasNonNull("data") && !dataNode.path("data").asText().isBlank()) {
//                        return part;
//                    }
//                }
//            }
//        }
//        return null;
//    }
//
//    /**
//     * text 파트들을 모아서 디버깅용 리스트 반환
//     */
//    private List<String> collectTextParts(JsonNode root) {
//        List<String> texts = new ArrayList<>();
//        JsonNode candidates = root.path("candidates");
//        if (!candidates.isArray()) return texts;
//        for (JsonNode cand : candidates) {
//            JsonNode parts = cand.path("content").path("parts");
//            if (!parts.isArray()) continue;
//            for (JsonNode p : parts) {
//                if (p.has("text")) texts.add(p.get("text").asText());
//            }
//        }
//        return texts;
//    }
//
//    /**
//     * 바디 내용을 짧게 요약해서 로그문구로 사용 (민감정보 제외)
//     */
//    private String bodySummary(Map<String, Object> body) {
//        if (body.containsKey("generationConfig")) {
//            Object gen = body.get("generationConfig");
//            return "generationConfig=" + gen.toString();
//        }
//        return "no-generationConfig";
//    }
}
