package com.shortstracker.repository;

import com.shortstracker.domain.Channel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelRepository extends JpaRepository<Channel, Long> {
  java.util.Optional<Channel> findByYoutubeChannelId(String id);
}
