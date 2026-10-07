package com.shortstracker.dto;

import com.shortstracker.domain.GrowthStatus;
import jakarta.validation.constraints.*;
import java.time.Instant;

public final class ApiDtos {
  public record ChannelInput(
      @NotBlank @Pattern(regexp = "UC[A-Za-z0-9_-]{22}") String youtubeChannelId,
      @NotBlank @Size(max = 200) String channelName,
      boolean isMyChannel) {}

  public record ChannelDto(
      Long id,
      String youtubeChannelId,
      String channelName,
      boolean isMyChannel,
      Instant createdAt) {}

  public record TrackInput(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{11}") String videoId) {}

  public record Metrics(
      double averageVph,
      double recent10mVph,
      double recent30mVph,
      double recent1hVph,
      double previous1hVph,
      double accelerationRate,
      GrowthStatus status) {}

  public record VideoDto(
      String videoId,
      String title,
      String channelName,
      boolean isMyChannel,
      long views,
      long likes,
      long comments,
      double averageVph,
      double recent10mVph,
      double recent30mVph,
      double recent1hVph,
      double previous1hVph,
      double accelerationRate,
      GrowthStatus status,
      String thumbnailUrl,
      String shortsUrl,
      Instant publishedAt,
      Instant collectedAt) {}

  public record SnapshotDto(
      long views, long likes, long comments, Instant collectedAt, double vph) {}
}
