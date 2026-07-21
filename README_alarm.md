# Jupiter Perps Price Alarm

The Jupiter Perps Price Alarm monitors the Jupiter Perps aggregated oracle accounts for SOL, ETH, and BTC. It evaluates configurable price conditions and dispatches matching alarms to independently configured console, Pushover, and Jupiter position-increase actions.

Oracle prices are received through Solana WebSocket `accountSubscribe` using `processed` commitment. The application does not poll prices once per second. It also fetches the current oracle account state whenever a WebSocket connection opens, reconnects automatically, and supports multiple RPC endpoints for redundancy.

> **Important:** This application is not necessarily notification-only. Every active entry in `alarmaction_JupiterPerpsPositionIncreaseAlarmAction.conf` can sign and submit a real Jupiter Perps transaction. See [Automatic position increase](#automatic-position-increase) before running it.

## Startup flow

The application performs the following steps before it starts monitoring prices:

1. Parse command-line options and the alarm configuration file.
2. Fetch the wallet's current open Jupiter Perps positions.
3. Populate entry-price and liquidation-price variables.
4. Resolve and validate every alarm condition.
5. Start the periodic variable refresher and file-trigger watcher.
6. Parse and validate the three action configuration files.
7. Open one oracle WebSocket connection per configured asset and RPC endpoint.

Startup fails before WebSockets are opened if an alarm condition or action configuration contains an unresolved variable or invalid value.

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

The repository contains example files for the alarm configuration and every action configuration. The real `conf/*.conf` files are ignored by Git; keep credentials and operational wallet details out of version control.

### Command-line options and environment variables

| Purpose | Command-line option | Environment variable | Default |
|---|---|---|---|
| Alarm configuration | `--config=<path>` | `PRICE_ALARMS_CONFIG` | `price-alarms.conf` |
| Solana WebSocket endpoints | `--ws=<url1,url2,...>` | `SOLANA_WS_URLS` | `wss://api.mainnet-beta.solana.com` |

The command-line option takes precedence over its environment variable.

The variable-refresh file watcher uses the configuration file's parent directory. Until that is changed, use a configuration path with an explicit directory component, such as `conf/alarms.conf` or `./price-alarms.conf`, rather than relying on the bare default filename.

## Configuration files

The application uses four configuration files:

| File | Purpose |
|---|---|
| `alarms.conf` | Variables and price-alarm definitions |
| `alarmaction_Console.conf` | Alarm IDs written to the console |
| `alarmaction_Pushover.conf` | Pushover credentials and notifications |
| `alarmaction_JupiterPerpsPositionIncreaseAlarmAction.conf` | Jupiter wallet, signer, and position increases |

The three action files are resolved in the same directory as the file supplied through `--config`. All four files are required at startup, although an action can contain no alarm IDs. Blank lines and lines beginning with `#` are ignored.

Example files:

- [alarms.conf.example](conf/alarms.conf.example)
- [alarmaction_Console.conf.example](conf/alarmaction_Console.conf.example)
- [alarmaction_Pushover.conf.example](conf/alarmaction_Pushover.conf.example)
- [alarmaction_JupiterPerpsPositionIncreaseAlarmAction.conf.example](conf/alarmaction_JupiterPerpsPositionIncreaseAlarmAction.conf.example)

## Shared variables

### Variable definitions

Variable definitions live in `alarms.conf`. A definition consists of a name and a single-token value:

```text
{{JUPITER_PERPS_WALLET}}  vj98roDZ7744EBfxyuDFkKpEGCsKQLr7K8UFRumJNHf
```

Variable names must use uppercase letters, digits, and underscores. User-defined variables can be referenced from alarm conditions and action configuration values as `{{NAME}}`.

`JUPITER_PERPS_WALLET` is required for the initial and periodic Jupiter position fetches. For open SOL, BTC, and ETH positions, the refresher supplies these dynamic variables:

```text
{{SOL_LONG_ENTRY_PRICE}}       {{SOL_LONG_LIQ_PRICE}}
{{SOL_SHORT_ENTRY_PRICE}}      {{SOL_SHORT_LIQ_PRICE}}
{{BTC_LONG_ENTRY_PRICE}}       {{BTC_LONG_LIQ_PRICE}}
{{BTC_SHORT_ENTRY_PRICE}}      {{BTC_SHORT_LIQ_PRICE}}
{{ETH_LONG_ENTRY_PRICE}}       {{ETH_LONG_LIQ_PRICE}}
{{ETH_SHORT_ENTRY_PRICE}}      {{ETH_SHORT_LIQ_PRICE}}
```

The refresher replaces user-supplied variables with these names when position data is fetched. Only variables for positions returned by Jupiter are populated. Startup validation fails if a condition or action value references a variable that is unavailable after the initial position fetch.

Alarm conditions, Pushover notes, and Jupiter position-increase rows are resolved again when an alarm triggers, so refreshed values take effect without restarting the application. Pushover credentials and severity plus the Jupiter wallet and signer are resolved during startup.

## Alarm definitions

Each alarm is one line with four columns:

```text
ID  ASSET  CONDITION  TRIGGER
```

Example:

```text
4  SOL  <={{SOL_LONG_LIQ_PRICE}}+1%  PERSISTENT:60
5  SOL  ({{SOL_LONG_LIQ_PRICE}}+1%-->{{SOL_LONG_LIQ_PRICE}}+3%]  PERSISTENT:3600
```

Supported column values:

- `ID`: integer alarm identifier. IDs should be unique. Action files use this ID to select alarms.
- `ASSET`: `SOL`, `ETH`, or `BTC`.
- `CONDITION`: a comparison or range expression. It must be one token with no whitespace.
- `TRIGGER`: `ONETIME`, `CROSSING`, `PERSISTENT`, or `PERSISTENT:<seconds>`.

A trailing comment is allowed after a variable value or trigger.

An alarm can be listed in zero, one, or several action files. Absence means that action ignores the alarm; absence from all action files means that the condition is monitored but produces no action.

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

On the first accepted oracle price after startup, an already-satisfied `ONETIME` or `PERSISTENT` alarm can trigger immediately. A `CROSSING` alarm only establishes its initial state.

### `ONETIME`

- Triggers on the first accepted price that satisfies the condition.
- Triggers at most once during the current process lifetime.
- Leaving and re-entering the condition does not re-arm it.
- Restarting the application re-arms it because alarm state is held only in memory.

### `CROSSING`

- Triggers when the condition changes from not satisfied to satisfied.
- The first accepted price establishes the initial state and never triggers the alarm.
- Remaining inside the condition does not trigger again.
- Leaving the condition re-arms the alarm, so the next entry triggers again.
- A grace period is not supported; `CROSSING:<seconds>` is rejected.

The condition determines the crossing direction. For example:

```text
19  SOL  >={{SOL_LONG_ENTRY_PRICE}}  CROSSING
20  SOL  <{{SOL_LONG_ENTRY_PRICE}}   CROSSING
```

Alarm 19 triggers when the price enters the profitable side from below. Alarm 20 triggers when it enters the losing side from above. With a range condition, `CROSSING` triggers whenever the price enters the range from either side.

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

Actions are executed sequentially through `CompositeAlarmAction`. A runtime failure in one action is logged so the remaining actions can still run.

### Console

`alarmaction_Console.conf` contains one alarm ID per line:

```text
1
4
5
```

Only listed alarms are written to the console. The output includes the asset, current oracle price, resolved condition, trigger type, oracle time, slot, and source endpoint. Duplicate or non-integer IDs are rejected. An empty file disables console output for all alarms.

### Pushover

`alarmaction_Pushover.conf` contains the credentials followed by zero or more notification definitions:

```text
APPLICATION_TOKEN  <pushover-application-token>
USER_KEY            <pushover-user-key>

# ID  SEVERITY  "NOTE"
4     CRITICAL  "🚨 SOL Long add 1.1x"
5     INFO      "🌱 SOL Long add 3x"
```

The application token and user/group key are required. Only listed alarm IDs send a Pushover notification. Notes must be quoted and support `\n`, `\r`, `\t`, `\"`, and `\\` escapes.

| Severity | Pushover parameters |
|---|---|
| `EMERGENCY` | `priority=2`, `retry=30`, `expire=10800`, `sound=persistent` |
| `CRITICAL` | `priority=1`, `sound=spacealarm` |
| `WARN` | `priority=0`, `sound=siren` |
| `INFO` | `priority=0` |
| `SILENT` | `priority=-1` |
| `GHOST` | `priority=-2` |

Credentials, severity, and notes may reference shared variables. Credentials and severity are resolved during startup; notes are also resolved for every trigger so refreshed variables take effect. Pushover requests are sent asynchronously with a 15-second request timeout. Rejected requests and asynchronous failures are logged.

### Automatic position increase

`alarmaction_JupiterPerpsPositionIncreaseAlarmAction.conf` contains the wallet and signer followed by zero or more position increases:

```text
WALLET_ID        {{JUPITER_PERPS_WALLET}}
SIGNER_KEY_NAME  <jup-key-name>

# ID  ASSET  DIRECTION  COLLATERAL_USDC  SIZE_DELTA_USD  MAX_SLIPPAGE_BPS
4     SOL    LONG       0.25             2.50            200
```

Each active row can submit a real transaction when its alarm triggers. Supported values are:

- `ASSET`: `SOL`, `ETH`, or `BTC`.
- `DIRECTION`: `LONG` or `SHORT`.
- `COLLATERAL_USDC`: greater than zero.
- `SIZE_DELTA_USD`: zero or greater.
- `MAX_SLIPPAGE_BPS`: from `0` to `10000`.

Wallet, signer, asset, direction, amounts, and slippage may reference shared variables. Wallet and signer are resolved during startup. Every transaction row is validated during startup and resolved again at trigger time, allowing refreshed variables to change the transaction parameters without a restart.

For a configured ID, the action builds a Jupiter position-increase transaction, signs it by running `jup sign -f json --key <SIGNER_KEY_NAME> --tx <serialized-transaction>`, verifies that the returned signer matches `WALLET_ID`, and submits the signed transaction. IDs absent from this file never perform a transaction.

The example transaction rows are commented out. Do not activate a row until its alarm condition, wallet, signer, asset, direction, amounts, and slippage have been reviewed.

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

Structural configuration errors include the file path and line number. After the initial Jupiter position fetch, every condition is resolved and parsed. The action files are then parsed and validated before WebSockets are opened.

Startup validation rejects, among other things:

- unknown or unavailable variables;
- missing or unknown comparison operators;
- conditions split by whitespace;
- malformed ranges or multiple `-->` separators;
- missing range targets;
- negative target values or percentages;
- ranges whose lower target exceeds the upper target;
- missing action credentials or wallet settings;
- duplicate action IDs;
- invalid Pushover severities;
- invalid Jupiter assets, directions, amounts, or slippage.

At runtime, a later condition resolution/parsing failure skips that price event for the affected alarm. A later action-variable failure is logged by the action dispatcher, and the remaining actions are still attempted.

## Current limitations

- `processed` commitment minimizes delay but an observed update may belong to a fork that is later abandoned.
- Solana PubSub is not a durable event log and does not guarantee delivery. Multiple independent endpoints reduce, but do not eliminate, this risk.
- Alarm state and grace-period timestamps are kept only in memory and reset on restart.
- Dynamic variables exist only for currently returned open positions.
- Target expressions support only a decimal value with an optional single percentage adjustment; they are not a general expression language.
- Configuration files are read only during startup; editing an action file requires a restart.
- Action IDs are not cross-validated against `alarms.conf`; an unknown ID is accepted but never triggered.
- Pushover credentials and severity plus the Jupiter wallet and signer are resolved only during startup. Dynamic re-resolution applies to conditions, Pushover notes, and Jupiter transaction rows.
- A bare default configuration path has no parent directory for the refresh watcher; use an explicit path such as `./price-alarms.conf` or `conf/alarms.conf`.
- This application is a monitoring and automation aid, not a substitute for independent risk controls.
