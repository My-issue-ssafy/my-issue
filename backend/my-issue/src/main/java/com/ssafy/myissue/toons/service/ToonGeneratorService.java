package com.ssafy.myissue.toons.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ssafy.myissue.news.domain.News;
import com.ssafy.myissue.news.infrastructure.NewsRepository;
import com.ssafy.myissue.toons.domain.Toons;
import com.ssafy.myissue.toons.infrastructure.ToonsRepository;
import com.ssafy.myissue.toons.infrastructure.S3Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;            // [ADDED]
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ToonGeneratorService {

    private final NewsRepository newsRepository;
    private final ToonsRepository toonsRepository;
    private final GptService gptService;
    private final ImageService imageService;
    private final S3Uploader s3Uploader;

    @Value("${openai.enabled:true}")                             // [ADDED] (있으면 사용)
    private boolean openaiEnabled;

    @Transactional
    public void generateDailyToons() throws JsonProcessingException {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime start = yesterday.atStartOfDay();
        LocalDateTime end = yesterday.atTime(LocalTime.MAX);

        List<News> topNews = newsRepository.findTop10ByDate(start, end, PageRequest.of(0, 10));

        for (News news : topNews) {
            String summary = gptService.summarize(news.getContent());

            Toons toon = Toons.builder()
                    .newsId(news.getId())
                    .title(news.getTitle())
                    .summary(summary)
                    .toonImage(null)
                    .date(news.getCreatedAt().toLocalDate())
                    .build();

            toonsRepository.saveAndFlush(toon); // ID 확정

            if (!openaiEnabled) {                                        // [ADDED]
                System.err.println("[ToonGeneratorService] OpenAI disabled. Skip image.");
                continue;
            }

            try {
                ImageService.ImageResult imageResult = imageService.generateToonImage(summary);
                String extension = imageResult.mimeType().equals("image/jpeg") ? ".jpg" : ".png";
                String fileName = "toons/" + toon.getNewsId() + extension;

                String imageUrl = s3Uploader.upload(imageResult.data(), fileName, imageResult.mimeType());
                toon.setToonImage(imageUrl);

            } catch (RuntimeException ex) {
                String msg = ex.getMessage() == null ? "" : ex.getMessage();

                if (msg.contains("BILLING_HARD_LIMIT_REACHED")) {
                    System.err.println("[ToonGeneratorService] 하드 리밋 도달. 이미지 생성 중단: " + msg);
                    break;                                                // [UNCHANGED]
                }
                if (msg.contains("ORG_NOT_VERIFIED")) {                   // [ADDED]
                    System.err.println("[ToonGeneratorService] 조직 미인증. 이미지 생성 중단: " + msg);
                    break;                                                // [ADDED]
                }

                // 그 외는 해당 건만 스킵
                System.err.println("[ToonGeneratorService] 이미지 생성 실패(스킵): " + msg);
            }
        }
    }
}
