package com.ssafy.myissue.notification.service.impl;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class RecommendationService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final String pythonApiUrl = "http://j13d101.p.ssafy.io/fastapi/api/recommendations";

    public List<Long> getRecommendations(Long userId, int cfCount, int cbfCount, String strategy) {
        String url = String.format("%s/%d?cf_count=%d&cbf_count=%d&strategy=%s",
            pythonApiUrl, userId, cfCount, cbfCount, strategy);

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object recs = response.getBody().get("recommendations");

                if (recs instanceof List<?> list && !list.isEmpty()) {
                    return list.stream()
                        .map(obj -> (Map<String, Object>) obj) // 하나하나 Map임
                        .map(m -> ((Number) m.get("news_id")).longValue())
                        .toList();
                }
            }
        } catch (Exception e) {
            System.err.println("Python API call failed for userId=" + userId + ": " + e.getMessage());
        }

        return Collections.emptyList();
    }
}
