package com.example.gitactivity.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(RepositoryNotFoundException.class)
  public ResponseEntity<String> handleRepositoryNotFound(
          RepositoryNotFoundException exception) {

    return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(exception.getMessage());
  }

  @ExceptionHandler(GitHubRateLimitException.class)
  public ResponseEntity<String> handleGitHubRateLimitException(
          GitHubRateLimitException exception) {

      return ResponseEntity
              .status(HttpStatus.TOO_MANY_REQUESTS)
              .body(exception.getMessage());
  }

  @ExceptionHandler(GitHubServiceException.class)
  public ResponseEntity<String> handleGitHubServiceException(
          GitHubServiceException exception) {

      return ResponseEntity
              .status(HttpStatus.BAD_GATEWAY)
              .body(exception.getMessage());
  }

}