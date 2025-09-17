package com.ssafy.myissue.toons.service;

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
    public void generateDailyToons() {
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            LocalDateTime start = yesterday.atStartOfDay();
            LocalDateTime end = yesterday.atTime(LocalTime.MAX);

            List<News> topNews = newsRepository.findTop10ByDate(start, end, PageRequest.of(0, 10));

            for (News news : topNews) {
                String summary = gptService.summarize(news.getContent());
                byte[] image = imageService.generateImage(news.getContent());
                String imageUrl = s3Uploader.upload(image, "toons/" + news.getNewsId() + ".png", "image/png");

                Toons toon = Toons.builder()
                        .newsId(news.getNewsId())
                        .title(news.getTitle())
                        .summary(summary)
                        .toonImage(imageUrl)
                        .build();

                toonsRepository.save(toon);
            }
        } catch (Exception e) {
            e.printStackTrace(); // 콘솔에 원인 출력
            throw e; // 그대로 다시 던져서 Swagger는 500 리턴
        }
    }
}
