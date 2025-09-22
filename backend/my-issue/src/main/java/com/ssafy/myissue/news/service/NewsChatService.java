package com.ssafy.myissue.news.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.myissue.common.exception.CustomException;
import com.ssafy.myissue.common.exception.ErrorCode;
import com.ssafy.myissue.news.domain.News;
import com.ssafy.myissue.news.dto.NewsChatResponse;
import com.ssafy.myissue.news.infrastructure.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsChatService {

    private static final int GPT_INPUT_MAX_CHARS = 12000;
    private static final int GPT_INPUT_MIN_CHARS = 4000;

    private static final String KEY_PREFIX = "chat:news:";
    private static final int MAX_TURNS = 6; // 최대 6번 대화까지
    private static final Duration TTL = Duration.ofMinutes(30);

    private final NewsRepository newsRepository;
    private final NewsGptService newsGptService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RedisTemplate<String, Object> redisTemplate;

    public NewsChatResponse answerAboutNews(Long newsId, String question, String sid) {
        if (newsId == null || question == null || question.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_PARAMETER);
        }

        News n = newsRepository.findById(newsId)
                .orElseThrow(() -> new CustomException(ErrorCode.NEWS_NOT_FOUND));

        final String assignedSid = (sid == null || sid.isBlank())
                ? UUID.randomUUID().toString()
                : sid.trim();
        final String key = KEY_PREFIX + assignedSid + ":" + newsId;

        String title    = nz(n.getTitle());
        String outlet   = nz(n.getNewsPaper());
        String author   = nz(n.getAuthor());
        String category = nz(n.getCategory());
        String created  = format(n.getCreatedAt());

        // 기사 본문 텍스트화
        String articleText = extractPlainText(n.getContent());
        articleText = normalize(articleText);

        // 히스토리 로드
        String historyBlock = loadHistoryBlock(key);

        // 히스토리 + 기사 합쳐 프롬프트 본문 구성 후 길이 제한
        String promptBody = historyBlock.isBlank()
                ? articleText
                : "[대화 히스토리]\n" + historyBlock + "\n\n[기사 본문]\n" + articleText;
        promptBody = clip(promptBody, GPT_INPUT_MAX_CHARS);

        // GPT 호출
        String answer = newsGptService.askAbout(
                title, outlet, author, category, created, promptBody, question.trim()
        );

        // 히스토리 저장 (최근 MAX_TURNS만 유지, TTL 갱신)
        appendTurn(key, "Q: " + question.trim() + " || A: " + answer);

        // 응답에 sid 포함
        return new NewsChatResponse(answer, assignedSid);
    }

    // ---- Redis helpers ----
    private String loadHistoryBlock(String key) {
        Long lenL = redisTemplate.opsForList().size(key);
        int len = (lenL == null) ? 0 : lenL.intValue();
        if (len == 0) return "";

        List<Object> items = redisTemplate.opsForList().range(key, Math.max(0, len - MAX_TURNS), len - 1);
        if (items == null || items.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (Object it : items) {
            String row = unquote(String.valueOf(it)); // GenericJackson2Json 직렬화로 생긴 양끝 따옴표 제거
            if (!row.isBlank()) sb.append(row).append('\n');
        }
        return sb.toString().trim();
    }

    private void appendTurn(String key, String row) {
        redisTemplate.opsForList().rightPush(key, row);
        // 길이 제한
        Long sizeL = redisTemplate.opsForList().size(key);
        int size = (sizeL == null) ? 0 : sizeL.intValue();
        while (size > MAX_TURNS) {
            redisTemplate.opsForList().leftPop(key);
            size--;
        }
        // TTL 갱신
        redisTemplate.expire(key, TTL);
    }

    private String unquote(String s) {
        if (s == null || s.length() < 2) return (s == null ? "" : s);
        if (s.charAt(0) == '"' && s.charAt(s.length()-1) == '"') {
            return s.substring(1, s.length()-1);
        }
        return s;
    }

    // ---- 기존 helpers ----
    private String nz(Object o) { return (o == null) ? "" : String.valueOf(o); }

    private String format(LocalDateTime t) {
        if (t == null) return "";
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(t);
    }

    private String extractPlainText(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (!(s.startsWith("{") || s.startsWith("["))) return s;

        try {
            JsonNode root = objectMapper.readTree(s);
            StringBuilder sb = new StringBuilder();
            collectText(root, sb);
            return sb.toString();
        } catch (Exception e) {
            log.warn("[NewsChat] content JSON 파싱 실패 → 원문 사용: {}", e.getMessage());
            return raw;
        }
    }

    private void collectText(JsonNode node, StringBuilder sb) {
        if (node == null) return;
        if (node.isTextual()) {
            String t = node.asText().trim();
            if (!t.isEmpty()) sb.append(t).append('\n');
            return;
        }
        if (node.isArray()) {
            for (JsonNode c : node) collectText(c, sb);
            return;
        }
        if (node.isObject()) {
            String[] keys = {"title","subtitle","text","content","paragraph","caption","summary","body"};
            for (String k : keys) if (node.has(k)) collectText(node.get(k), sb);

            Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                String k = e.getKey().toLowerCase();
                if (k.contains("image") || k.contains("thumb") || k.contains("url")) continue;
                collectText(e.getValue(), sb);
            }
        }
    }

    private String normalize(String s) {
        return s.replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String clip(String s, int max) {
        if (s.length() <= max) return s;
        int cut = Math.max(GPT_INPUT_MIN_CHARS, max);
        return s.substring(0, cut);
    }
}
