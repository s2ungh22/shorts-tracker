package com.shortstracker.client;

import com.shortstracker.client.YouTubeDtos.*;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class YouTubeClient {
  private final RestClient client;
  private final String key;

  public YouTubeClient(@Value("${youtube.api-key:}") String key) {
    this.key = key;
    var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(10000);
    factory.setReadTimeout(20000);
    client =
        RestClient.builder()
            .baseUrl("https://www.googleapis.com/youtube/v3")
            .requestFactory(factory)
            .build();
  }

  public boolean configured() {
    return !key.isBlank();
  }

  private void check() {
    if (!configured())
      throw new IllegalStateException("Backend YOUTUBE_API_KEY is required for collection");
  }

  public List<Item> videos(List<String> ids) {
    check();
    if (ids.size() > 50) throw new IllegalArgumentException("Maximum 50 video IDs per request");
    try {
      var page =
          client
              .get()
              .uri(
                  b ->
                      b.path("/videos")
                          .queryParam("part", "snippet,statistics,contentDetails")
                          .queryParam("id", String.join(",", ids))
                          .queryParam("key", key)
                          .build())
              .retrieve()
              .body(new ParameterizedTypeReference<Page<Item>>() {});
      return page == null || page.items() == null ? List.of() : page.items();
    } catch (Exception e) {
      throw new YouTubeUnavailableException();
    }
  }

  public List<String> search(String channel, Instant after) {
    check();
    try {
      var page =
          client
              .get()
              .uri(
                  b -> {
                    b.path("/search")
                        .queryParam("part", "snippet")
                        .queryParam("channelId", channel)
                        .queryParam("order", "date")
                        .queryParam("type", "video")
                        .queryParam("videoDuration", "short")
                        .queryParam("maxResults", 50)
                        .queryParam("key", key);
                    if (after != null) b.queryParam("publishedAfter", after.toString());
                    return b.build();
                  })
              .retrieve()
              .body(new ParameterizedTypeReference<Page<SearchItem>>() {});
      return page == null || page.items() == null
          ? List.of()
          : page.items().stream().map(x -> x.id().videoId()).toList();
    } catch (Exception e) {
      throw new YouTubeUnavailableException();
    }
  }

  public List<SearchItem> searchKeyword(String query, Instant publishedAfter, int maxResults) {
    check();
    try {
      var page =
          client
              .get()
              .uri(
                  b ->
                      b.path("/search")
                          .queryParam("part", "snippet")
                          .queryParam("type", "video")
                          .queryParam("q", "{query}")
                          .queryParam("order", "date")
                          .queryParam("publishedAfter", publishedAfter.toString())
                          .queryParam("maxResults", maxResults)
                          .queryParam("regionCode", "KR")
                          .queryParam("relevanceLanguage", "ko")
                          .queryParam("key", "{key}")
                          .build(query, key))
              .retrieve()
              .body(new ParameterizedTypeReference<Page<SearchItem>>() {});
      return page == null || page.items() == null ? List.of() : page.items();
    } catch (Exception e) {
      throw new YouTubeUnavailableException();
    }
  }

  public static class YouTubeUnavailableException extends RuntimeException {
    public YouTubeUnavailableException() {
      super("YouTube request failed; check API key, quota and network access");
    }
  }
}
