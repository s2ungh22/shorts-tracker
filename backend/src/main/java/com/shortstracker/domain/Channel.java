package com.shortstracker.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    indexes = @Index(name = "idx_channel_youtube", columnList = "youtubeChannelId", unique = true))
public class Channel {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, unique = true)
  public String youtubeChannelId;

  @Column(nullable = false)
  public String channelName;

  public boolean isMyChannel;
  public Instant createdAt = Instant.now();
}
