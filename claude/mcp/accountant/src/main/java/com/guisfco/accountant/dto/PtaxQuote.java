package com.guisfco.accountant.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * One PTAX bulletin. Rates are BRL per 1 unit of the foreign currency.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PtaxQuote(BigDecimal cotacaoCompra, BigDecimal cotacaoVenda, String dataHoraCotacao, String tipoBoletim) {
}
