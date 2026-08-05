## Purpose

Provide AssetAZ callers with typed latest prices backed by explicitly enabled, durable observation histories.

## ADDED Requirements

### Requirement: Ticker exposes typed latest prices
The ticker SHALL expose the latest successfully persisted `PriceObservation` for a requested `TradingPair`. A `PriceObservation` SHALL contain exactly an `AssetPrice` and its observation `Instant`; the `AssetPrice` SHALL contain both its `ΩPriceΩ` value and `TradingPair`. The ticker SHALL initially support only `EVE_USDC`. A request for an unsupported or inactive pair, or for an enabled pair that has no persisted observation, SHALL throw a clear exception rather than return `null`.

#### Scenario: Latest supported price is available
- **WHEN** a caller requests `EVE_USDC` after at least one observation has been loaded or published
- **THEN** the ticker returns the latest observation with an `AssetPrice` identified by the `EVE_USDC` `TradingPair`

#### Scenario: Unsupported or inactive pair is requested
- **WHEN** a caller requests a pair that is unsupported or not activated
- **THEN** the ticker throws an exception that clearly identifies the unavailable trading pair

#### Scenario: Enabled pair has no persisted observation
- **WHEN** a caller requests an enabled pair before any observation has been successfully persisted
- **THEN** the ticker throws an exception that clearly states that no persisted price is available for that trading pair

### Requirement: Price history explicitly activates the pair
The ticker SHALL treat the existence of `data/assetaz/ticker/EVE_USDC.prices` when the pair is initialized as activation of `EVE_USDC`. It SHALL load every valid observation and select the observation with the greatest timestamp as latest, regardless of file order. It SHALL accept empty lines, full-line comments whose first non-whitespace character is `#`, and inline comments beginning at the first `#` on a data line. A missing file SHALL produce a warning containing the trading pair and expected filepath, leave the pair unavailable, and SHALL NOT be created automatically; an existing file with no data observations SHALL enable the pair without an initial latest price.

#### Scenario: Existing history activates and restores the pair
- **WHEN** the ticker starts with an existing history containing valid observations and permitted whitespace or comments
- **THEN** `EVE_USDC` is available and the observation with the greatest timestamp is its latest persisted price

#### Scenario: Empty history activates without a price
- **WHEN** the ticker starts with an existing empty history file
- **THEN** `EVE_USDC` is enabled with no initial latest observation

#### Scenario: Missing history leaves the pair unavailable
- **WHEN** the ticker starts without the `EVE_USDC` history file
- **THEN** it logs a warning containing `EVE_USDC` and `data/assetaz/ticker/EVE_USDC.prices`, continues starting without that pair, and does not create the file or its parent directories

#### Scenario: Malformed history is rejected clearly
- **WHEN** comment text is removed and a non-empty data line does not contain a valid UTC timestamp and price
- **THEN** Ticker startup fails with an error containing the history filename, the one-based physical line number, and enough of the invalid content to identify it

### Requirement: History uses the human-editable observation format
Each data line SHALL have the form `<UTC timestamp>:<price>`, where the timestamp uses `uuuuMMddHHmmssSSS'Z'` with millisecond precision in UTC. Automatic writes SHALL append plain data lines only and SHALL leave all existing comments and blank lines untouched.

#### Scenario: Observation is written in the required format
- **WHEN** an observation at `2026-08-05T13:15:42.783Z` with price `14.85` is persisted
- **THEN** the appended line is `20260805131542783Z:14.85`

#### Scenario: Existing operator annotations are preserved
- **WHEN** the ticker appends an observation to a history containing comments or blank lines
- **THEN** those existing lines remain unchanged and the appended observation contains neither a comment nor extra annotation

### Requirement: New observations are durable before publication
For enabled `EVE_USDC`, the ticker SHALL generate price `14.85` immediately at startup. After that startup attempt completes, and after each subsequent attempt completes, the ticker SHALL wait one minute before beginning the next attempt. It SHALL NOT use fixed-rate scheduling or perform catch-up attempts after a delay. For each generated observation it SHALL validate the observation, append it to history, and flush or force the data to persistent storage; every generated observation SHALL be persisted unless that persistence attempt fails. Only after persistence succeeds SHALL the ticker replace the in-memory latest observation, and then only if the newly persisted observation's `observedAt` timestamp is later than the current latest timestamp. If persistence fails, it SHALL report the failure clearly, SHALL NOT publish the attempted observation, and SHALL preserve the previously persisted latest observation. Concurrent reads SHALL never observe an unpersisted price, and latest SHALL always be the successfully persisted observation with the greatest `observedAt` timestamp across loaded history and newly persisted observations.

#### Scenario: Startup observation is persisted
- **WHEN** the ticker starts with an enabled writable history
- **THEN** it appends and forces a `14.85` observation to persistent storage before considering it for latest

#### Scenario: Periodic observation is persisted
- **WHEN** one minute has elapsed after the preceding observation attempt completed while the enabled ticker is running
- **THEN** it begins another attempt and appends and forces its `14.85` observation to persistent storage before considering it for latest

#### Scenario: Future-dated history remains latest
- **WHEN** history contains a successfully persisted observation whose timestamp is later than a newly generated observation
- **THEN** the newly generated observation is still persisted and the future-dated history observation remains latest

#### Scenario: Persistence fails
- **WHEN** appending or flushing a new observation fails
- **THEN** the failure is reported clearly, the failed observation is not published to any reader, and the previously persisted latest observation remains available
