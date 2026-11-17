package com.guisfco.stockmarket;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpConfig {

    @Bean
    public ToolCallbackProvider weatherTools(StockService stockService) {
        return MethodToolCallbackProvider.builder().toolObjects(stockService).build();
    }
}
