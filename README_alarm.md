# Jupiter Perps Price Alarm

The Jupiter Perps Price Alarm monitors the Jupiter Perps aggregated oracle accounts for SOL, ETH, and BTC. It evaluates configurable price conditions and dispatches matching alarms to the console, an optional Pushover account, and the currently hard-coded Jupiter position-increase action.

Oracle prices are received through Solana WebSocket `accountSubscribe` using `processed` commitment. The application does not poll prices once per second. It also fetches the current oracle account state whenever a WebSocket connection opens, reconnects automatically, and supports multiple RPC endpoints for redundancy.

> **Important:** This application is not currently notification-only. Alarm IDs `4` and `5` can sign and submit real Jupiter Perps position-increase transactions. See [Automatic position increase](#automatic-position-increase) before running it.

## Startup flow

The application performs the following steps before it starts monitoring prices:

1. Parse command-line options and the alarm configuration file.
2. Fetch the wallet's current open Jupiter Perps positions.
3. Populate entry-price and liquidation-price variables.
4. Resolve and validate every alarm condition.
5. Start the periodic variable refresher and file-trigger watcher.
6. Configure console, automatic Jupiter, and optional Pushover actions.
7. Open one oracle WebSocket connection per configured asset and RPC endpoint.

Startup fails before WebSockets and alarm actions are started if a condition contains an unresolved variable, invalid syntax, or an invalid range.

## Requirements and build

The project uses:

- JDK 25
- Gradle 9.3.1 through the Gradle wrapper
- Java preview features
- Detag-generated Java sources from `src/main/tjava`

Compile the project with:

```bash
./gradlew classes
```

To create the application JAR and collect runtime dependencies in `build/libs`:

```bash
./gradlew prepareLibs
```

There is currently no alarm-specific automated self-test task.

## Running the alarm application

The alarm entry point is:

```text
com.r35157.jupiterperpsalarm.impl.ref.JupiterPerpsAlarmImpl
```

The Gradle application plugin currently points at the Nenjim Hub main class, so `./gradlew run` does **not** directly start the alarm application. Run `JupiterPerpsAlarmImpl` from the IDE, or run the prepared JAR and dependencies explicitly:

```bash
java --enable-preview \
  -cp "build/libs/*" \
  com.r35157.jupiterperpsalarm.impl.ref.JupiterPerpsAlarmImpl \
  --config=conf/alarms.conf
```

The repository contains [the example configuration](conf/alarms.conf.example). Keep the real configuration outside version control if it contains operational wallet details.

### Command-line options and environment variables

| Purpose | Command-line option | Environment variable | Default |
|---|---|---|---|
| Alarm configuration | `--config=<path>` | `PRICE_ALARMS_CONFIG` | `price-alarms.conf` |
| Solana WebSocket endpoints | `--ws=<url1,url2,...>` | `SOLANA_WS_URLS` | `wss://api.mainnet-beta.solana.com` |
| Pushover application token | — | `PUSHOVER_APP_TOKEN` | Disabled |
| Pushover user/group key | — | `PUSHOVER_USER_KEY` | Disabled |

The command-line option takes precedence over its environment variable. Pushover is enabled only when both Pushover variables are non-blank.

The variable-refresh file watcher uses the configuration file's parent directory. Until that is changed, use a configuration path with an explicit directory component, such as `conf/alarms.conf` or `./price-alarms.conf`, rather than relying on the bare default filename.

## Configuration format

Blank lines and lines beginning with `#` are ignored. The file contains variable definitions and alarm definitions.

### Variable definitions

A variable definition consists of a name and a value:

```text
{{JUPITER_PERPS_WALLET}}  vj98roDZ7744EBfxyuDFkKpEGCsKQLr7K8UFRumJNHf
```

Variable names must use uppercase letters, digits, and underscores. Variables can be referenced in conditions and notes as `{{NAME}}`.

`JUPITER_PERPS_WALLET` is required for the initial and periodic Jupiter position fetches. For open SOL, BTC, and ETH positions, the refresher supplies these dynamic variables:

```text
{{SOL_LONG_ENTRY_PRICE}}       {{SOL_LONG_LIQ_PRICE}}
{{SOL_SHORT_ENTRY_PRICE}}      {{SOL_SHORT_LIQ_PRICE}}
{{BTC_LONG_ENTRY_PRICE}}       {{BTC_LONG_LIQ_PRICE}}
{{BTC_SHORT_ENTRY_PRICE}}      {{BTC_SHORT_LIQ_PRICE}}
{{ETH_LONG_ENTRY_PRICE}}       {{ETH_LONG_LIQ_PRICE}}
{{ETH_SHORT_ENTRY_PRICE}}      {{ETH_SHORT_LIQ_PRICE}}
```

Only variables for positions returned by Jupiter are populated. Startup validation fails if an alarm references a variable that is unavailable after the initial position fetch.

### Alarm definitions

Each alarm is one line with six columns:

```text
ID  ASSET  CONDITION  TRIGGER  SEVERITY  "NOTE"
```

Example:

```text
4  SOL  <={{SOL_LONG_LIQ_PRICE}}+1%  PERSISTENT:60  CRITICAL  "🚨 SOL Long add 1.1x"
5  SOL  ({{SOL_LONG_LIQ_PRICE}}+1%-->{{SOL_LONG_LIQ_PRICE}}+3%]  PERSISTENT:3600  INFO  "🌱 SOL Long add 3x"
```

Supported column values:

- `ID`: integer alarm identifier. IDs should be unique. IDs `4` and `5` have special transaction behavior in the current implementation.
- `ASSET`: `SOL`, `ETH`, or `BTC`.
- `CONDITION`: a comparison or range expression. It must be one token with no whitespace.
- `TRIGGER`: `ONETIME`, `PERSISTENT`, or `PERSISTENT:<seconds>`.
- `SEVERITY`: `EMERGENCY`, `CRITICAL`, `WARN`, `INFO`, `SILENT`, or `GHOST`.
- `NOTE`: quoted text. Supported escapes include `\n`, `\r`, `\t`, `\"`, and `\\`.

A trailing comment is allowed after a variable value or the quoted note.

## Condition expressions

A condition is evaluated against the current oracle price in USD. Conditions are resolved again for every accepted price event, so periodically refreshed entry and liquidation prices take effect without restarting the application.

The complete condition must not contain whitespace.

### Target expressions

Each target can be:

```text
75.50
{{SOL_LONG_LIQ_PRICE}}
{{SOL_LONG_LIQ_PRICE}}+1%
{{SOL_SHORT_LIQ_PRICE}}-3%
```

`BASE+P%` adds `P` percent of `BASE`; `BASE-P%` subtracts it. The base, percentage, and resulting target must be zero or positive. General arithmetic expressions are not supported.

### Comparisons

| Syntax | Matches when |
|---|---|
| `<X` | `price < X` |
| `<=X` | `price <= X` |
| `>X` | `price > X` |
| `>=X` | `price >= X` |

Examples:

```text
<={{SOL_LONG_LIQ_PRICE}}+1%
>={{SOL_SHORT_LIQ_PRICE}}-1%
```

### Ranges

Ranges use `-->` between a lower and an upper target. Parentheses exclude a boundary; square brackets include it.

| Syntax | Matches when |
|---|---|
| `(A-->B)` | `A < price < B` |
| `[A-->B)` | `A <= price < B` |
| `(A-->B]` | `A < price <= B` |
| `[A-->B]` | `A <= price <= B` |

The lower resolved target must not exceed the upper resolved target.

Example of two adjacent, non-overlapping long-position conditions:

```text
<={{SOL_LONG_LIQ_PRICE}}+1%
({{SOL_LONG_LIQ_PRICE}}+1%-->{{SOL_LONG_LIQ_PRICE}}+3%]
```

At exactly `LIQ+1%`, only the first condition matches. The corresponding short-position split is:

```text
>={{SOL_SHORT_LIQ_PRICE}}-1%
[{{SOL_SHORT_LIQ_PRICE}}-3%-->{{SOL_SHORT_LIQ_PRICE}}-1%)
```

## Trigger behavior

On the first accepted oracle price after startup or reconnect, an already-satisfied alarm can trigger immediately.

### `ONETIME`

- Triggers on the first accepted price that satisfies the condition.
- Triggers at most once during the current process lifetime.
- Leaving and re-entering the condition does not re-arm it.
- Restarting the application re-arms it because alarm state is held only in memory.

### `PERSISTENT`

- Triggers immediately on the first accepted matching price.
- With no grace period, it can trigger for every accepted matching price event.
- `PERSISTENT:<seconds>` limits repeated triggering to at most once per configured interval while the condition remains satisfied.
- Leaving and re-entering a condition does not bypass the grace period measured from the previous trigger.

Duplicate price events received from redundant RPC endpoints are suppressed using the raw price, exponent, and oracle timestamp. Up to 512 recent event keys are retained per asset monitor.

## Dynamic price-variable refresh

Entry-price and liquidation-price variables are refreshed:

- once during startup, before condition validation;
- every 60 seconds in a daemon task;
- when `jupiter-perps-alarm-var.refresh` is created in the configuration directory.

For example:

```bash
touch conf/jupiter-perps-alarm-var.refresh
```

The watcher detects creation of the file, refreshes the variables, and then deletes the trigger file.

When a dynamic price variable changes, the application logs its old and new value.

## Alarm actions

### Console

Console output is always enabled. It includes the asset, current oracle price, resolved condition, trigger type, oracle time, slot, and source endpoint.

Actions are executed sequentially through `CompositeAlarmAction`. A runtime failure in one action is logged so the remaining actions can still run.

### Pushover

Pushover is enabled when both `PUSHOVER_APP_TOKEN` and `PUSHOVER_USER_KEY` are configured.

| Severity | Pushover parameters |
|---|---|
| `EMERGENCY` | `priority=2`, `retry=30`, `expire=10800`, `sound=persistent` |
| `CRITICAL` | `priority=1`, `sound=spacealarm` |
| `WARN` | `priority=0`, `sound=siren` |
| `INFO` | `priority=0` |
| `SILENT` | `priority=-1` |
| `GHOST` | `priority=-2` |

Pushover requests are sent asynchronously with a 15-second request timeout. Rejected requests and asynchronous failures are logged.

### Automatic position increase

The current reference implementation always registers `JupiterPerpsPositionIncreaseAlarmAction`. It performs no transaction for most alarms, but alarm IDs `4` and `5` have hard-coded live behavior:

| Alarm ID | Asset/direction | USDC collateral | Position-size delta | Maximum slippage |
|---|---|---:|---:|---:|
| `4` | SOL long | `0.25` | `2.50` USD | `200` bps |
| `5` | SOL long | `0.25` | `6.25` USD | `200` bps |

For these IDs, the action builds a Jupiter position-increase transaction, signs it by running the external `jup sign` command with key name `evelyn-prod`, verifies that the returned signer matches the hard-coded wallet address, and submits the signed transaction.

The hard-coded wallet address is:

```text
vj98roDZ7744EBfxyuDFkKpEGCsKQLr7K8UFRumJNHf
```

Changing which alarm uses ID `4` or `5` does not change this action's hard-coded SOL-long behavior. Treat those IDs as operationally significant, and do not run the application unless the wallet, signer key, amounts, slippage, and conditions have been reviewed.

## RPC redundancy and reconnect behavior

Provide multiple comma-separated WebSocket endpoints for redundancy:

```bash
export SOLANA_WS_URLS='wss://first-provider.example,wss://second-provider.example'
```

The application creates one WebSocket connection for every configured asset/endpoint combination. With three configured assets and two endpoints, it creates six connections.

Each connection:

- subscribes with `processed` commitment and Base64 account encoding;
- fetches the current account state after opening;
- sends a heartbeat every 20 seconds;
- reconnects with exponential delays from 1 to 30 seconds.

## Validation and errors

Structural configuration errors include the file path and line number. After the initial Jupiter position fetch, every condition is resolved and parsed before periodic refresh, WebSockets, and alarm actions are started.

Startup validation rejects, among other things:

- unknown or unavailable variables;
- missing or unknown comparison operators;
- conditions split by whitespace;
- malformed ranges or multiple `-->` separators;
- missing range targets;
- negative target values or percentages;
- ranges whose lower target exceeds the upper target.

At runtime, a later resolution/parsing failure is logged for the affected alarm and that price event is skipped for that alarm.

## Current limitations

- `processed` commitment minimizes delay but an observed update may belong to a fork that is later abandoned.
- Solana PubSub is not a durable event log and does not guarantee delivery. Multiple independent endpoints reduce, but do not eliminate, this risk.
- Alarm state and grace-period timestamps are kept only in memory and reset on restart.
- Dynamic variables exist only for currently returned open positions.
- Target expressions support only a decimal value with an optional single percentage adjustment; they are not a general expression language.
- The automatic position-increase action is hard-coded to one wallet, signer key, asset, direction, and two alarm IDs.
- A bare default configuration path has no parent directory for the refresh watcher; use an explicit path such as `./price-alarms.conf` or `conf/alarms.conf`.
- This application is a monitoring and automation aid, not a substitute for independent risk controls.