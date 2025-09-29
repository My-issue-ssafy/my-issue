package com.ssafy.myissue.news.dto;

/** 스크랩 토글 결과 */
public record ScrapToggleResponse(boolean scrapped, Long scrapId) {} // true면 스크랩됨, false면 해제됨. 새로 저장되면 id 반환
// 프론트가 버튼 상태/메시지/추가 액션 판단에 쓰면 됨