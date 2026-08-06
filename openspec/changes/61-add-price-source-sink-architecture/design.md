## Context

See `proposal.md` for motivation and the `assetaz-ticker-service` delta for behavior. The current reference ticker combines one hardcoded generator, a scheduler, one fixed history path, persistence, and latest publication. Issue #61 requires plugin-style acquisition while retaining the existing typed ValueTypes, durable append format, and temporary Cauldron placement.

## Goals / Non-Goals

**Goals:** Keep acquisition resources owned by sources; make the ticker a source-agnostic registry and durable sink; isolate histories by stable source identity; preserve greatest-timestamp selection and persist-before-publish under concurrent callbacks; provide deterministic lifecycle transitions.

**Non-Goals:** Runtime source discovery, dynamic registration after startup, automatic migration, source health/retry policy, Raydium or streaming integrations, public history queries, consumers, or automated tests.

## Decisions

- Add API-package `PriceSource` and functional `PriceSink` interfaces. `TickerService` gains `addPriceSource` and `stop`, but does not extend `PriceSink`; concrete plugins receive only a sink reference. This prevents plugins from depending on registry/query operations and leaves acquisition strategy entirely source-owned.
- Add `PriceSourceName` beneath `String -> Name` in `conf/detag.conf`, as explicitly authorized by issue #61. Store source identity in `PriceObservation` but not in history lines, because the source directory is the persistent identity and repeated line metadata would create disagreement risks.
- Represent a registration internally by the source object, its `(TradingPair, sourceName)` key, derived path, activation state, and a per-history lock. Reject duplicate keys, unsafe names, and registration after startup. Compare callbacks by registered source object identity as well as stable metadata so an unregistered impersonating instance cannot address another source's history.
- Centralize paths under the ticker data root using canonical base UUID, canonical quote UUID, validated source name, and sanitized symbols in the final filename. Source names are rejected rather than rewritten because they are technical persistent identity. Symbols are non-identity display text and are converted character-by-character to safe filename components, rejecting an empty result.
- During `start()`, validate and derive every registration, check exact file existence without scanning or creating, load all active histories into local state, compute per-pair maximum timestamps, then commit startup state and start active sources. If loading or starting fails, stop every source already started and leave the ticker stopped rather than expose a partially started lifecycle.
- During `stop()`, prevent new callbacks, stop every started source, and aggregate/report lifecycle failures while ensuring every source receives a stop attempt. Sources can be registered before a subsequent restart only if the implementation returns to the pre-start state; dynamic registration while starting, started, or stopping remains rejected.
- Move the hardcoded price, clock, one-minute delay, and scheduled executor into `HardcodedPriceSource`. It announces immediately in `start()` and schedules subsequent attempts with `scheduleWithFixedDelay`; `stop()` shuts down its executor. The ticker contains none of those acquisition constants or resources.
- On `announce`, resolve the exact active registration, validate non-null price and timestamp, construct `AssetPrice` and `PriceObservation`, lock that source history, append the complete UTF-8 line with `FileChannel`, and call `force(true)`. Only afterward atomically update the pair's latest value when the new timestamp is greater. Per-history locks allow independent sources to persist concurrently without interleaving, while atomic per-pair maximum updates preserve cross-source ordering.
- Keep strict UTC parsing and the existing comment behavior. Loaded observations receive trading pair and source name from the registration context. The old flat history path is deliberately ignored; operator-controlled manual movement is the only migration.
- Temporary NenjimHub wiring constructs one ticker, passes it as `PriceSink` to one `HardcodedPriceSource`, registers the source, then starts the ticker. This wires a reference implementation without transferring AssetAZ ownership or adding consumers.

## Risks / Trade-offs

- [A source throws during startup after earlier sources started] → Stop all sources that were successfully started, suppress secondary stop failures, and fail ticker startup clearly.
- [A source callback races with stop] → Guard lifecycle and active-registration checks so callbacks are accepted only while the ticker is started; source stop is responsible for terminating its own producer resources.
- [Multiple source callbacks contend for one trading pair] → Use per-history serialization for files and an atomic greatest-timestamp update for the shared pair view.
- [Symbol sanitization can make filenames less recognizable] → Preserve UUIDs as technical identity and use a deterministic safe replacement only for the human-readable leaf filename.
- [Manual migration can temporarily deactivate the hardcoded source] → Log the exact expected new path and never create or infer content, making operator action explicit and reversible.

## Migration Plan

Before deployment, stop the old service and move `data/assetaz/ticker/EVE_USDC.prices` to `data/assetaz/ticker/019c3f9f-41d1-7a73-b1df-d4c11c7ff301/019c3f9f-41d1-7a73-b1df-d4c11c7ff302/Hardcoded/EVE_USDC.prices`, creating parent directories only as an operator action. Deploy the API, ticker, hardcoded source, Detag configuration, and wiring together. Rollback restores the previous code and moves the file back to the legacy flat path; no automatic data transformation is involved.
