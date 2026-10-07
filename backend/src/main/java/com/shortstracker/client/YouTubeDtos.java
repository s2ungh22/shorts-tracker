package com.shortstracker.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.*;

public final class YouTubeDtos {
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Page<T>(List<T> items, String nextPageToken) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Item(
      String id, Snippet snippet, Statistics statistics, ContentDetails contentDetails) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Snippet(
      String channelId,
      String channelTitle,
      String title,
      String description,
      Instant publishedAt,
      Map<String, Thumbnail> thumbnails) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Thumbnail(String url) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Statistics(String viewCount, String likeCount, String commentCount) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ContentDetails(String duration) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record SearchItem(SearchId id, Snippet snippet) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record SearchId(String videoId) {}
}
