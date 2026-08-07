## Why

The AssetAZ Ticker currently owns a hardcoded price generator, so acquisition, scheduling, persistence, and publication are coupled in one implementation. Nenjim's future component model constructs components independently and lets a component obtain peer plugins from its context after construction, so price sources must be independently constructible and wired to the ticker only when the ticker starts them.

## What Changes

- Add `PriceSource` and functional `PriceSink` API contracts in which independently constructed sources receive their sink through `start(PriceSink)` and own their acquisition resources.
- Keep plugin discovery and lifecycle inside `TickerServiceImpl`: it obtains `PriceSource` implementations from its Nenjim context, starts them with itself as sink, and stops them. Ordinary `TickerService` clients receive no plugin-management API.
- Add the `ΩPriceSourceNameΩ` ValueTag and include stable source identity in each in-memory `PriceObservation`.
- **BREAKING** Replace the single history file with an independent UUID-based history path for each obtained `(TradingPair, sourceName)` source identity; the old file is not migrated automatically.
- Refactor `TickerServiceImpl` into a source-agnostic sink that validates callbacks, persists each source independently, and publishes the greatest successfully persisted timestamp across active sources for a pair.
- Move the existing `EVE_USDC = 14.85` generation and fixed-delay scheduler into a stoppable `HardcodedPriceSource` plugin.
- Use a temporary `TickerServiceImpl(PriceSource...)` Cauldron constructor to simulate the future Nenjim context lookup while constructing `HardcodedPriceSource` independently.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `assetaz-ticker-service`: Generalize the ticker from one internal generator and history into context-provided source plugins with per-source activation, persistence, lifecycle, and cross-source latest selection.

## Impact

- Changes the AssetAZ ticker plugin API and reference implementation under `com.r35157.assetaz.core.service.ticker`; the ordinary `TickerService` API exposes lifecycle and price queries, not plugin registration.
- Adds `PriceSourceName` to the shared Detag hierarchy under `String -> Name`; this configuration change is explicitly required by issue #61.
- Changes the operator-managed history location and requires manual movement of the old hardcoded history.
- Updates temporary wiring in `NenjimHubImpl` to construct the source and ticker independently before injecting the source list as a stand-in for future context discovery.
- Adds no Raydium, WebSocket, history-query, retention, or automated-test functionality.
