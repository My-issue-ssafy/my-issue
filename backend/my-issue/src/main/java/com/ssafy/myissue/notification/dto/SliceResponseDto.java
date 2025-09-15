package com.ssafy.myissue.notification.dto;

import java.util.List;

public record SliceResponseDto<T>(List<T> content, boolean hasNext) {
    public static <T> SliceResponseDto<T> of(List<T> content, boolean hasNext) {
        return new SliceResponseDto<>(content, hasNext);
    }
}
