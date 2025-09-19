package com.ssafy.myissue.podcast.service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class TtsService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String ttsUrl = "http://j13d101.p.ssafy.io/fastapi/api/tts/synthesize";

    public byte[] convertTextToSpeech(String text, String voice, String fileName) {
        Map<String, String> request = new HashMap<>();

        request.put("text", text);
        request.put("voice", voice);
        request.put("language", "ko");
        request.put("filename", fileName);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
        ResponseEntity<byte[]> response = restTemplate.postForEntity(ttsUrl, entity, byte[].class);

        if(response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            log.error("TTS 변환 실패: " + response.getStatusCode());
            throw new RuntimeException("TTS 요청 실패: " + response.getStatusCode());
        }
    }
}
