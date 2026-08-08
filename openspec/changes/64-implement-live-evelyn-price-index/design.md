## Context

See `proposal.md` for motivation. Evelyn currently returns hardcoded history and EMC copies it once while constructing six static series. The existing Ticker already supplies persisted latest EVE/USDT observations through its public API. The baseline deliberately disables all NenjimHub autorun services, so this change must explicitly reactivate only the dependency chain needed for the live chart.

## Goals / Non-Goals

**Goals:**

- Keep financial calculation, sampling, and process-local history in Evelyn-owned code.
- Keep JavaFX chart mutation and visualization lifecycle in EMC-owned code.
- Make formula and scheduler behavior deterministic using explicit instants, an injected clock, and injectable delay units.
- Preserve all six typed index fields while enabling only the price-index chart series.
- Retain the safety posture of the baseline composition root.

**Non-Goals:**

- Ticker/Raydium contract changes, index persistence/backfill, other five index calculations, conversions, configuration, streaming, trading, alarms, unrelated service activation, or automated unit-test coverage in this change.

## Decisions

### Separate the pure formula from sampling

An Evelyn-owned calculator accepts explicit `BigDecimal` prices and `Instant` values. It uses the Copenhagen start configuration resolved to the fixed instant `2026-07-31T22:00:00Z`, a 365-day seconds denominator, and an explicit high-precision `MathContext`. Fractional exponentiation is isolated behind the calculator rather than embedded in JavaFX or scheduler code. This makes all reference examples deterministic. Direct calculation in EMC was rejected because presentation must not own financial behavior.

### Give Evelyn an explicit restartable sampling lifecycle

The Evelyn API gains start and stop operations for status-index sampling. The reference implementation receives only public `TickerService`, the canonical expected trading pair, a clock, and delay configuration. Each instance owns a daemon single-thread scheduler using fixed delay with zero initial delay. Successful attempts append under a lock; snapshot reads use `List.copyOf` under the same lock. Exceptions are caught at the task boundary so future attempts survive. Stop is idempotent and releases the owned executor. A global scheduler or shared Production/Test history was rejected because environments and lifecycles must remain independent.

### Let EMC incrementally reconcile snapshots on the JavaFX thread

Each environment chart keeps its own enabled series and count of rendered snapshot points. A JavaFX `Timeline` periodically obtains safe Evelyn snapshots and appends unseen points on the JavaFX Application Thread, then recalculates explicit X/Y bounds. Empty and single-point histories receive non-degenerate timestamp bounds; Y bounds use only absolute Evelyn Price Index values and fall back to a symmetric default. The five existing series-add and bound-contribution statements remain commented scaffolding. Background observer callbacks were rejected because they complicate thread ownership and can mutate JavaFX objects unsafely.

### Couple the window and sampling lifecycle

EMC creates and shows both empty charts, starts each Evelyn sampler, and begins its JavaFX refresh timeline. Window close stops the timeline and both Evelyn samplers. Starting after window creation satisfies immediate-after-services-and-EMC behavior and allows an empty chart to update. This avoids scheduler leakage after the UI lifecycle ends.

### Enable only the required NenjimHub dependency chain

`NenjimHubImpl` creates one CIS, Solana implementation, Raydium implementation, the existing pool-specific EVE/USDT PriceSource, and Ticker; it starts Ticker before constructing two Evelyn instances with the public Ticker and canonical pair, then starts EMC. The hardcoded source, alarm, Composer, Process Manager, Test Tool, Soda Task Manager, Suwimo Client, and all trading behavior remain disabled. Existing-file-only Ticker activation remains unchanged.

## Risks / Trade-offs

- [Fractional exponentiation ultimately uses finite floating-point transcendental support] → isolate conversion and retain DECIMAL128 arithmetic around it; the deterministic seam remains available for later focused verification.
- [EMC refresh can observe several newly sampled points at once] → reconcile snapshots by rendered count on the JavaFX thread and preserve oldest-to-newest order.
- [Ticker has no active EVE/USDT history or has not completed its first poll] → log and skip the point; fixed-delay sampling retries without fabricating data.
- [Window close can race with a sample] → stop UI refresh first, then idempotently shut down both source-owned sampling executors.

## Migration Plan

Deploy with the existing manually activated EVE/USDT Ticker history file. On process start, histories are intentionally empty. Rollback restores the disabled autorun composition and static presentation; no data migration is needed because index history is never persisted.
