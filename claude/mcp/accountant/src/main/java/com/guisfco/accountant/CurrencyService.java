package com.guisfco.accountant;

import com.guisfco.accountant.dto.PtaxQuote;
import com.guisfco.accountant.dto.PtaxResponse;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.Map;

@Service
public class CurrencyService {

    private static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter BCB_DATE = DateTimeFormatter.ofPattern("MM-dd-yyyy");
    private static final String CLOSING_BULLETIN = "Fechamento";
    /** Covers weekends and long holidays when looking for the last closing bulletin. */
    private static final int LOOKBACK_DAYS = 5;

    private final RestClient restClient;

    public CurrencyService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata")
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Tool(description = "Convert an amount between currencies using the official Banco Central do Brasil PTAX closing buy rate "
            + "(the same value shown on bcb.gov.br/conversao). One side must be BRL, or both foreign currencies are converted through BRL. "
            + "The rate is the closing bulletin of the given date; if that date has none (weekend, holiday, or the market has not closed yet) "
            + "the previous business day's closing rate is used, and the result states which date was used.")
    public String convertCurrency(@ToolParam(description = "Amount to convert, e.g. 100") double amount,
                                  @ToolParam(description = "Source currency, e.g. USD") Currency source,
                                  @ToolParam(description = "Target currency, e.g. BRL") Currency target,
                                  @ToolParam(description = "Reference date as yyyy-MM-dd. Defaults to today in Brasília time", required = false) String date) {
        if (source == null || target == null) {
            return "Both source and target currencies are required.";
        }

        LocalDate reference;
        try {
            reference = date == null || date.isBlank() ? LocalDate.now(BRASILIA) : LocalDate.parse(date.trim());
        } catch (DateTimeParseException e) {
            return "Invalid date '%s'. Use yyyy-MM-dd.".formatted(date);
        }

        BigDecimal value = BigDecimal.valueOf(amount);
        if (source == target) {
            return "%s %s = %s %s (same currency)".formatted(format(value), source, format(value), target);
        }

        try {
            Rate sourceRate = source == Currency.BRL ? null : rateToBrl(source, reference);
            Rate targetRate = target == Currency.BRL ? null : rateToBrl(target, reference);

            BigDecimal inBrl = sourceRate == null ? value : value.multiply(sourceRate.value());
            BigDecimal result = targetRate == null ? inBrl : inBrl.divide(targetRate.value(), 10, RoundingMode.HALF_UP);

            return "%s %s = %s %s (PTAX closing buy rate%s)".formatted(
                    format(value), source, format(result), target, describe(source, sourceRate, target, targetRate));
        } catch (Exception e) {
            return "Could not convert %s to %s: %s".formatted(source, target, e.getMessage());
        }
    }

    private static String describe(Currency source, Rate sourceRate, Currency target, Rate targetRate) {
        StringBuilder text = new StringBuilder();
        if (sourceRate != null) {
            text.append(" 1 %s = %s BRL on %s".formatted(source, sourceRate.value().toPlainString(), sourceRate.date()));
        }
        if (targetRate != null) {
            if (sourceRate != null) {
                text.append(";");
            }
            text.append(" 1 %s = %s BRL on %s".formatted(target, targetRate.value().toPlainString(), targetRate.date()));
        }
        return ":" + text;
    }

    private static String format(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    /** BRL per 1 unit of the currency: the latest closing buy rate on or before the reference date. */
    private Rate rateToBrl(Currency currency, LocalDate reference) {
        LocalDate start = reference.minusDays(LOOKBACK_DAYS);

        PtaxResponse response = restClient.get()
                .uri(uri -> uri
                        .path("/CotacaoMoedaPeriodo(moeda=@moeda,dataInicial=@di,dataFinalCotacao=@df)")
                        .queryParam("@moeda", "'{currency}'")
                        .queryParam("@di", "'{start}'")
                        .queryParam("@df", "'{end}'")
                        .queryParam("$top", 50)
                        .queryParam("$format", "json")
                        .build(Map.of(
                                "currency", currency,
                                "start", start.format(BCB_DATE),
                                "end", reference.format(BCB_DATE))))
                .retrieve()
                .body(PtaxResponse.class);

        if (response == null || response.value() == null) {
            throw new IllegalStateException("no response from the Banco Central");
        }

        return response.value().stream()
                .filter(q -> CLOSING_BULLETIN.equals(q.tipoBoletim()) && q.cotacaoCompra() != null)
                .max(Comparator.comparing(PtaxQuote::dataHoraCotacao))
                .map(q -> new Rate(q.cotacaoCompra(), LocalDate.parse(q.dataHoraCotacao().substring(0, 10))))
                .orElseThrow(() -> new IllegalStateException(
                        "no %s closing rate found in the %d days up to %s".formatted(currency, LOOKBACK_DAYS, reference)));
    }

    private record Rate(BigDecimal value, LocalDate date) {
    }
}
