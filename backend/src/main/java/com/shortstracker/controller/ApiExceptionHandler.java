package com.shortstracker.controller;

import com.shortstracker.client.YouTubeClient.YouTubeUnavailableException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(YouTubeUnavailableException.class)
  public ResponseEntity<ProblemDetail> youtube(YouTubeUnavailableException e) {
    return ResponseEntity.status(502)
        .body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, e.getMessage()));
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ProblemDetail> config(IllegalStateException e) {
    return ResponseEntity.status(503)
        .body(ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage()));
  }

  @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
  public ResponseEntity<ProblemDetail> conflict() {
    return ResponseEntity.status(409)
        .body(
            ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "Conflicting record; retry the operation"));
  }
}
