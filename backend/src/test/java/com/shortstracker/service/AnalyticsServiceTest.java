package com.shortstracker.service;

import static org.junit.jupiter.api.Assertions.*;

import com.shortstracker.config.GrowthRules;
import com.shortstracker.domain.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class AnalyticsServiceTest {
  private final AnalyticsService service =
      new AnalyticsService(new GrowthRules(300, 50, 10, .6, 2, 100, 1.2, 30, .5, 6, 12, 1));
  private final Instant published = Instant.parse("2026-01-01T00:00:00Z");

  private VideoSnapshot s(double h, long views) {
    var s = new VideoSnapshot();
    s.collectedAt = published.plusMillis((long) (h * 3600000));
    s.viewCount = views;
    return s;
  }

  @Test
  void averageVph() {
    assertEquals(100, service.calculate(published, List.of(s(2, 200))).averageVph(), .001);
  }

  @Test
  void recent1hVphUsesActualTime() {
    assertEquals(
        100, service.calculate(published, List.of(s(1, 100), s(2.1, 210))).recent1hVph(), .001);
  }

  @Test
  void accelerationRate() {
    assertEquals(
        100,
        service.calculate(published, List.of(s(1, 100), s(2, 200), s(3, 400))).accelerationRate(),
        .001);
  }

  @Test
  void plateau() {
    assertEquals(
        GrowthStatus.PLATEAU, service.calculate(published, List.of(s(1, 100), s(2, 105))).status());
  }

  @Test
  void secondWave() {
    assertEquals(
        GrowthStatus.SECOND_WAVE,
        service
            .calculate(published, List.of(s(3, 0), s(4, 400), s(5, 450), s(6, 475), s(7, 675)))
            .status());
  }

  @Test
  void zeroDenominatorsAreFinite() {
    var m = service.calculate(published, List.of(s(0, 0), s(1, 0), s(2, 300)));
    assertTrue(Double.isFinite(m.accelerationRate()));
    assertEquals(0, m.previous1hVph());
  }

  @Test
  void reexpanding() {
    assertEquals(
        GrowthStatus.REEXPANDING,
        service
            .calculate(published, List.of(s(9, 0), s(10, 400), s(11, 450), s(12, 475), s(13, 675)))
            .status());
  }

  @Test
  void emptyHistory() {
    assertEquals(GrowthStatus.NEW, service.calculate(published, List.of()).status());
  }
}
