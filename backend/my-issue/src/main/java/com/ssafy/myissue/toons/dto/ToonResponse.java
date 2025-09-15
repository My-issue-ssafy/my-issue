package com.ssafy.myissue.toons.dto;

import com.ssafy.myissue.toons.domain.Toons;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToonResponse {

    private Long toonId;
    private Long newsId;
    private String toonImage;
    private String summary;
    private String title;

    public static ToonResponse from(Toons toon) {
        return ToonResponse.builder()
                .toonId(toon.getToonId())
                .newsId(toon.getNewsId())
                .toonImage(toon.getToonImage())
                .summary(toon.getSummary())
                .title(toon.getTitle())
                .build();
    }
}
