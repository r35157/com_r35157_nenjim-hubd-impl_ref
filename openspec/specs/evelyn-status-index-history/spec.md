# evelyn-status-index-history Specification

## Purpose

Provide typed process-local Evelyn status measurements and visualize the currently implemented live indexes over time in Mission Control while preserving the six-index model for future work.

## Requirements

### Requirement: Evelyn calculates the live price index
Evelyn SHALL obtain the actual EVE/USDT price from the latest AssetAZ Ticker observation for the canonical EVE/USDT trading pair. It SHALL treat USDT as USD for this calculation and SHALL NOT access Raydium directly. At sampling instant t, on or after 2026-07-31T22:00:00Z, the expected price SHALL be 15 × 1.20^(elapsedSeconds / 31,536,000) and the Evelyn Price Index SHALL be ((actualPrice / expectedPrice) - 1) × 10, calculated with adequate decimal precision and without arbitrary history rounding.

#### Scenario: Calculate expected price at the configured start
- **WHEN** the sampling instant is 2026-07-31T22:00:00Z
- **THEN** the expected EVE price is exactly 15 USD

#### Scenario: Calculate expected price after one year
- **WHEN** exactly 31,536,000 seconds have elapsed since the configured start
- **THEN** the expected EVE price is exactly 18 USD

#### Scenario: Calculate representative fractional-year growth
- **WHEN** 18 days, 3 hours, 8 minutes, and 14 seconds have elapsed
- **THEN** the expected EVE price is approximately 15.136464436276 USD

#### Scenario: Calculate index from actual price
- **WHEN** actual price equals expected price, is 10 percent above, is 10 percent below, is double, or is half the expected price
- **THEN** the respective index is 0, 1, -1, 10, or -5

### Requirement: Evelyn instances have safe unique persistent names
Every Evelyn instance SHALL have an NFC-normalized Unicode name whose preserved spelling and capitalization selects the direct child directory data/evelyn/<name>. A name SHALL be rejected if it is empty or whitespace-only, has leading or trailing whitespace, is . or .., ends with a period, contains a control character or any of < > : " / \ | ? *, is a case-insensitive Windows-reserved filename including a reserved base with an extension, or normalizes as a path to anything other than one direct child of data/evelyn. Within one JVM, normalized names SHALL be reserved atomically and compared case-insensitively using locale-independent rules.

#### Scenario: A portable Unicode name is accepted
- **WHEN** an instance is constructed with a valid Unicode name containing Danish characters
- **THEN** NFC normalization is applied and the normalized spelling and capitalization identify its direct child data directory

#### Scenario: An unsafe name is rejected without reservation
- **WHEN** construction receives an invalid name or any other invalid constructor argument
- **THEN** construction fails before reserving a persistent identity

#### Scenario: Concurrent equivalent names conflict
- **WHEN** two constructor calls in one JVM concurrently request names that are equal after NFC normalization and locale-independent case folding
- **THEN** exactly one reserves the persistent identity and the other fails

### Requirement: Evelyn distinguishes restartable stop from permanent close
Stopping Evelyn SHALL terminate sampling, clear memory, preserve its persistent file, and retain its name reservation so the same object can restart. Evelyn SHALL support permanent, idempotent close; closing an active instance SHALL first stop it safely, a successfully closed instance SHALL never start again, and its name SHALL be released only after its sampling thread has definitely terminated. A termination failure SHALL be reported and SHALL retain the reservation.

#### Scenario: A stopped instance restarts
- **WHEN** a stopped but not closed Evelyn instance is started again
- **THEN** it retains exclusive ownership of the same name and reloads the complete persisted history before sampling

#### Scenario: An active instance closes successfully
- **WHEN** close is called on an active instance and its sampling thread terminates
- **THEN** memory is cleared, the name reservation is released, and later start calls fail

#### Scenario: Close cannot confirm termination
- **WHEN** close cannot confirm that the sampling thread terminated
- **THEN** close reports failure and retains the name reservation

#### Scenario: Close is repeated
- **WHEN** close is called after the instance was successfully closed
- **THEN** it completes without changing state or failing

### Requirement: Evelyn uses externally provisioned per-instance storage
Each named instance SHALL use the existing regular file data/evelyn/<normalized-name>/status.log. Before every start, Evelyn SHALL require the direct instance path to be an existing readable and writable directory and status.log to be an existing readable and writable regular file. Evelyn SHALL NOT create, replace, repair, truncate, rename, or otherwise provision either path. Any validation or actual open failure SHALL identify the affected path, leave memory unpublished, and prevent sampling from starting.

#### Scenario: Required storage is absent or unsuitable
- **WHEN** the instance directory or status file is missing, the path has the wrong type, or required access is unavailable
- **THEN** start fails with the affected path and reason without modifying storage, publishing history, or starting sampling

#### Scenario: Storage changes while stopped
- **WHEN** storage becomes invalid after stop and the same instance is started again
- **THEN** the complete startup validation is repeated and restart fails without modifying storage

### Requirement: Evelyn validates status history format strictly
The UTF-8 status.log format SHALL have supported version 1, declared by FORMAT_VERSION=1 as the first actual entry; blank lines and full-line comments MAY precede it when loading, while an externally provisioned new file SHALL place it on the physical first line. Missing, duplicate, malformed, misplaced, or unsupported declarations SHALL fail loading before record parsing. After the declaration, blank lines and comments SHALL be ignored, inline comments MAY follow data, and every other entry SHALL be a valid record with an explicit stable numeric type code.

Type 1 SHALL encode <UTC uuuuMMddHHmmssSSS'Z' timestamp>:1:<plain BigDecimal Evelyn Price Index>. It SHALL have exactly three fields. Unknown types, invalid timestamps or decimals, missing or surplus fields, and decreasing timestamps SHALL fail the complete load; equal timestamps SHALL be accepted. A valid declaration with no records SHALL represent empty history. Applicable failures SHALL identify the file path, one-based line number, and original line, and Evelyn SHALL neither skip, sort, partially publish, nor repair invalid content.

#### Scenario: A valid versioned history loads
- **WHEN** the file has a valid first actual version declaration and valid type-1 records in non-decreasing timestamp order
- **THEN** Evelyn restores every record in file order with zero values for the five unimplemented indexes

#### Scenario: The format declaration is invalid
- **WHEN** the declaration is missing, duplicated, malformed, misplaced, or not version 1
- **THEN** startup fails before parsing records and reports useful file diagnostics

#### Scenario: A record is invalid
- **WHEN** a record has an unknown type, malformed field, wrong field count, or timestamp earlier than its predecessor
- **THEN** startup fails with path, line number, and original line without exposing partial history or altering the file

### Requirement: Evelyn samples without overlapping or catch-up attempts
Each started Evelyn environment SHALL own its sampling executor, attempt its first sample only after complete storage validation and history restoration, and make subsequent attempts approximately one minute after the preceding attempt completes. Sampling SHALL remain active for the Evelyn lifecycle independently of whether Mission Control is open. Evelyn SHALL add exactly one timestamped point per successful attempt and SHALL isolate a failed or temporarily unavailable Ticker lookup or persistence operation without adding a fake or unpersisted point, terminating Evelyn, or cancelling the next attempt. Stopping Evelyn SHALL terminate its sampling executor.

#### Scenario: Initial Ticker data is unavailable
- **WHEN** the immediate sampling attempt cannot obtain a latest EVE/USDT price
- **THEN** no point is persisted or published, diagnostic context is logged, and the next fixed-delay attempt remains scheduled

#### Scenario: A later sample succeeds
- **WHEN** a later attempt obtains a latest EVE/USDT price whose millisecond timestamp is not earlier than the last persisted timestamp
- **THEN** exactly one type-1 record is durably appended before the corresponding point becomes visible in memory

#### Scenario: Persistence fails for one sample
- **WHEN** appending or durably flushing a calculated point fails
- **THEN** the point is not published, instance and path context are logged, and the next fixed-delay attempt remains scheduled

#### Scenario: A sampled timestamp goes backwards
- **WHEN** a newly sampled timestamp is earlier than the last persisted timestamp
- **THEN** no record or point is added, the failure is logged, and later attempts remain scheduled

### Requirement: Evelyn provides historical status-index measurements
Each named Evelyn instance SHALL publish a safe, non-null, oldest-to-newest snapshot only after its complete persisted history has been validated and loaded. Each type-1 point SHALL contain its persisted or sampled timestamp, the Evelyn Price Index, and BigDecimal.ZERO placeholders for EVE_SYRUP Pool Depth Index, EVE_SYRUP Pool Balance Index, AAZDKK_USDT Pool Balance Index, AAZDKK_USDT Pool Price Index, and AAZDKK_USDT Pool Depth Index. Production and Test SHALL use independently named files, history collections, reservations, and lifecycles. A sampled point SHALL be durably appended using robust line-boundary handling before publication. Stop SHALL clear only memory; restart SHALL restore the complete file, and close SHALL preserve the file.

#### Scenario: Status-index history is requested during sampling
- **WHEN** a caller requests an Evelyn environment's status-index history while persisted points may be appended
- **THEN** it receives a safe snapshot containing only completely persisted points and all six typed fields

#### Scenario: A process starts two named Evelyn environments
- **WHEN** Production and Test Evelyn instances start with independently provisioned files
- **THEN** each restores and extends only its own complete history even if calculated values are identical

#### Scenario: Existing file lacks a final line separator
- **WHEN** a valid existing status file ends with a record but no line separator
- **THEN** the next durable append first supplies a line boundary and preserves both records as distinct entries

#### Scenario: An Evelyn instance stops and restarts
- **WHEN** a named instance is stopped and later restarted
- **THEN** its memory is cleared at stop and its complete unchanged persistent history is revalidated and restored before sampling resumes

### Requirement: Evelyn Mission Control visualizes status-index measurements
Evelyn Mission Control SHALL display separate live Production and Test Overview charts exclusively from their corresponding authoritative Evelyn snapshots and SHALL never read or manage persistence directly. Each chart SHALL contain one enabled series named Evelyn Price Index; the five future series and their typed fields SHALL remain available as disabled source scaffolding. Opening or reopening EMC SHALL immediately reconstruct each chart from the complete restored current snapshot before periodically refreshing it. Successful points SHALL be added to chart objects only on the JavaFX Application Thread while the window remains open. Timestamp bounds SHALL expand as points arrive, and only enabled Evelyn Price Index values SHALL determine a dynamic Y-axis range that is symmetric around and always displays zero, using a reasonable default range for empty or all-zero history. Hiding or closing EMC SHALL release only EMC's JavaFX refresh resources and SHALL NOT start, stop, close, persist, or clear either Evelyn environment.

#### Scenario: Production and Test Overviews show restored history
- **WHEN** Mission Control opens after two named Evelyn environments have restored their histories
- **THEN** it displays separate one-series charts immediately populated from the corresponding complete snapshots

#### Scenario: A successful point appears while EMC is open
- **WHEN** an Evelyn history publishes a newly persisted point
- **THEN** the corresponding chart adds it on the JavaFX Application Thread and updates its timestamp and symmetric price-index bounds without reopening EMC

#### Scenario: EMC reopens after collecting hidden-window samples
- **WHEN** the same EMC instance is reopened after Evelyn persisted additional points while its window was hidden
- **THEN** each chart is reconstructed immediately from its environment's complete current history without missing points because of stale rendering state

#### Scenario: Future fields contain larger values
- **WHEN** any disabled future-index field has a greater absolute value than Evelyn Price Index
- **THEN** that disabled value does not affect the visible Y-axis bounds
