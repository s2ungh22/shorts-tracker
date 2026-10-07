package com.shortstracker;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:smoke;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "tracking.enabled=false",
      "youtube.api-key="
    })
@AutoConfigureMockMvc
class ApiSmokeTest {
  @Autowired MockMvc mvc;

  @Test
  void seededChannelAndEmptyDashboard() throws Exception {
    mvc.perform(get("/api/channels"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].youtubeChannelId").value("UCbeVcittZw4OMmRnbmd9ekg"));
    mvc.perform(get("/api/dashboard/trending"))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
  }

  @Test
  void channelRegistrationAndValidation() throws Exception {
    mvc.perform(
            post("/api/channels")
                .contentType("application/json")
                .content(
                    "{\"youtubeChannelId\":\"UC1234567890123456789012\",\"channelName\":\"Competitor\",\"isMyChannel\":false}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.channelName").value("Competitor"));
    mvc.perform(
            post("/api/channels")
                .contentType("application/json")
                .content("{\"youtubeChannelId\":\"bad\",\"channelName\":\"\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void missingKeyIsSafeAndUnknownVideoIs404() throws Exception {
    mvc.perform(
            post("/api/videos/track")
                .contentType("application/json")
                .content("{\"videoId\":\"UmGZGaMMy9c\"}"))
        .andExpect(status().isServiceUnavailable());
    mvc.perform(get("/api/videos/unknown")).andExpect(status().isNotFound());
  }
}
