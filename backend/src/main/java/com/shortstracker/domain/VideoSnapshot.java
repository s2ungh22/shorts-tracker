package com.shortstracker.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(indexes = @Index(name = "idx_snapshot_video_time", columnList = "video_id,collectedAt"))
public class VideoSnapshot {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  public Video video;

  public long viewCount, likeCount, commentCount;
  public Instant collectedAt;
}
