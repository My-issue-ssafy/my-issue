package com.ssafy.myissue.podcast.service.Impl;

import com.ssafy.myissue.common.util.AudioUtils;
import com.ssafy.myissue.news.domain.News;
import com.ssafy.myissue.news.infrastructure.NewsRepository;
import com.ssafy.myissue.podcast.domain.Podcast;
import com.ssafy.myissue.podcast.domain.PodcastNews;
import com.ssafy.myissue.podcast.domain.PodcastSubtitle;
import com.ssafy.myissue.podcast.dto.PodcastResult;
import com.ssafy.myissue.podcast.infrastructure.PodcastNewsRepository;
import com.ssafy.myissue.podcast.infrastructure.PodcastRepository;
import com.ssafy.myissue.podcast.infrastructure.PodcastSubtitleRepository;
import com.ssafy.myissue.podcast.service.PodcastService;
import com.ssafy.myissue.toons.domain.Toons;
import com.ssafy.myissue.toons.infrastructure.ToonsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PodcastServiceImpl implements PodcastService {

    private final NewsRepository newsRepository;
    private final ToonsRepository toonsRepository;
    private final PodcastNewsRepository podcastNewsRepository;
    private final PodcastRepository podcastRepository;
    private final PodcastSubtitleRepository podcastSubtitleRepository;

    private final GptService gptService;
    private final TtsService ttsService;
    private final S3Service s3Service;

    @Transactional
    @Override
    public void generateDailyPodcast() {

        // 어제 날짜의 네컷뉴스 10개 가져오기
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<Toons> topNews = toonsRepository.findByDate(yesterday);

        if(topNews.isEmpty()) {
            log.warn("어제 날짜의 HOT 뉴스가 없습니다. 팟캐스트 생성을 건너뜁니다.");
            return;
        }
        if(topNews.size() > 10) topNews = topNews.subList(0, 10);

        // 대본 생성
        List<List<String>> generatedScript = generateScript(topNews);
        // 키워드 뽑기
        List<String> keywords = generateKeywords(generatedScript);
        // Python TTS 호출
        PodcastResult podcastResult = callTtsService(generatedScript);
        // S3 업로드
        String url = s3Service.uploadPodcast(podcastResult.finalPodcast(), "podcast_" + yesterday + ".wav");
        log.debug("✅ 팟캐스트 업로드 완료: {}", url);
        // DB 저장
        savePodcast(generatedScript, topNews, keywords, url, podcastResult.accumulatedTimes());
    }

    // db 저장
    private void savePodcast(List<List<String>> scripts, List<Toons> topNews, List<String> keywords, String podcastUrl, double[] accumulatedTimes) {
        Podcast podcast = Podcast.of(podcastUrl, LocalDate.now().minusDays(1), String.join(",", keywords));
        podcastRepository.save(podcast);

        for(Toons toons: topNews) {
            News news = newsRepository.findById(toons.getNewsId()).orElseThrow();
            podcastNewsRepository.save(PodcastNews.of(podcast, news));
        }

        for(int i = 0; i < scripts.size(); i++) {
            List<String> line = scripts.get(i);
            String speaker = line.get(0);
            String text = line.get(1);
            double startTime = accumulatedTimes[i];

            podcastSubtitleRepository.save(PodcastSubtitle.of(podcast, Integer.parseInt(speaker), text, startTime));
        }
    }

    // python TTS 호출
    private PodcastResult callTtsService(List<List<String>> scripts) {
        // TTS 길이 측정 및 누적
        List<byte[]> wavParts = new ArrayList<>();
        double[] accumulatedTimes = new double[scripts.size()]; // 각 파트의 누적 시간 저장 배열
        double accumulatedSeconds = 0.0;

        for(int i = 0; i < scripts.size(); i++) {
            List<String> line = scripts.get(i);
            String speaker = "voice" + line.get(0);
            String text = line.get(1);

            log.debug("{} - 발화자: {}, 대본: {}", i+1, speaker, text);

            // TTS 서비스 호출
            byte[] wavBytes = ttsService.convertTextToSpeech(text, speaker, "podcast_" + LocalDate.now().minusDays(1) + "_part" + (i + 1) + ".wav");
            double duration = AudioUtils.getWavDurationinSeconds(wavBytes);
            log.debug("생성된 음성 파일 - {} 길이: {}초", i+1, duration);

            accumulatedTimes[i] = accumulatedSeconds;
            accumulatedSeconds += duration;

            wavParts.add(wavBytes);
        }
        byte[] finalPodcast = AudioUtils.mergeWavFiles(wavParts);

        return PodcastResult.of(finalPodcast, accumulatedTimes);
    }

    // 키워드 생성
    private List<String> generateKeywords(List<List<String>> scripts) {
        String fullScript = scripts.stream()
                .map(line -> line.get(1))
                .collect(Collectors.joining(" "));

        log.debug("대본 전체: \n{}", fullScript);
        return gptService.extractKeywords(fullScript);
    }

    // 대본 생성
    private List<List<String>> generateScript(List<Toons> topNews) {
        StringBuilder prompt = new StringBuilder();
        for(int i = 0; i < topNews.size(); i++) {
            Toons news = topNews.get(i);

            log.debug("뉴스 제목: {}", news.getTitle());

            prompt.append(i + 1).append(") 제목: ")
                    .append(news.getTitle()).append("\n")
                    .append("   요약: ").append(news.getSummary()).append("\n\n");
        }
        log.debug("생성된 프롬프트: \n{}", prompt.toString());

        return gptService.generateScript(prompt.toString());
    }
}
