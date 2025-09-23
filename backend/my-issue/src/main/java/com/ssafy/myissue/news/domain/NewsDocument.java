package com.ssafy.myissue.news.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = "news")
public class NewsDocument {

    @Id
    @Field(type = FieldType.Long)
    private Long id;

    @Field(type = FieldType.Text, analyzer = "nori") // 한국어 검색
    private String title;

    @Field(type = FieldType.Text, analyzer = "nori")
    private String content;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Keyword)
    private String author;

    @Field(type = FieldType.Keyword)
    private String newsPaper;
}
