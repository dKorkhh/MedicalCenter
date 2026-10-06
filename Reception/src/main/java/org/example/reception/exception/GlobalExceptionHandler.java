package org.example.reception.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.example.reception.client.DownstreamProblemException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail pd) {
            ProblemDetails.enrich(pd);
        }
        return response;
    }

    @ExceptionHandler(DownstreamProblemException.class)
    public ProblemDetail handleDownstreamProblem(DownstreamProblemException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatus()) != null ? HttpStatus.resolve(ex.getStatus()) : HttpStatus.BAD_GATEWAY;
        String detail = ex.getProblem() != null && ex.getProblem().detail() != null
                ? ex.getProblem().detail() : "Billing service rejected the request.";
        ProblemDetail pd = ProblemDetails.of(status, "downstream-error", "Billing service error", detail);
        pd.setProperty("downstreamService", "billing-service");
        if (ex.getProblem() != null && ex.getProblem().type() != null) {
            pd.setProperty("downstreamType", ex.getProblem().type());
        }
        return pd;
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ProblemDetail> handleCircuitOpen(CallNotPermittedException ex) {
        ProblemDetail pd = ProblemDetails.of(HttpStatus.SERVICE_UNAVAILABLE, "circuit-open",
                "Dependency temporarily unavailable", "Billing service circuit breaker is OPEN. Retry later.");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).header(HttpHeaders.RETRY_AFTER, "10").body(pd);
    }

    @ExceptionHandler(BulkheadFullException.class)
    public ResponseEntity<ProblemDetail> handleBulkheadFull(BulkheadFullException ex) {
        ProblemDetail pd = ProblemDetails.of(HttpStatus.TOO_MANY_REQUESTS, "bulkhead-full",
                "Too many concurrent calls", "Concurrent call limit to Billing service is reached.");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).header(HttpHeaders.RETRY_AFTER, "1").body(pd);
    }

    @ExceptionHandler({ResourceAccessException.class, HttpServerErrorException.class})
    public ProblemDetail handleDownstreamUnavailable(Exception ex) {
        log.warn("Downstream failure: {}", ex.toString());
        return ProblemDetails.of(HttpStatus.BAD_GATEWAY, "downstream-unavailable",
                "Downstream service failure", "Billing service is unavailable or returned a server error.");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ProblemDetails.of(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error",
                "Internal Server Error", "An unexpected error occurred. Please contact support with the correlationId.");
    }
}
