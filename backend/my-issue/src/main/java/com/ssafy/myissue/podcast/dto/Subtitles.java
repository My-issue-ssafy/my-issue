package com.ssafy.myissue.podcast.dto;

public record Subtitles(int speaker, String line, int startTime) {
    public static Subtitles of(int speaker, String line, int startTime) {
        return new Subtitles(speaker, line, startTime);
    }
}
