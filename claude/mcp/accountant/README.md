# accountant

A [Model Context Protocol](https://modelcontextprotocol.io) server (Spring Boot + Spring AI, stdio transport) that exposes currency conversion based on the official Banco Central do Brasil **PTAX** rates.

## Tools

### `convertCurrency`

Converts an amount between currencies using the PTAX **closing buy rate** (`Fechamento` bulletin, `cotacaoCompra`), the same value shown on [bcb.gov.br/conversao](https://www.bcb.gov.br/conversao).

| Parameter | Required | Description |
|---|---|---|
| `amount` | yes | Amount to convert, e.g. `100` |
| `source` | yes | Source currency (see below) |
| `target` | yes | Target currency (see below) |
| `date` | no | Reference date as `yyyy-MM-dd`. Defaults to today in Brasília time |

Supported currencies: `BRL`, `USD`, `EUR`, `GBP`, `JPY`, `CHF`, `CAD`, `AUD`, `DKK`, `NOK`, `SEK`. Conversions between two foreign currencies go through BRL.

Example result:

```
100.00 EUR = 110.03 USD (PTAX closing buy rate: 1 EUR = 6.36470 BRL on 2026-11-17; 1 USD = 5.78450 BRL on 2026-11-17)
```

If the requested date has no closing bulletin (weekend, holiday, or the market has not closed yet), the previous business day's closing rate is used, looking back up to 5 days. The result always states the date of the rate that was used.

## Requirements

- Java 27 (the project is compiled for it, see `java.version` in `pom.xml`)
- Internet access to `olinda.bcb.gov.br`

## Build

```bash
JAVA_HOME=/path/to/jdk-27 ./mvnw clean install
```

This produces `target/accountant-0.0.1-SNAPSHOT.jar`.

## Register in Claude Code (user scope)

User scope makes the server available in every project. Use absolute paths, and point `java` at a Java 27 binary: Claude launches the command itself, and an older `java` on your `PATH` fails with `UnsupportedClassVersionError` and shows up as `CONNECTION_CLOSED`.

```bash
claude mcp add --scope user accountant -- /path/to/jdk-27/bin/java -jar /path/to/accountant/target/accountant-0.0.1-SNAPSHOT.jar
```

Check that it connected:

```bash
claude mcp list
claude mcp get accountant
```

Inside a Claude Code session, `/mcp` shows the server status and reconnects it. After rebuilding the jar, run `/mcp` to reconnect. A new or changed tool schema may only appear in a new session.

Remove it with:

```bash
claude mcp remove accountant --scope user
```

## Notes

- The server speaks MCP over stdio, so Claude Code starts and stops the process itself. You don't run it manually, and nothing listens on a port.
- Don't print to stdout from the application: stdout carries the protocol.
- To support another currency, add a constant to `Currency`. The tool schema picks it up automatically.
