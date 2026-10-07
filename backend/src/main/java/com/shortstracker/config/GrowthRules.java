package com.shortstracker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("tracking.rules")
public record GrowthRules(
    double surgeVph,
    double surgeAcceleration,
    double plateauVph,
    double slowingRatio,
    double waveRatio,
    double waveMinVph,
    double risingRatio,
    double lowVph,
    double declineRatio,
    long secondWaveHours,
    long reexpandingHours,
    long newHours) {}
