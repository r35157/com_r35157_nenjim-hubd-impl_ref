## Why

Evelyn currently discards every collected status-index point when it stops, so process restarts lose operational history. Each independently named Evelyn environment needs durable, operator-provisioned append-only storage while Evelyn—not Mission Control—continues to own collection, validation, and publication.

## What Changes

- **BREAKING** Require every `EvelynImpl` to have a validated, NFC-normalized instance name that selects its permanent `data/evelyn/<name>/status.log` identity.
- Reserve normalized instance names atomically and case-insensitively within the JVM until permanent, idempotent close.
- **BREAKING** Extend Evelyn lifecycle with permanent close in addition to restartable start/stop.
- Strictly validate externally provisioned storage, format version 1, every record, and non-decreasing timestamps before publishing restored history or starting sampling.
- Durably append each sampled type-1 status record before publishing it in memory, while isolating individual append failures.
- Keep Production and Test storage, lifecycle, and history independent and keep EMC presentation-only.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `evelyn-status-index-history`: Replace process-local disposable history with named-instance storage, strict restore validation, persist-before-publish behavior, and distinct restartable stop versus permanent close semantics.

## Impact

The change affects the public Evelyn lifecycle, `EvelynImpl` construction and persistence, and temporary Production/Test composition in `NenjimHubImpl`. Operators must provision each instance directory and versioned `status.log` before startup. EMC, AssetAZ Ticker, Raydium PriceSource, generated Detag sources, and unrelated services remain behaviorally unchanged.
