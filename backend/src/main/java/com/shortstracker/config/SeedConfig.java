package com.shortstracker.config;

import com.shortstracker.dto.ApiDtos.ChannelInput;
import com.shortstracker.service.TrackingService;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;

@Configuration
public class SeedConfig {
  @Bean
  CommandLineRunner seed(
      TrackingService service, @Value("${tracking.seed-videos:false}") boolean seedVideos) {
    return args -> {
      if (service.channels().stream()
          .noneMatch(c -> c.youtubeChannelId().equals("UCbeVcittZw4OMmRnbmd9ekg")))
        service.addChannel(new ChannelInput("UCbeVcittZw4OMmRnbmd9ekg", "이거괜찮네", true));
      if (seedVideos)
        for (String id : java.util.List.of("UmGZGaMMy9c", "q3LoMSRnTpM", "iJzJYKEs59I")) {
          try {
            service.track(id);
          } catch (Exception e) {
            LoggerFactory.getLogger(SeedConfig.class)
                .warn("Seed {} unavailable ({})", id, e.getClass().getSimpleName());
          }
        }
    };
  }
}
