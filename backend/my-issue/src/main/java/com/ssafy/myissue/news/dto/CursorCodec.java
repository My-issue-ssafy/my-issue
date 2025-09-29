package com.ssafy.myissue.news.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Base64(JSON) 커서 인/디코더 유틸 */
public final class CursorCodec {
    private static final ObjectMapper M = new ObjectMapper();
    private CursorCodec() {}

    /** 객체 -> JSON 바이트 -> URL-세이프 Base64 문자열
     * 쿼리 스트링에 넣기 좋게 +/= 제거와 대체. 패딩 제거함*/
    public static String encode(Object obj) {
        try {
            byte[] json = M.writeValueAsBytes(obj);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("cursor encode error", e);
        }
    }

    /** URL-세이프 Base64 문자열 -> JSON 바이트 -> 객체 */
    public static <T> T decode(String cursor, Class<T> type) {
        try {
            byte[] json = Base64.getUrlDecoder().decode(cursor.getBytes(StandardCharsets.UTF_8));
            return M.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalArgumentException("cursor decode error", e);
        }
    }
}

// 이렇게 진행함으로써 프론트는 문자열만 주고받으면 됨. 서버는 필요할 때 디코딩하여 경계값 꺼내 쿼리에 사용하면 됨.

