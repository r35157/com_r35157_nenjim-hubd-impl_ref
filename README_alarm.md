# Jupiter Perps Price Alarm

A small Java program that listens to Jupiter Perps' on-chain aggregated oracle accounts through Solana WebSocket `accountSubscribe`.

It does **not** poll once per second. Every account update observed by the connected RPC node is decoded immediately. The program reconnects automatically, performs an initial/reconnect state fetch, and can connect to multiple independent RPC endpoints for redundancy.

The program supports any number of alarms for SOL, ETH, and BTC. Only one oracle stream is created per configured asset and RPC endpoint, regardless of how many alarms use that asset.

## Java version

The project uses the current JDK compiler but generates Java 17-compatible class files:

```gradle
tasks.withType(JavaCompile).configureEach {
    options.release = 17
}
```

## Alarm configuration

The default file is `price-alarms.conf`:

```text
# Asset   Direction   Target    TRIGGER     SEVERITY   NOTE
##################################################################################################
SOL       ABOVE       75.7      ONETIME     2          "ALARM: Risiko for Perps Solana short LIKVIDERING!"
SOL       BELOW       60.8      ONETIME     2          "ALARM: Risiko for Perps Solana long LIKVIDERING!"
SOL       BELOW       71.4      ONETIME     2          "ALARM: Risiko for Solana Raydium LÅN LIKVIDERING!"
ETH       ABOVE       1848.41   ONETIME     2          "ALARM: Risiko for Perps Ethereum short LIKVIDERING!"
ETH       BELOW       1789      ONETIME     1          "OK: Perps Ethereum short er lukket!"
```

Supported values:

- Asset: `SOL`, `ETH`, or `BTC`
- Direction: `ABOVE` or `BELOW`
- Trigger: `ONETIME` or `PERSISTENT`
- Target: positive decimal USD price
- Severity: zero or positive integer
- Note: quoted text; escaped quotes can be written as `\"`

`SEVERITY` and `NOTE` are parsed and retained in `PriceAlarmDefinition`, but are intentionally not used by the alarm actions yet.

## Trigger behavior

On the first received price after program start:

- An already satisfied alarm triggers immediately.
- An unsatisfied alarm waits for the price to cross into its triggered side.

Alarm state is retained across WebSocket reconnects within the same process. If the price moves from the safe side to the triggered side during a connection outage, the first price received after reconnect will therefore trigger the alarm.

After that:

- `ONETIME` triggers only once during the current program run.
- `PERSISTENT` triggers each time the price crosses from the safe side into the triggered side.
- Remaining on the triggered side does not repeatedly fire the alarm.

`ONETIME` state is currently kept in memory. Restarting the process arms the alarm again.

## Build and test

```bash
gradle classes
gradle run --args='--self-test'
```

The self-test covers:

- binary oracle decoding
- configuration parsing
- initial satisfied alarm behavior
- `ONETIME` behavior
- `PERSISTENT` crossing behavior

## Run

Using the default `price-alarms.conf` in the working directory:

```bash
gradle run
```

Using another file:

```bash
gradle run --args='--config=/path/to/price-alarms.conf'
```

The path can also be selected through:

```bash
export PRICE_ALARMS_CONFIG='/path/to/price-alarms.conf'
```

## Use two RPC WebSocket streams

A single WebSocket/RPC provider is not a durable event log. For better resilience, provide two independent endpoints:

```bash
export SOLANA_WS_URLS='wss://first-provider.example,wss://second-provider.example'
gradle run
```

With two configured assets and two RPC endpoints, the program opens four WebSocket connections.

## Pushover emergency alarm

```bash
export PUSHOVER_APP_TOKEN='...'
export PUSHOVER_USER_KEY='...'
gradle run
```

The current implementation sends `priority=2`, `retry=30`, `expire=10800`, and `sound=persistent`.

## Important limitations

- `processed` is intentionally used for minimum delay, but a processed update may belong to a fork that is later abandoned.
- Solana PubSub is not guaranteed delivery. Two independent RPC streams reduce, but do not eliminate, the risk of missing an update.
- The alarm reports the Jupiter Perps oracle price. It does not prove that a specific position was liquidated.
- This is an alerting aid, not a substitute for placing an on-platform stop-loss or reducing leverage.
