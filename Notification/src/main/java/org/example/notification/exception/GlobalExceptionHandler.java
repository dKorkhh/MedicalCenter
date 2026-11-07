package org.example.notification.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
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

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return buildProblemDetail(HttpStatus.FORBIDDEN, "Access Denied", "You do not have permission to access this resource.");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationFailure(AuthenticationException ex) {
        return buildProblemDetail(HttpStatus.UNAUTHORIZED, "Authentication Required", "A valid bearer token is required to access this resource.");
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

    private ProblemDetail buildProblemDetail(HttpStatus status, String title, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setType(URI.create("https://datatracker.ietf.org/doc/html/rfc9457"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    private ProblemDetail buildProblemDetail(HttpStatus status, String title, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setType(URI.create("https://datatracker.ietf.org/doc/html/rfc9457"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }
}
