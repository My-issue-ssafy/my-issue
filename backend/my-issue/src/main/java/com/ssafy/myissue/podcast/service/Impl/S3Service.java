package com.ssafy.myissue.podcast.service.Impl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3 amazonS3;

    @Value("venture-review")
    private String bucket;

    public String uploadPodcast(byte[] fileBytes, String key) {
        if (fileBytes == null || fileBytes.length == 0) {
            log.debug("팟캐스트 생성 - 저장할 파일이 비어있습니다.");
            throw new IllegalArgumentException("업로드할 파일 데이터가 비어 있습니다.");
        }

        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(fileBytes.length);
            metadata.setContentType("audio/wav");

            PutObjectRequest request = new PutObjectRequest(
                    bucket,
                    key,
                    new ByteArrayInputStream(fileBytes),
                    metadata
            ).withCannedAcl(CannedAccessControlList.PublicRead);

            amazonS3.putObject(request);

            return amazonS3.getUrl(bucket, key).toString();
        } catch (Exception e) {
            log.error("팟캐스트 생성 - S3 업로드 실패", e);
            throw new RuntimeException("S3 업로드 실패", e);
        }
    }
}
