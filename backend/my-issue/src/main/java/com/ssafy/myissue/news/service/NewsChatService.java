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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsChatService {

    private static final int GPT_INPUT_MAX_CHARS = 12000;
    private static final int GPT_INPUT_MIN_CHARS = 4000;

    private final NewsRepository newsRepository;
    private final NewsGptService newsGptService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NewsChatResponse answerAboutNews(Long newsId, String question) {
        if (newsId == null || question == null || question.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_PARAMETER);
        }

        News n = newsRepository.findById(newsId)
                .orElseThrow(() -> new CustomException(ErrorCode.NEWS_NOT_FOUND));

        String title    = nz(n.getTitle());
        String outlet   = nz(n.getNewsPaper());
        String author   = nz(n.getAuthor());
        String category = nz(n.getCategory());
        String created  = format(n.getCreatedAt());

        String articleText = extractPlainText(n.getContent());

        articleText = normalize(articleText);
        articleText = clip(articleText, GPT_INPUT_MAX_CHARS);

        String answer = newsGptService.askAbout(
                title, outlet, author, category, created, articleText, question.trim()
        );
        return new NewsChatResponse(answer);
    }

    private String nz(Object o) { return (o == null) ? "" : String.valueOf(o); }

    private String format(LocalDateTime t) {
        if (t == null) return "";
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(t);
    }

    /** content(JSONB or TEXT) → 순수 텍스트 */
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
