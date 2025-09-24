package com.ssafy.myissue.news.dto;

import co.elastic.clients.elasticsearch._types.FieldValue;
import java.util.List;

public record SearchAfterCursor(
    Double score,     // 검색 스코어 (_score)
    Long createdAtSec, // 생성일 (초 단위)
    Long newsId       // 뉴스 ID (tie-breaker)
) {
    public List<FieldValue> toSearchAfterValues() {
        return List.of(
            FieldValue.of(score),
            FieldValue.of(createdAtSec),
            FieldValue.of(newsId)
        );
    }
    
    public List<FieldValue> toSearchAfterValuesWithoutScore() {
        return List.of(
            FieldValue.of(createdAtSec),
            FieldValue.of(newsId)
        );
    }
    
    public static SearchAfterCursor from(double score, long createdAtSec, long newsId) {
        return new SearchAfterCursor(score, createdAtSec, newsId);
    }
}