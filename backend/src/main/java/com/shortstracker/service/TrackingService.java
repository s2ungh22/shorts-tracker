package com.shortstracker.service;

import com.shortstracker.client.*;
import com.shortstracker.domain.*;
import com.shortstracker.dto.ApiDtos.*;
import com.shortstracker.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class TrackingService {
  private final ChannelRepository channels;
  private final VideoRepository videos;
  private final VideoSnapshotRepository snapshots;
  private final YouTubeClient youtube;
  private final AnalyticsService analytics;

  public TrackingService(
      ChannelRepository c,
      VideoRepository v,
      VideoSnapshotRepository s,
      YouTubeClient y,
      AnalyticsService a) {
    channels = c;
    videos = v;
    snapshots = s;
    youtube = y;
    analytics = a;
  }

  public ChannelDto channelDto(Channel c) {
    return new ChannelDto(c.id, c.youtubeChannelId, c.channelName, c.isMyChannel, c.createdAt);
  }

  public List<ChannelDto> channels() {
    return channels.findAll().stream().map(this::channelDto).toList();
  }

  public ChannelDto addChannel(ChannelInput in) {
    var c = channels.findByYoutubeChannelId(in.youtubeChannelId()).orElseGet(Channel::new);
    c.youtubeChannelId = in.youtubeChannelId();
    c.channelName = in.channelName();
    c.isMyChannel = in.isMyChannel();
    return channelDto(channels.save(c));
  }

  public Video find(String id) {
    return videos
        .findByYoutubeVideoId(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not tracked"));
  }

  public VideoDto detail(String id) {
    return dto(find(id));
  }

  public List<VideoDto> list() {
    return videos.findAll().stream()
        .map(this::dto)
        .sorted(Comparator.comparingDouble(VideoDto::recent1hVph).reversed())
        .toList();
  }

  private VideoDto dto(Video v) {
    var s = snapshots.findByVideoOrderByCollectedAtAsc(v);
    var m = analytics.calculate(v.publishedAt, s);
    var last = s.isEmpty() ? null : s.getLast();
    return new VideoDto(
        v.youtubeVideoId,
        v.title,
        v.channel.channelName,
        v.channel.isMyChannel,
        last == null ? 0 : last.viewCount,
        last == null ? 0 : last.likeCount,
        last == null ? 0 : last.commentCount,
        m.averageVph(),
        m.recent10mVph(),
        m.recent30mVph(),
        m.recent1hVph(),
        m.previous1hVph(),
        m.accelerationRate(),
        m.status(),
        v.thumbnailUrl,
        "https://www.youtube.com/shorts/" + v.youtubeVideoId,
        v.publishedAt,
        last == null ? null : last.collectedAt);
  }

  public List<SnapshotDto> history(String id) {
    var s = snapshots.findByVideoOrderByCollectedAtAsc(find(id));
    var out = new ArrayList<SnapshotDto>();
    for (int i = 0; i < s.size(); i++) {
      var x = s.get(i);
      double rate = 0;
      if (i > 0) {
        var p = s.get(i - 1);
        double hours = Duration.between(p.collectedAt, x.collectedAt).toMillis() / 3600000.0;
        if (hours > 0) rate = Math.max(0, x.viewCount - p.viewCount) / hours;
      }
      out.add(new SnapshotDto(x.viewCount, x.likeCount, x.commentCount, x.collectedAt, rate));
    }
    return out;
  }

  private long count(String value) {
    return value == null ? 0 : Long.parseLong(value);
  }

  public int collect(List<String> ids, boolean shortsOnly) {
    int saved = 0;
    for (var item : youtube.videos(ids)) {
      long duration = Duration.parse(item.contentDetails().duration()).getSeconds();
      if (shortsOnly && duration > 180) continue;
      var snippet = item.snippet();
      var c =
          channels
              .findByYoutubeChannelId(snippet.channelId())
              .orElseGet(
                  () -> {
                    var channel = new Channel();
                    channel.youtubeChannelId = snippet.channelId();
                    channel.channelName = snippet.channelTitle();
                    return channels.save(channel);
                  });
      var v = videos.findByYoutubeVideoId(item.id()).orElseGet(Video::new);
      v.youtubeVideoId = item.id();
      v.channel = c;
      v.title = snippet.title();
      v.description = snippet.description();
      v.publishedAt = snippet.publishedAt();
      v.durationSeconds = duration;
      var thumbs = snippet.thumbnails();
      v.thumbnailUrl =
          thumbs == null || thumbs.isEmpty()
              ? null
              : thumbs.getOrDefault("high", thumbs.values().iterator().next()).url();
      v.updatedAt = Instant.now();
      videos.save(v);
      var stats = item.statistics();
      if (stats == null) continue;
      var s = new VideoSnapshot();
      s.video = v;
      s.viewCount = count(stats.viewCount());
      s.likeCount = count(stats.likeCount());
      s.commentCount = count(stats.commentCount());
      s.collectedAt = Instant.now();
      snapshots.save(s);
      saved++;
    }
    return saved;
  }

  public VideoDto track(String id) {
    collect(List.of(id), false);
    return detail(id);
  }

  public VideoDto collectOne(String id) {
    find(id);
    collect(List.of(id), false);
    return detail(id);
  }

  public int sync(Long id) {
    var c =
        channels
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Channel not found"));
    Instant latest =
        videos.findAll().stream()
            .filter(v -> v.channel.id.equals(id))
            .map(v -> v.publishedAt)
            .max(Comparator.naturalOrder())
            .orElse(null);
    return collect(
        youtube.search(c.youtubeChannelId, latest == null ? null : latest.minusSeconds(1)), true);
  }

  public List<String> trackedIds() {
    return videos.findAll().stream().map(v -> v.youtubeVideoId).toList();
  }
}
