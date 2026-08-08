## 1. Evelyn Calculation and History

- [x] 1.1 Add the deterministic expected-price and Evelyn Price Index calculator with explicit precision and configured start instant.
- [x] 1.2 Replace hardcoded history with an empty thread-safe per-instance snapshot history and preserve all six typed fields using zero placeholders.
- [x] 1.3 Make restartable fixed-delay sampling and complete process-local history owned by each Evelyn lifecycle, with immediate first attempt, failure isolation, clean stop, and history discard on stop.

## 2. Live Mission Control Chart

- [x] 2.1 Refactor Production and Test Overview charts into separate live chart states that reconstruct the complete authoritative snapshot on every open and reconcile later snapshots on the JavaFX thread.
- [x] 2.2 Enable only Evelyn Price Index, retain the five future series and bound contributions as commented scaffolding, and calculate valid dynamic X and symmetric Y bounds from enabled data only.
- [x] 2.3 Keep EMC lifecycle presentation-only: hiding stops its JavaFX refresh, reopening rebuilds fresh chart state, and EMC never starts or stops Evelyn sampling.

## 3. Safe NenjimHub Composition

- [x] 3.1 Enable only CIS, Solana, Raydium, the EVE/USDT Raydium PriceSource, Ticker, two Evelyn environments, and EMC in the composition root.
- [x] 3.2 Start Ticker before constructing and independently starting both Evelyn lifecycles; pass public TickerService plus the canonical CIS-resolved EVE/USDT pair to each instance before starting EMC.
- [x] 3.3 Confirm HardcodedPriceSource and every unrelated alarm, trading, Composer, Process Manager, Test Tool, Soda Task Manager, and Suwimo service remain disabled.

## 4. Specification and Final Checks

- [x] 4.1 Update the canonical capability Purpose to stop claiming that all six indexes are currently visible.
- [x] 4.2 Compile main and test source sets without running unit tests; automated unit-test coverage is deliberately deferred.
- [x] 4.3 Run strict OpenSpec validation and `git diff --check`.
- [x] 4.4 Review the complete diff for unrelated changes, generic Ticker/Raydium changes, persistence changes, and accidentally enabled services.
