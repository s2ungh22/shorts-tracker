package com.shortstracker.scheduler;

import com.shortstracker.client.YouTubeClient;
import com.shortstracker.service.TrackingService;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CollectionScheduler {
  private static final Logger log = LoggerFactory.getLogger(CollectionScheduler.class);
  private final TrackingService service;
  private final YouTubeClient client;

  @Value("${tracking.enabled:true}")
  private boolean enabled;

  public CollectionScheduler(TrackingService service, YouTubeClient client) {
    this.service = service;
    this.client = client;
  }

  @Scheduled(
      fixedDelayString = "${tracking.snapshot-interval-ms:600000}",
      initialDelayString = "${tracking.snapshot-interval-ms:600000}")
  public void snapshots() {
    if (!enabled || !client.configured()) return;
    var ids = service.trackedIds();
    for (int i = 0; i < ids.size(); i += 50) {
      try {
        log.info(
            "Collected {} snapshots",
            service.collect(ids.subList(i, Math.min(i + 50, ids.size())), false));
      } catch (Exception e) {
        log.warn("Snapshot batch failed ({})", e.getClass().getSimpleName());
      }
    }
  }

  @Scheduled(
      fixedDelayString = "${tracking.discovery-interval-ms:3600000}",
      initialDelayString = "${tracking.discovery-interval-ms:3600000}")
  public void discover() {
    if (!enabled || !client.configured()) return;
    for (var c : service.channels()) {
      try {
        log.info("Channel {} collected {} videos", c.id(), service.sync(c.id()));
      } catch (Exception e) {
        log.warn("Channel {} sync failed ({})", c.id(), e.getClass().getSimpleName());
      }
    }
  }
}
