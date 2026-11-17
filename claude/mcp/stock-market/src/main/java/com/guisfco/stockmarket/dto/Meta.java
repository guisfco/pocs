package com.guisfco.stockmarket.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Meta(Double regularMarketPrice, String currency) {
}
