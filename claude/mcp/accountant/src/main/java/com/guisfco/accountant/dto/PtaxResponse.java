package com.guisfco.accountant.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PtaxResponse(List<PtaxQuote> value) {
}
