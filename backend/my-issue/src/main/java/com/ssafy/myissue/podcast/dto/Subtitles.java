package com.ssafy.myissue.podcast.dto;

public record Subtitles(int speaker, String line, double startTime) {
    public static Subtitles of(int speaker, String line, double startTime) {
        return new Subtitles(speaker, line, startTime);
    }
}
