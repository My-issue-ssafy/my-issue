package com.ssafy.myissue.news.dto;

// [NEW] content(jsonb)의 각 블록 표현
// type: "text" | "image" | "img_desc"
// content: 본문 텍스트 또는 이미지 URL
public record ContentBlock(String type, String content) {}
