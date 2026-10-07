package com.shortstracker.controller;

import com.shortstracker.dto.ApiDtos.*;
import com.shortstracker.service.TrackingService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ApiController {
  private final TrackingService service;

  public ApiController(TrackingService service) {
    this.service = service;
  }

  @GetMapping("/dashboard/trending")
  public List<VideoDto> trending() {
    return service.list();
  }

  @GetMapping("/videos")
  public List<VideoDto> videos() {
    return service.list();
  }

  @GetMapping("/videos/{id}")
  public VideoDto video(@PathVariable String id) {
    return service.detail(id);
  }

  @GetMapping("/videos/{id}/history")
  public List<SnapshotDto> history(@PathVariable String id) {
    return service.history(id);
  }

  @GetMapping("/channels")
  public List<ChannelDto> channels() {
    return service.channels();
  }

  @PostMapping("/channels")
  public ChannelDto channel(@Valid @RequestBody ChannelInput in) {
    return service.addChannel(in);
  }

  @PostMapping("/videos/track")
  public VideoDto track(@Valid @RequestBody TrackInput in) {
    return service.track(in.videoId());
  }

  @PostMapping("/videos/{id}/collect")
  public VideoDto collect(@PathVariable String id) {
    return service.collectOne(id);
  }

  @PostMapping("/channels/{id}/sync")
  public Map<String, Integer> sync(@PathVariable Long id) {
    return Map.of("collected", service.sync(id));
  }
}
