package com.shortstracker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  @Value("${frontend.origin:http://localhost:5173}")
  private String origin;

  public void addCorsMappings(CorsRegistry r) {
    r.addMapping("/api/**").allowedOrigins(origin).allowedMethods("GET", "POST");
  }
}
