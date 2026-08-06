## Why

The AssetAZ Ticker currently owns a hardcoded price generator, so acquisition, scheduling, persistence, and publication are coupled in one implementation. Separating acquisition behind registered price-source plugins makes the ticker reusable for polling, streaming, and future market integrations while retaining durable persist-before-publish behavior.

## What Changes

- Add `PriceSource` and functional `PriceSink` API contracts, source registration, and complete start/stop lifecycle management.
- Add the `ΩPriceSourceNameΩ` ValueTag and include stable source identity in each in-memory `PriceObservation`.
- **BREAKING** Replace the single history file with an independent UUID-based history path for each `(TradingPair, sourceName)` registration; the old file is not migrated automatically.
- Refactor `TickerServiceImpl` into a source-agnostic sink that validates callbacks, persists each source independently, and publishes the greatest successfully persisted timestamp across active sources for a pair.
- Move the existing `EVE_USDC = 14.85` generation and fixed-delay scheduler into a stoppable `HardcodedPriceSource` plugin.
- Update temporary NenjimHub startup wiring to construct, register, and start the hardcoded source through the new architecture.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `assetaz-ticker-service`: Generalize the ticker from one internal generator and history into registered source plugins with per-source activation, persistence, lifecycle, and cross-source latest selection.

## Impact

- Changes the AssetAZ ticker API and reference implementation under `com.r35157.assetaz.core.service.ticker`.
- Adds `PriceSourceName` to the shared Detag hierarchy under `String -> Name`; this configuration change is explicitly required by issue #61.
- Changes the operator-managed history location and requires manual movement of the old hardcoded history.
- Updates temporary wiring in `NenjimHubImpl` without integrating the ticker into any consumer.
- Adds no Raydium, WebSocket, history-query, retention, or automated-test functionality.
