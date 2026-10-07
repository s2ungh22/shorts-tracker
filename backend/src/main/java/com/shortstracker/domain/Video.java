package com.shortstracker.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(indexes = @Index(name = "idx_video_youtube", columnList = "youtubeVideoId", unique = true))
public class Video {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, unique = true)
  public String youtubeVideoId;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  public Channel channel;

  public String title;

  @Column(columnDefinition = "text")
  public String description;

  public Instant publishedAt;
  public long durationSeconds;
  public String thumbnailUrl;
  public Instant createdAt = Instant.now();
  public Instant updatedAt = Instant.now();
}
