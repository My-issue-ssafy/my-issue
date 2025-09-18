package com.ssafy.myissue.toons.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class ImageService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${gms.key}")
    private String gmsKey;

    public byte[] generateImage(String content) {
        String url = "https://gms.ssafy.io/gmsapi/generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp-image-generation:generateContent?key=" + gmsKey;

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", "다음 뉴스 기사를 네컷 만화 이미지로 만들어줘. 텍스트는 포함하지 말고 그림만:\n" + content))
                )),
                "generationConfig", Map.of("responseModalities", List.of("Image"))
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

        // Gemini 응답 파싱 (inlineData.base64)
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        Map<String, Object> contentMap = (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) contentMap.get("parts");

        Map<String, Object> inlineData = (Map<String, Object>) parts.get(0).get("inlineData");
        String base64Image = inlineData.get("data").toString();

        return Base64.getDecoder().decode(base64Image);
    }
}
