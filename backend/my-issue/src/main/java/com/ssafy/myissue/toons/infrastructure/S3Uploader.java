package com.ssafy.myissue.toons.infrastructure;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;

@Component
@RequiredArgsConstructor
public class S3Uploader {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    /**
     * byte[] 파일을 S3에 업로드 후 URL 반환
     */
    public String upload(byte[] fileBytes, String fileName, String mimeType) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException("업로드할 파일 데이터가 비어 있습니다.");
        }

        try {
            // 파일 확장자 자동 보완 (예: fileName="toon_001" → toon_001.png)
            String finalFileName = ensureExtension(fileName, mimeType);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(fileBytes.length);
            metadata.setContentType(mimeType); // ✅ mimeType 반영 (image/png, image/jpeg 등)

            amazonS3.putObject(new PutObjectRequest(bucket, finalFileName,
                    new ByteArrayInputStream(fileBytes), metadata)
                    .withCannedAcl(CannedAccessControlList.PublicRead));

            return amazonS3.getUrl(bucket, finalFileName).toString();
        } catch (Exception e) {
            throw new RuntimeException("S3 업로드 실패", e);
        }
    }

    /**
     * mimeType에 따라 확장자 자동 부여
     */
    private String ensureExtension(String fileName, String mimeType) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return fileName;
        }

        if ("image/jpeg".equalsIgnoreCase(mimeType)) {
            return fileName + ".jpg";
        } else if ("image/png".equalsIgnoreCase(mimeType)) {
            return fileName + ".png";
        } else {
            // 기본 PNG
            return fileName + ".png";
        }
    }
}
