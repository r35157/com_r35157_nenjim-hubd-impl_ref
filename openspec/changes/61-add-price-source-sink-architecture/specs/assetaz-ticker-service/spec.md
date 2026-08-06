## ADDED Requirements

### Requirement: Price sources are registered and lifecycle-managed
The ticker SHALL accept independent price sources identified by the combination of their `TradingPair` and stable source name. A source SHALL be registered before ticker startup, and duplicate registrations with the same trading pair and source name SHALL be rejected clearly. Source names SHALL be non-empty safe directory components and SHALL reject `/`, `\`, `..`, and control characters without rewriting them. The ticker SHALL start each registered source whose history is active and SHALL stop every source it started. Price sources SHALL announce a typed price and observation timestamp through a sink without knowing their persistence path.

#### Scenario: Distinct sources are registered
- **WHEN** two sources have different source names or trading pairs and are registered before startup
- **THEN** the ticker accepts both registrations and manages each source independently

#### Scenario: Duplicate persistent identity is rejected
- **WHEN** a source is registered with the same trading pair and source name as an existing registration
- **THEN** registration fails with an error identifying the duplicate source history

#### Scenario: Unsafe source name is rejected
- **WHEN** a source name is empty or contains a slash, backslash, `..`, or a control character
- **THEN** registration or startup fails clearly without rewriting the source name or creating filesystem content

#### Scenario: Registration after startup is rejected
- **WHEN** a caller attempts to register a source after ticker startup has begun
- **THEN** registration fails clearly and the running source set remains unchanged

#### Scenario: Active source lifecycle is managed
- **WHEN** the ticker starts and later stops with an active registered source
- **THEN** it starts that source once and subsequently stops it without leaving source-owned resources running

## MODIFIED Requirements

### Requirement: Ticker exposes typed latest prices
The ticker SHALL expose the latest successfully persisted `PriceObservation` for a requested `TradingPair` across all active registered sources for that pair. A `PriceObservation` SHALL contain exactly an `AssetPrice`, its observation `Instant`, and the stable source name; the `AssetPrice` SHALL contain both its `ΩPriceΩ` value and `TradingPair`. If several active sources provide a pair, latest SHALL be the observation with the greatest `observedAt` timestamp, and its source name SHALL identify its source. A request for a pair with no registered sources, no active sources, or no successfully persisted observation SHALL throw a clear exception rather than return `null`.

#### Scenario: Latest supported price is available
- **WHEN** a caller requests a trading pair after at least one observation has been loaded or persisted by an active source
- **THEN** the ticker returns the successfully persisted observation with the greatest timestamp and its source identity

#### Scenario: Newest observation is selected across sources
- **WHEN** multiple active sources for one trading pair have successfully persisted observations
- **THEN** the ticker returns the observation with the greatest `observedAt` across those sources

#### Scenario: Pair has no registered or active source
- **WHEN** a caller requests a pair with no registered source or whose registered sources are all inactive
- **THEN** the ticker throws an exception that clearly identifies the unavailable trading pair

#### Scenario: Active pair has no persisted observation
- **WHEN** a caller requests a pair whose active sources have no successfully persisted observation
- **THEN** the ticker throws an exception that clearly states that no persisted price is available for that trading pair

### Requirement: Price history explicitly activates each source
For each registered source, the ticker SHALL derive an independent history path as `data/assetaz/ticker/<base UUID>/<quote UUID>/<source name>/<safe base symbol>_<safe quote symbol>.prices`. UUIDs SHALL use their unchanged canonical representation and SHALL be the technical trading-pair identity; sanitized symbols SHALL be used only for the human-readable filename. Only an existing file at that exact path SHALL activate the registered source. The ticker SHALL neither scan for unregistered sources nor create or migrate missing files or directories. It SHALL load every valid observation using the registered source's trading pair and source name and select the greatest timestamp across all active histories for each pair, regardless of file order. Empty lines, full-line comments, and inline comments SHALL remain supported. An existing history with no data SHALL activate only that source without an initial observation, while a missing history SHALL leave only that source inactive and emit a warning containing the trading pair, source name, and complete expected path.

#### Scenario: Existing source history activates and restores identity
- **WHEN** a registered source has an existing valid history with permitted whitespace or comments
- **THEN** that source is active and its observations are restored with its registered trading pair and source name

#### Scenario: Empty source history activates without a price
- **WHEN** a registered source has an existing empty or comment-only history file
- **THEN** that source is active without an initial observation

#### Scenario: Missing source history leaves only that source inactive
- **WHEN** one registered source history is missing while another source for the same pair has an existing history
- **THEN** the missing source is not started, the existing source remains active, and no missing path is created

#### Scenario: Missing history warning identifies the source
- **WHEN** a registered source's expected history file is absent
- **THEN** the ticker logs a warning containing its trading pair, source name, and complete expected UUID-based path

#### Scenario: Malformed source history is rejected clearly
- **WHEN** comment text is removed and a non-empty data line in an active source history lacks a valid UTC timestamp and price
- **THEN** ticker startup fails with an error containing the history filename, one-based physical line number, and identifying invalid content

#### Scenario: Legacy history is not migrated
- **WHEN** only `data/assetaz/ticker/EVE_USDC.prices` exists
- **THEN** the ticker does not read, move, copy, or create a replacement for that legacy file

### Requirement: History uses the human-editable observation format
Each source history data line SHALL retain the form `<UTC timestamp>:<price>`, where the timestamp uses `uuuuMMddHHmmssSSS'Z'` with millisecond precision in UTC. Trading-pair and source identity SHALL come from the registered source and its directory context and SHALL NOT be stored in individual data lines. Automatic writes SHALL append plain data lines only and SHALL leave all existing comments and blank lines untouched.

#### Scenario: Observation is written in the required format
- **WHEN** any source observation at `2026-08-05T13:15:42.783Z` with price `14.85` is persisted
- **THEN** the appended line is `20260805131542783Z:14.85`

#### Scenario: Identity is not duplicated in data lines
- **WHEN** an observation is appended to a source history
- **THEN** its data line contains neither source name nor trading-pair identity

#### Scenario: Existing operator annotations are preserved
- **WHEN** the ticker appends an observation to a source history containing comments or blank lines
- **THEN** those existing lines remain unchanged and the appended observation contains neither a comment nor extra annotation

### Requirement: New observations are durable before publication
Only an active registered source SHALL be allowed to announce an observation. For each accepted announcement, the ticker SHALL validate the source, typed price, and timestamp; construct an observation with the registered trading pair and source name; serialize writes to that source history; append and force the data to persistent storage; and only after persistence succeeds consider it for the trading pair's in-memory latest value. Concurrent callbacks from different sources SHALL neither corrupt nor interleave writes, and readers SHALL never observe an unpersisted price. Every accepted observation SHALL be persisted even when an existing observation has a later timestamp, and latest SHALL change only when the newly persisted timestamp is later. If persistence fails, the ticker SHALL report the trading pair, source, and path clearly and preserve the prior latest observation.

#### Scenario: Active registered source observation is persisted
- **WHEN** an active registered source announces a valid price and timestamp
- **THEN** the ticker appends and forces the observation to that source's history before considering it for latest

#### Scenario: Inactive or unregistered source callback is rejected
- **WHEN** an inactive or unregistered source announces an observation
- **THEN** the ticker rejects the callback without persisting or publishing it

#### Scenario: Future-dated observation remains latest
- **WHEN** an accepted observation is persisted with a timestamp earlier than the current latest observation across active sources
- **THEN** the accepted observation remains in its source history and the current later observation remains latest

#### Scenario: Concurrent source callbacks remain durable
- **WHEN** active sources announce observations concurrently
- **THEN** each successful append is a complete non-interleaved history line and latest identifies the greatest successfully persisted timestamp

#### Scenario: Persistence fails
- **WHEN** appending or forcing an accepted observation fails
- **THEN** the failure identifies the trading pair, source, and path, the attempted observation is not published, and the previously persisted latest observation remains available
