package com.ssafy.myissue.news.dto;

/** HOT(views desc, createdAt desc, newsId desc) 커서 키 */
public record HotCursor(int views, long createdAtSec, long newsId) {}
// views DESC, createdAt DESC, newsId DESC. 키셋 WHERE 비교도 같은 순서여야 누락/중복이 없음