package com.guisfco.stockmarket;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Supported markets and the Yahoo Finance suffix each one uses.
 * To support a new market, add one constant here.
 */
public enum Market {

    B3(".SA"),
    US(""),
    LSE(".L"),
    XETRA(".DE"),
    TSE(".T"),
    TSX(".TO"),
    CRYPTO(""),
    INDEX(""),
    FX("");

    private final String suffix;

    Market(String suffix) {
        this.suffix = suffix;
    }

    /** Builds the Yahoo symbol; symbols that already carry a suffix are left untouched. */
    public String toYahooSymbol(String symbol) {
        return symbol.contains(".") ? symbol : symbol + suffix;
    }

    public static Optional<Market> parse(String value) {
        return Arrays.stream(values())
                .filter(m -> m.name().equalsIgnoreCase(value.trim()))
                .findFirst();
    }

    public static String supported() {
        return Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", "));
    }
}
