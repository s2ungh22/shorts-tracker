package com.shortstracker.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.shortstracker.client.YouTubeClient;
import com.shortstracker.client.YouTubeDtos.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class SearchServiceTest {
  private final YouTubeClient youtube = mock(YouTubeClient.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-10-07T03:00:00Z"), ZoneOffset.UTC);

  @Test
  void todayStartsAtKoreanMidnightAndMapsResults() {
    Instant after = Instant.parse("2026-10-06T15:00:00Z");
    when(youtube.searchKeyword("연예인 추천", after, 25))
        .thenReturn(
            List.of(
                new SearchItem(
                    new SearchId("UmGZGaMMy9c"),
                    new Snippet(
                        "channel",
                        "채널",
                        "제목",
                        "설명",
                        after,
                        Map.of("high", new Thumbnail("https://example.com/image.jpg"))))));
    var result = new SearchService(youtube, clock).search(" 연예인 추천 ", 1, 25);
    assertEquals(after, result.publishedAfter());
    assertEquals("제목", result.items().getFirst().title());
    assertEquals(
        "https://www.youtube.com/watch?v=UmGZGaMMy9c", result.items().getFirst().videoUrl());
    verify(youtube).searchKeyword("연예인 추천", after, 25);
  }

  @Test
  void sevenDaysIncludesTodayAndSixPreviousDates() {
    Instant after = Instant.parse("2026-09-30T15:00:00Z");
    when(youtube.searchKeyword("test", after, 50)).thenReturn(List.of());
    var result = new SearchService(youtube, clock).search("test", 7, 50);
    assertEquals(after, result.publishedAfter());
    assertTrue(result.items().isEmpty());
  }
}
