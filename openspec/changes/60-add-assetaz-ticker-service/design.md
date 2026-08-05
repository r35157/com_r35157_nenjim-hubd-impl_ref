## Context

See `proposal.md` for motivation. `TickerService` and `TickerServiceImpl` already exist as empty shells in AssetAZ-owned packages. `AssetPrice` wraps `ΩPriceΩ` and `TradingPair`; the latter identifies currencies structurally rather than by raw symbol. The repository temporarily hosts AssetAZ code as Cauldron, and NenjimHub's current autorun list directly constructs services.

## Goals / Non-Goals

**Goals:** Establish a small reusable ticker contract, deterministic file-backed startup, atomic persist-before-publish behavior, and temporary autorun wiring while retaining AssetAZ ownership.

**Non-Goals:** Datasource plugins, real market data, pairs other than `EVE_USDC`, consumers, subscriptions/callbacks, history queries, retention, compaction, or automated tests.

## Decisions

- Define `PriceObservation` in `com.r35157.assetaz.core.service.ticker` exactly as a record of `AssetPrice price` and `Instant observedAt`. Extend `TickerService` with `start()` and `PriceObservation getLatestPrice(TradingPair tradingPair)`. Use clear exceptions for unsupported/inactive pairs and enabled pairs without a persisted observation; `Optional` and `null` were rejected because issue #60 explicitly requires exceptions for unavailable prices. Raw symbol strings were rejected because callers must use the established ValueTypes.
- Add stable EVE and USDC currency definitions and an `EVE_USDC` well-known `TradingPair`, then construct the sample price with `new ΩPriceΩ("14.85")`. This keeps identity consistent with other typed prices instead of parsing pair names at the API boundary.
- Keep the public API and `PriceObservation` in the AssetAZ API package and implementation details in `com.r35157.assetaz.core.service.ticker.impl.ref`. Their current repository location is temporary Cauldron placement and does not imply Evelyn or NenjimHub ownership.
- Use the fixed path `data/assetaz/ticker/EVE_USDC.prices`; do not create its directory or file. Check existence when initializing the pair. Missing means inactive, emits a warning containing the pair and expected path, and does not prevent the rest of Ticker startup; an existing zero-length, blank-only, or comment-only file is active with no latest value.
- Store one observation per data line as `<uuuuMMddHHmmssSSS'Z'>:<decimal-price>`, using a strict formatter fixed to UTC. Read line by line, remove text from the first `#`, trim, and skip empty results. Split the remaining data at the required colon and parse it into `Instant`, `ΩPriceΩ`, and the fixed `EVE_USDC` pair. Wrap any structural, timestamp, or price failure with the filename, one-based physical line number, and identifying invalid content.
- Load history synchronously during `start()` and select the parsed observation with the greatest `observedAt` timestamp rather than assuming the file is chronologically ordered. After loading, generate the first hardcoded observation immediately, then use a single-thread scheduled executor with a one-minute interval for subsequent observations. Do not schedule generation for an inactive pair. The scheduler is an implementation mechanism, not a public lifecycle or subscription API.
- Serialize generation and publication, and keep the published latest value behind a thread-safe reference so concurrent callers see either the prior persisted observation or the new persisted observation. Validate and encode the complete line, append all encoded bytes through a `FileChannel`, call `force(true)`, and only then replace the in-memory latest reference. A buffered-writer `flush()` alone was rejected because it does not satisfy the issue's explicit flush/force-to-persistence ordering. On open, write, or force failure, log/report the failure and retain the prior latest value; never publish the attempted observation.
- Add a temporary `startAssetAZTickerService()` entry to `NenjimHubImpl` following its direct autorun pattern. It constructs and starts `TickerServiceImpl`; no Ticker dependency is passed to Evelyn, EMC, alarms, portfolios, or other services.

## Risks / Trade-offs

- [Filesystem or device guarantees can still limit crash durability after `FileChannel.force(true)`] → Force each complete appended record before publication and document that the implementation uses the strongest standard Java file-channel guarantee available here.
- [Fixed-rate execution can bunch runs after a long pause] → The single writer preserves ordering; richer scheduling policy is outside this bootstrap.
- [Malformed history prevents the service from starting] → Include exact filename and line number so operators can repair the activation file safely.
- [Hardcoded identifiers and price are temporary] → Isolate them behind well-known ValueTypes and the reference implementation for later source replacement.

## Migration Plan

Operators opt in by creating `data/assetaz/ticker/EVE_USDC.prices`, optionally pre-populated in the documented format. Deploy the service and temporary autorun wiring together. Rollback removes the autorun call and ticker implementation while leaving the append-only data file intact.
