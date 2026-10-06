package org.example.billing.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class ApiProblemException extends ErrorResponseException {

    public ApiProblemException(HttpStatus status, String typeSlug, String title, String detail) {
        super(status, ProblemDetails.of(status, typeSlug, title, detail), null);
    }
}
