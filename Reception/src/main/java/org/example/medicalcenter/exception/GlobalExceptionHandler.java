package org.example.medicalcenter.exception;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatusException(ResponseStatusException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(ex.getStatusCode(), ex.getReason() != null ? ex.getReason() : ex.getMessage());
        problemDetail.setTitle("Request Error");
        problemDetail.setType(URI.create("https://datatracker.ietf.org/doc/html/rfc9457"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleAllUncaughtException(Exception ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage()
        );
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create("https://datatracker.ietf.org/doc/html/rfc9457"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }
}