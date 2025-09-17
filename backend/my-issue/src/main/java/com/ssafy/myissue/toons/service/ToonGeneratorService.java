package com.ssafy.myissue.toons.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ssafy.myissue.news.domain.News;
import com.ssafy.myissue.news.infrastructure.NewsRepository;
import com.ssafy.myissue.toons.domain.Toons;
import com.ssafy.myissue.toons.infrastructure.ToonsRepository;
import com.ssafy.myissue.toons.infrastructure.S3Uploader;
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public void generateDailyToons() throws JsonProcessingException {
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            LocalDateTime start = yesterday.atStartOfDay();
            LocalDateTime end = yesterday.atTime(LocalTime.MAX);

            // 어제 기준 TOP 10 뉴스 가져오기
            List<News> topNews = newsRepository.findTop10ByDate(start, end, PageRequest.of(0, 10));

            for (News news : topNews) {
                // 1. GPT 요약본 생성
                String summary = gptService.summarize(news.getContent());

                // 2. 요약본만 포함한 Toons 저장 (이미지는 null)
                Toons toon = Toons.builder()
                        .newsId(news.getId())
                        .title(news.getTitle())
                        .summary(summary)
                        .toonImage(null)   // ✅ 이미지 나중에 업데이트
                        .build();

                toonsRepository.save(toon);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e; // Swagger에서 500 에러 확인 가능
        }
    }
}
