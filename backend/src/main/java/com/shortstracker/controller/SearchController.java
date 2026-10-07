package com.shortstracker.controller;

import com.shortstracker.service.SearchService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
public class SearchController {
  private final SearchService service;

  public SearchController(SearchService service) {
    this.service = service;
  }

  public record SearchInput(
      @NotBlank @Size(max = 200) String query,
      @Min(1) @Max(7) int days,
      @Min(1) @Max(50) int maxResults) {}

  @PostMapping
  public SearchService.SearchResponse search(@Valid @RequestBody SearchInput input) {
    return service.search(input.query(), input.days(), input.maxResults());
  }
}
