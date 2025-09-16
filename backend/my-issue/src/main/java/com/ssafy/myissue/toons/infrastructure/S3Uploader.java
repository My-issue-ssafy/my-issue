package com.ssafy.myissue.toons.infrastructure;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;

@Component
public class S3Uploader {

    private final AmazonS3 s3Client;
    private final String bucket;

    public S3Uploader(@Value("${cloud.aws.region.static}") String region,
                      @Value("${cloud.aws.s3.bucket}") String bucket) {
        this.s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(region)
                .build();
        this.bucket = bucket;
    }

    public String upload(byte[] bytes, String fileName, String contentType) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(bytes.length);

        try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
            s3Client.putObject(bucket, fileName, inputStream, metadata);
            s3Client.setObjectAcl(bucket, fileName, CannedAccessControlList.PublicRead);
        } catch (Exception e) {
            throw new RuntimeException("S3 업로드 실패", e);
        }

        URL url = s3Client.getUrl(bucket, fileName);
        return url.toString();
    }
}
