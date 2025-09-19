package com.ssafy.myissue.podcast.dto;

public record PodcastResult(byte[] finalPodcast, double[] accumulatedTimes) {
    public static PodcastResult of(byte[] finalPodcast, double[] accumulatedTimes) {
        return new PodcastResult(finalPodcast, accumulatedTimes);
    }
}
