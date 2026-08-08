## Why

Evelyn Mission Control currently renders a static six-series chart from hardcoded data, so it does not describe Evelyn's live market-price balance. Evelyn needs to calculate and retain the first real status index from AssetAZ Ticker while EMC updates safely during the current process lifetime.

## What Changes

- Replace hardcoded Evelyn status-index points with separate, thread-safe, in-memory Production and Test histories sampled for the complete lifecycle of each started Evelyn instance.
- Calculate the expected exponential EVE price and Evelyn Price Index deterministically from an explicit sampling instant and the latest canonical EVE/USDT Ticker observation.
- Isolate temporary Ticker/sample failures so no fake point is created and later fixed-delay samples continue.
- Make EMC's Production and Test charts reconstruct the complete current Evelyn snapshots when opened or reopened and update live on the JavaFX Application Thread, with only Evelyn Price Index enabled and controlling symmetric bounds.
- Compose only CIS, Solana, Raydium, the real EVE/USDT PriceSource, Ticker, Evelyn, and EMC in NenjimHub; leave all unrelated and safety-sensitive services disabled.
- Keep calculation, timing, sampling-delay, and chart-update seams deterministic so focused automated verification can be added later; unit tests are deliberately deferred from this change.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `evelyn-status-index-history`: Replaces hardcoded six-line snapshots with live in-memory Evelyn Price Index sampling, formula, failure handling, separate environment histories, and one enabled live chart series.

## Impact

The change affects the Evelyn API/reference implementation, EMC chart lifecycle and rendering, and temporary NenjimHub composition. It depends on the existing public Ticker and canonical currency identities; generic Ticker and Raydium PriceSource contracts and persistence remain unchanged.
