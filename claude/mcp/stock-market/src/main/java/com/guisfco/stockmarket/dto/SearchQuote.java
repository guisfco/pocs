package com.guisfco.stockmarket.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SearchQuote(String symbol, String shortname, String longname, String quoteType, String exchDisp) {
}
