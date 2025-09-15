package com.ssafy.myissue.notification.dto;

import lombok.Builder;

import java.util.List;

public record SliceResponseDto<T>(List<T> content, boolean hasNext) {
}
