package com.shortstracker.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class YouTubeKeywordSearchTest {
  @Test
  void sendsKoreanQueryAndRequestedSearchParameters() {
    var builder = RestClient.builder().baseUrl("https://www.googleapis.com/youtube/v3");
    var server = MockRestServiceServer.bindTo(builder).build();
    var youtube = new YouTubeClient("test-only-key");
    ReflectionTestUtils.setField(youtube, "client", builder.build());
    server
        .expect(
            request -> {
              assertEquals("/youtube/v3/search", request.getURI().getPath());
              String query =
                  URLDecoder.decode(request.getURI().getRawQuery(), StandardCharsets.UTF_8);
              for (String parameter :
                  java.util.List.of(
                      "part=snippet",
                      "type=video",
                      "q=연예인 추천 & 음악",
                      "order=date",
                      "maxResults=25",
                      "regionCode=KR",
                      "relevanceLanguage=ko",
                      "publishedAfter=2026-10-06T15:00:00Z",
                      "key=test-only-key")) {
                assertTrue(query.contains(parameter), parameter);
              }
              assertTrue(request.getURI().getRawQuery().contains("%26"));
            })
        .andRespond(
            withSuccess(
                """
{"items":[{"id":{"videoId":"UmGZGaMMy9c"},"snippet":{"title":"테스트 영상","channelTitle":"채널","publishedAt":"2026-10-07T01:00:00Z"}}]}
""",
                MediaType.APPLICATION_JSON));
    var result = youtube.searchKeyword("연예인 추천 & 음악", Instant.parse("2026-10-06T15:00:00Z"), 25);
    assertEquals("테스트 영상", result.getFirst().snippet().title());
    server.verify();
  }

  @Test
  void upstreamFailureDoesNotExposeRequestOrKey() {
    var builder = RestClient.builder().baseUrl("https://www.googleapis.com/youtube/v3");
    var server = MockRestServiceServer.bindTo(builder).build();
    var youtube = new YouTubeClient("test-only-key");
    ReflectionTestUtils.setField(youtube, "client", builder.build());
    server.expect(request -> {}).andRespond(withServerError());
    var error =
        assertThrows(
            YouTubeClient.YouTubeUnavailableException.class,
            () -> youtube.searchKeyword("test", Instant.parse("2026-10-06T15:00:00Z"), 1));
    assertFalse(error.getMessage().contains("test-only-key"));
    assertNull(error.getCause());
    server.verify();
  }
}
