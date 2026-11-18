package com.guisfco.stockmarket;

import com.guisfco.stockmarket.dto.ChartResponse;
import com.guisfco.stockmarket.dto.Meta;
import com.guisfco.stockmarket.dto.SearchResponse;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StockService {

    private final RestClient restClient;

    public StockService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://query1.finance.yahoo.com")
                .defaultHeader("Accept", "application/json")
                .defaultHeader("User-Agent", "Mozilla/5.0")
                .build();
    }

    private static final int MAX_SEARCH_RESULTS = 8;

    @Tool(description = "Search for a ticker symbol by company, asset or fund name (e.g. 'americanas', 'bitcoin', 'apple'). "
            + "Returns matching symbols with name, type and exchange. Use the returned symbol with getCurrentPrice.")
    public String searchSymbol(@ToolParam(description = "Name or partial name to search for") String query) {
        if (query == null || query.isBlank()) {
            return "Please provide a search query.";
        }

        try {
            SearchResponse response = restClient.get()
                    .uri("/v1/finance/search?q={q}&quotesCount={n}&newsCount=0&listsCount=0", query.trim(), MAX_SEARCH_RESULTS)
                    .retrieve()
                    .body(SearchResponse.class);

            if (response == null || response.quotes() == null || response.quotes().isEmpty()) {
                return "No symbols found for '%s'".formatted(query);
            }

            return response.quotes().stream()
                    .map(q -> "%s | %s | %s | %s".formatted(
                            q.symbol(),
                            q.longname() != null ? q.longname() : q.shortname(),
                            q.quoteType(),
                            q.exchDisp()))
                    .collect(Collectors.joining("\n", "symbol | name | type | exchange\n", ""));
        } catch (Exception e) {
            return "Could not search for '%s': %s".formatted(query, e.getMessage());
        }
    }

    @Tool(description = "Get the current price for a specific stock, crypto, index or currency pair. "
            + "Pass the market so the symbol is resolved correctly. Supported markets: B3 (Brazil, e.g. PETR4), "
            + "US (e.g. AAPL), LSE (e.g. VOD), XETRA (e.g. SAP), TSE (e.g. 7203), TSX (e.g. SHOP), "
            + "CRYPTO (e.g. BTC-USD), INDEX (e.g. ^BVSP, ^GSPC), FX (e.g. BRL=X). "
            + "If the market is omitted, the symbol is sent as given (no suffix), so it must already be a full Yahoo symbol such as AMBP3.SA.")
    public String getCurrentPrice(
            @ToolParam(description = "Stock symbol (e.g. PETR4, AAPL, BTC-USD)") String stockSymbol,
            @ToolParam(description = "Market of the symbol: B3, US, LSE, XETRA, TSE, TSX, CRYPTO, INDEX or FX", required = false) String market) {
        String symbol = stockSymbol.trim().toUpperCase();

        if (market != null && !market.isBlank()) {
            Optional<Market> parsed = Market.parse(market);
            if (parsed.isEmpty()) {
                return "Unknown market '%s'. Supported markets: %s".formatted(market, Market.supported());
            }
            symbol = parsed.get().toYahooSymbol(symbol);
        }

        try {
            ChartResponse response = restClient.get()
                    .uri("/v8/finance/chart/{symbol}?interval=1d&range=1d", symbol)
                    .retrieve()
                    .body(ChartResponse.class);

            if (response == null || response.chart() == null
                    || response.chart().result() == null || response.chart().result().isEmpty()
                    || response.chart().result().getFirst().meta() == null
                    || response.chart().result().getFirst().meta().regularMarketPrice() == null) {
                return "No price found for '%s'".formatted(stockSymbol);
            }

            Meta meta = response.chart().result().getFirst().meta();
            return "The current price for '%s' is %.2f %s".formatted(
                    stockSymbol, meta.regularMarketPrice(), meta.currency() == null ? "" : meta.currency()).trim();
        } catch (Exception e) {
            return "Could not fetch the price for '%s': %s".formatted(stockSymbol, e.getMessage());
        }
    }
}
