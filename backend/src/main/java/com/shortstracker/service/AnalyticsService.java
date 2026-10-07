package com.shortstracker.service;

import com.shortstracker.config.GrowthRules;
import com.shortstracker.domain.*;
import com.shortstracker.dto.ApiDtos.Metrics;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {
  private final GrowthRules rules;

  public AnalyticsService(GrowthRules rules) {
    this.rules = rules;
  }

  private double hours(Instant a, Instant b) {
    return Duration.between(a, b).toMillis() / 3600000.0;
  }

  private VideoSnapshot nearest(List<VideoSnapshot> s, Instant target, Instant before) {
    return s.stream()
        .filter(x -> x.collectedAt.isBefore(before))
        .min(
            Comparator.comparingLong(
                x -> Math.abs(Duration.between(target, x.collectedAt).toMillis())))
        .orElse(null);
  }

  private double rate(VideoSnapshot a, VideoSnapshot b) {
    return a == null || b == null || !a.collectedAt.isBefore(b.collectedAt)
        ? 0
        : Math.max(0, b.viewCount - a.viewCount) / hours(a.collectedAt, b.collectedAt);
  }

  public Metrics calculate(Instant published, List<VideoSnapshot> input) {
    var s = input.stream().sorted(Comparator.comparing(x -> x.collectedAt)).toList();
    if (s.isEmpty()) return new Metrics(0, 0, 0, 0, 0, 0, GrowthStatus.NEW);
    var last = s.getLast();
    double age = Math.max(0, hours(published, last.collectedAt));
    var h = nearest(s, last.collectedAt.minusSeconds(3600), last.collectedAt);
    var prev = h == null ? null : nearest(s, h.collectedAt.minusSeconds(3600), h.collectedAt);
    double r = rate(h, last), p = rate(prev, h), acc = p > 0 ? (r - p) / p * 100 : 0;
    boolean declined = false;
    double prior = 0;
    for (int i = 1; i < s.size(); i++) {
      if (h != null && !s.get(i).collectedAt.isAfter(h.collectedAt)) {
        double v = rate(s.get(i - 1), s.get(i));
        if (prior >= rules.waveMinVph() && (v < prior * rules.declineRatio() || v < rules.lowVph()))
          declined = true;
        prior = v;
      }
    }
    GrowthStatus status = GrowthStatus.NORMAL;
    if (s.size() < 2 || age < rules.newHours()) status = GrowthStatus.NEW;
    else if (declined
        && prev != null
        && r >= rules.waveMinVph()
        && r >= p * rules.waveRatio()
        && age >= rules.secondWaveHours())
      status =
          age >= rules.reexpandingHours() ? GrowthStatus.REEXPANDING : GrowthStatus.SECOND_WAVE;
    else if (r >= rules.surgeVph() && acc >= rules.surgeAcceleration())
      status = GrowthStatus.SURGING;
    else if (r < rules.plateauVph()) status = GrowthStatus.PLATEAU;
    else if (p > 0 && r < p * rules.slowingRatio()) status = GrowthStatus.SLOWING;
    else if (p > 0 && r >= p * rules.risingRatio()) status = GrowthStatus.RISING;
    return new Metrics(
        age > 0 ? last.viewCount / age : 0,
        rate(nearest(s, last.collectedAt.minusSeconds(600), last.collectedAt), last),
        rate(nearest(s, last.collectedAt.minusSeconds(1800), last.collectedAt), last),
        r,
        p,
        acc,
        status);
  }
}
