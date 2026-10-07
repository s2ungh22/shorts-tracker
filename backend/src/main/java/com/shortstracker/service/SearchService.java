package com.shortstracker.service;

import com.shortstracker.client.YouTubeClient;
import java.time.*;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SearchService {
  private static final ZoneId SEARCH_ZONE = ZoneId.of("Asia/Seoul");
  private final YouTubeClient youtube;
  private final Clock clock;

  @org.springframework.beans.factory.annotation.Autowired
  public SearchService(YouTubeClient youtube) {
    this(youtube, Clock.systemUTC());
  }

  SearchService(YouTubeClient youtube, Clock clock) {
    this.youtube = youtube;
    this.clock = clock;
  }

  public record SearchResult(
      String videoId,
      String title,
      String channelName,
      Instant publishedAt,
      String thumbnailUrl,
      String videoUrl) {}

  public record SearchResponse(
      String query, Instant publishedAfter, int days, int maxResults, List<SearchResult> items) {}

  public SearchResponse search(String query, int days, int maxResults) {
    // 1 day includes today from midnight in Korea; 7 days includes today and six prior dates.
    Instant after =
        LocalDate.now(clock.withZone(SEARCH_ZONE))
            .minusDays(days - 1L)
            .atStartOfDay(SEARCH_ZONE)
            .toInstant();
    var items =
        youtube.searchKeyword(query.trim(), after, maxResults).stream()
            .filter(x -> x.id() != null && x.id().videoId() != null && x.snippet() != null)
            .map(
                x -> {
                  var snippet = x.snippet();
                  var thumbnails = snippet.thumbnails();
                  String thumbnail =
                      thumbnails == null || thumbnails.isEmpty()
                          ? null
                          : thumbnails
                              .getOrDefault("high", thumbnails.values().iterator().next())
                              .url();
                  return new SearchResult(
                      x.id().videoId(),
                      snippet.title(),
                      snippet.channelTitle(),
                      snippet.publishedAt(),
                      thumbnail,
                      "https://www.youtube.com/watch?v=" + x.id().videoId());
                })
            .toList();
    return new SearchResponse(query.trim(), after, days, maxResults, items);
  }
}
