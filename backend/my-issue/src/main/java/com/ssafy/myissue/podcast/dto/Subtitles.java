package com.ssafy.myissue.podcast.dto;

public record Subtitles(int speaker, String line, long startTime) {
    public static Subtitles of(int speaker, String line, long startTime) {
        return new Subtitles(speaker, line, startTime);
    }
}
