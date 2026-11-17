package com.guisfco.stockmarket.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SearchResponse(List<SearchQuote> quotes) {
}
