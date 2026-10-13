package org.example.reception.client;

public class DownstreamProblemException extends RuntimeException {

    private final int status;
    private final transient DownstreamProblem problem;

    public DownstreamProblemException(int status, DownstreamProblem problem, Throwable cause) {
        super("Billing service responded with HTTP " + status, cause);
        this.status = status;
        this.problem = problem;
    }

    public int getStatus() {
        return status;
    }

    public DownstreamProblem getProblem() {
        return problem;
    }
}
