package com.shortstracker.repository;

import com.shortstracker.domain.Video;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoRepository extends JpaRepository<Video, Long> {
  java.util.Optional<Video> findByYoutubeVideoId(String id);
}
