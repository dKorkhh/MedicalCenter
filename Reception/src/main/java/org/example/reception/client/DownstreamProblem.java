package org.example.reception.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DownstreamProblem(String type, String title, Integer status, String detail, String instance) {
}
