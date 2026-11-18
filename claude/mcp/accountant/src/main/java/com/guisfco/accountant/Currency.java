package com.guisfco.accountant;

/**
 * Currencies published by the Banco Central do Brasil PTAX service, plus BRL itself.
 * To support a new one, add a constant here; the tool schema picks it up automatically.
 */
public enum Currency {
    BRL, USD, EUR, GBP, JPY, CHF, CAD, AUD, DKK, NOK, SEK
}
