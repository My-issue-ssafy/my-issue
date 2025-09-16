package com.ssafy.myissue.podcast.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/podcast")
@Tag(name = "Podcast", description = "팟캐스트(Podcast) API - 시은")
public class PodcastController {
}
