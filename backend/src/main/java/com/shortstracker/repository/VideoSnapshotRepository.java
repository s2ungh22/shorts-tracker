package com.shortstracker.repository;

import com.shortstracker.domain.Video;
import com.shortstracker.domain.VideoSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoSnapshotRepository extends JpaRepository<VideoSnapshot, Long> {
  java.util.List<VideoSnapshot> findByVideoOrderByCollectedAtAsc(Video video);
}
