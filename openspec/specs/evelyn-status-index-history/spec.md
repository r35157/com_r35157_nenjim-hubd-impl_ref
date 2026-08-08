# evelyn-status-index-history Specification

## Purpose

Provide typed process-local Evelyn status measurements and visualize the currently implemented live indexes over time in Mission Control while preserving the six-index model for future work.

## Requirements

### Requirement: Evelyn calculates the live price index
Evelyn SHALL obtain the actual EVE/USDT price from the latest AssetAZ Ticker observation for the canonical EVE/USDT trading pair. It SHALL treat USDT as USD for this calculation and SHALL NOT access Raydium directly. At sampling instant `t`, on or after `2026-07-31T22:00:00Z`, the expected price SHALL be `15 × 1.20^(elapsedSeconds / 31,536,000)` and the Evelyn Price Index SHALL be `((actualPrice / expectedPrice) - 1) × 10`, calculated with adequate decimal precision and without arbitrary history rounding.

#### Scenario: Calculate expected price at the configured start
- **WHEN** the sampling instant is `2026-07-31T22:00:00Z`
- **THEN** the expected EVE price is exactly 15 USD

#### Scenario: Calculate expected price after one year
- **WHEN** exactly 31,536,000 seconds have elapsed since the configured start
- **THEN** the expected EVE price is exactly 18 USD

#### Scenario: Calculate representative fractional-year growth
- **WHEN** 18 days, 3 hours, 8 minutes, and 14 seconds have elapsed
- **THEN** the expected EVE price is approximately `15.136464436276` USD

#### Scenario: Calculate index from actual price
- **WHEN** actual price equals expected price, is 10 percent above, is 10 percent below, is double, or is half the expected price
- **THEN** the respective index is 0, 1, -1, 10, or -5

### Requirement: Evelyn samples without overlapping or catch-up attempts
Each started Evelyn environment SHALL own its sampling executor, attempt its first sample immediately, and make subsequent attempts approximately one minute after the preceding attempt completes. Sampling SHALL remain active for the Evelyn lifecycle independently of whether Mission Control is open. Evelyn SHALL add exactly one timestamped point per successful attempt and SHALL isolate a failed or temporarily unavailable Ticker lookup without adding a fake point, terminating Evelyn, or cancelling the next attempt. Stopping Evelyn SHALL terminate its sampling executor.

#### Scenario: Initial Ticker data is unavailable
- **WHEN** the immediate sampling attempt cannot obtain a latest EVE/USDT price
- **THEN** no point is added, diagnostic context is logged, and the next fixed-delay attempt remains scheduled

#### Scenario: A later sample succeeds
- **WHEN** a later attempt obtains a latest EVE/USDT price
- **THEN** exactly one point is appended using the sampling instant for both expected-price calculation and point timestamp

### Requirement: Evelyn provides historical status-index measurements
Each Evelyn instance SHALL begin with an empty, process-local in-memory history and expose a safe, non-null snapshot of typed measurement points ordered from oldest to newest while sampling may continue concurrently. Each successful point SHALL contain its sampling timestamp, the calculated Evelyn Price Index, and `BigDecimal.ZERO` placeholders for EVE_SYRUP Pool Depth Index, EVE_SYRUP Pool Balance Index, AAZDKK_USDT Pool Balance Index, AAZDKK_USDT Pool Price Index, and AAZDKK_USDT Pool Depth Index. Production and Test SHALL use separate history collections, and no point SHALL be persisted, restored, or backfilled.

#### Scenario: Status-index history is requested during sampling
- **WHEN** a caller requests an Evelyn environment's status-index history while points may be appended
- **THEN** it receives a safe, non-null, oldest-to-newest snapshot containing all six typed fields

#### Scenario: A process starts two Evelyn environments
- **WHEN** Production and Test Evelyn instances are created for a new process
- **THEN** both histories start empty and remain separate even if their calculated values are identical

#### Scenario: A real point is sampled
- **WHEN** Evelyn appends a successful price-index point
- **THEN** its Evelyn Price Index contains the calculated value and each of the other five fields contains zero

#### Scenario: An Evelyn instance is stopped
- **WHEN** a Production or Test Evelyn instance is stopped
- **THEN** its sampling executor terminates and its complete process-local history is discarded without affecting the other environment

### Requirement: Evelyn Mission Control visualizes status-index measurements
Evelyn Mission Control SHALL display separate live Production and Test Overview charts from their corresponding authoritative Evelyn snapshots. Each chart SHALL contain one enabled series named `Evelyn Price Index`; the five future series and their typed fields SHALL remain available as disabled source scaffolding. Opening or reopening EMC SHALL immediately reconstruct each chart from the complete current snapshot before periodically refreshing it. Successful points SHALL be added to chart objects only on the JavaFX Application Thread while the window remains open. Timestamp bounds SHALL expand as points arrive, and only enabled Evelyn Price Index values SHALL determine a dynamic Y-axis range that is symmetric around and always displays zero, using a reasonable default range for empty or all-zero history. Hiding or closing EMC SHALL release only EMC's JavaFX refresh resources and SHALL NOT start, stop, or clear either Evelyn environment.

#### Scenario: Production and Test Overviews start empty
- **WHEN** Mission Control opens with two newly created Evelyn environments
- **THEN** it displays two separate empty one-series charts with valid timestamp axes and symmetric Y-axes containing zero

#### Scenario: A successful point appears while EMC is open
- **WHEN** an Evelyn history receives a new successful point
- **THEN** the corresponding chart adds it on the JavaFX Application Thread and updates its timestamp and symmetric price-index bounds without reopening EMC

#### Scenario: EMC reopens after collecting hidden-window samples
- **WHEN** the same EMC instance is reopened after Evelyn collected additional points while its window was hidden
- **THEN** each chart is reconstructed immediately from its environment's complete current history without missing points because of stale rendering state

#### Scenario: Future fields contain larger values
- **WHEN** any disabled future-index field has a greater absolute value than Evelyn Price Index
- **THEN** that disabled value does not affect the visible Y-axis bounds
