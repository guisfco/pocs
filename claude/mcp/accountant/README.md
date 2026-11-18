# accountant

A [Model Context Protocol](https://modelcontextprotocol.io) server (Spring Boot + Spring AI, stdio transport) that exposes accounting helpers: currency conversion based on the official Banco Central do Brasil **PTAX** rates, and hourly rate / pay calculations from a base salary.

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
100 EUR = 110.030253263 USD (PTAX closing buy rate: 1 EUR = 6.36470 BRL on 2026-11-17; 1 USD = 5.78450 BRL on 2026-11-17)
```

If the requested date has no closing bulletin (weekend, holiday, or the market has not closed yet), the previous business day's closing rate is used, looking back up to 5 days. The result always states the date of the rate that was used.

### `calculateHourlyRate`

Hourly rate from a base salary and the hours it covers (`baseSalary / contractedHours`).

| Parameter | Required | Description |
|---|---|---|
| `baseSalary` | yes | Base salary for the contracted period |
| `contractedHours` | no | Hours the salary covers. Defaults to `180` |

Example: `baseSalary=10000` gives `Hourly rate: 55.5555555556 per hour (10000 for 180 hours)`.

### `calculatePay`

How much to receive for the hours actually worked, proportional to the base salary. No overtime or bonus rules are applied.

| Parameter | Required | Description |
|---|---|---|
| `hoursWorked` | yes | Hours actually worked in the period |
| `baseSalary` | yes | Base salary for the contracted period |
| `contractedHours` | no | Hours the salary covers. Defaults to `180` |

Example: `hoursWorked=150`, `baseSalary=10000` gives `Pay for 150 hours: 8333.3333333333 (hourly rate 55.5555555556, base salary 10000 for 180 hours)`.

Amounts are in whatever currency the salary is given in. To see the pay in another currency, pass the result to `convertCurrency`.

## Precision

None of the tools round their results. Multiplications are exact. Divisions that don't end (for example `10000 / 176`) are carried to 10 decimal places, with the last digit rounded half-up, and trailing zeros are removed.

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
