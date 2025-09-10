package com.ssafy.myissue.news.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Base64(JSON) 커서 인/디코더 유틸 */
public final class CursorCodec {
    private static final ObjectMapper M = new ObjectMapper();
    private CursorCodec() {}

    /** 객체 -> JSON 바이트 -> URL-세이프 Base64 문자열 */
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

