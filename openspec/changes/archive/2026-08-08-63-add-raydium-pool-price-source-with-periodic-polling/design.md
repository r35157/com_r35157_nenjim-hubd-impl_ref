## Context

See `proposal.md` for motivation. Commit `2fc553b` established package ownership that anticipates later extraction into Nenjim modules while the code remains in one Gradle project. The existing Raydium Pool PriceSource is an unimplemented scaffold in its intended implementation package. The Ticker already activates registered sources only when their exact history file exists and persists callbacks before updating its in-memory latest observation.

## Goals / Non-Goals

**Goals:**

- Complete the existing pool-source scaffold as an independently constructible, restartable PriceSource.
- Preserve the package and future artifact dependency direction introduced by `2fc553b`.
- Compose one EVE/USDT pool source beside the existing hardcoded EVE/USDC source using today's temporary Cauldron wiring.
- Make timing deterministic enough for verification through package-private clock and delay injection.

**Non-Goals:**

- Dynamic Nenjim discovery, configuration files, streaming, backoff, health APIs, history creation or migration, visualization, currency conversion, or further physical module splitting.
- Changes to the generic Ticker behavior or canonical Ticker specification.

## Decisions

### Preserve future module and package ownership

The future boundaries remain:

```text
Ticker API
  com.r35157.assetaz.services.ticker
  - TickerService
  - PriceObservation

PriceSource plugin API
  com.r35157.assetaz.services.ticker.plugins.pricesource
  - PriceSource
  - PriceSink

Ticker reference implementation
  com.r35157.assetaz.services.ticker.impl.ref
  -> Ticker API
  -> PriceSource plugin API

Hardcoded PriceSource implementation
  com.r35157.assetaz.services.ticker.plugins.pricesource.impl.hardcoded
  -> PriceSource plugin API

Raydium Pool PriceSource implementation
  com.r35157.assetaz.services.ticker.plugins.pricesource.impl.raydiumpool
  -> PriceSource plugin API
  -> Raydium API and value types
```

`PriceSink` stays in the plugin API so implementations do not depend on the Ticker API or reference implementation. `TickerServiceImpl` implements `PriceSink`, owns source lifecycle, and temporarily receives sources through its varargs constructor. `NenjimHubImpl` remains the temporary composition root; dynamic discovery is deferred. The alternative of moving the sink or source packages would recreate future circular or implementation dependencies and is explicitly rejected.

### Use a source-owned single-thread fixed-delay scheduler

Each source creates a daemon single-thread scheduled executor when started. The immediate attempt is submitted to that executor with zero initial delay, followed by fixed-delay scheduling so a slow or paused call cannot produce catch-up observations. A package-private constructor injects `Clock`, delay, and time unit; the public constructor fixes UTC and one minute. A shared scheduler was rejected because lifecycle and termination must belong to the source instance.

### Contain each attempt at the scheduler boundary

One safe polling method catches retrieval, validation, and sink-publication failures, logs pool and pair context, and returns normally so fixed-delay scheduling continues. Pair mismatch is treated as an invalid observation rather than a fatal lifecycle error. The timestamp is read only after a successful matching response and truncated to milliseconds before the sink callback.

### Keep startup and shutdown state synchronized

Startup checks the running state before validating or assigning the new sink, creates fresh scheduling resources, and schedules the immediate task. Stop detaches the scheduler under synchronization, requests immediate shutdown, and waits for termination without holding the lifecycle monitor. Interrupted waiting restores the caller's interrupt flag and ensures shutdown remains requested. This avoids replacing an active sink and permits restart after resource termination.

### Resolve EVE/USDT through the shared Currency Identity Service

The API identifier class gains only a stable USDT UUID. The hardcoded catalogue owns canonical metadata and the official Solana mint mapping. NenjimHub constructs the expected pair by resolving EVE and USDT through its single shared CIS instance, constructs Raydium with that same CIS dependency, and supplies both hardcoded and Raydium sources to the temporary Ticker constructor. The pool ID is market/source identity and is not registered in CIS.

## Risks / Trade-offs

- [A blocking Raydium call can delay shutdown until the call honors interruption] → request immediate shutdown, preserve interruption, and keep the polling thread daemonized.
- [A missing manually provisioned history silently leaves the new source inactive] → retain the existing explicit warning and document the exact pool-based history path.
- [Temporary constructor injection can be mistaken for permanent discovery] → document it as Cauldron wiring and preserve interfaces and packages for later Nenjim discovery.
- [A response may reverse or otherwise change the configured pair] → validate exact pair equality and publish nothing on mismatch.

## Migration Plan

Add USDT identity data, complete the existing source, and extend only the NenjimHub composition graph. Deployment requires manually creating the exact empty EVE/USDT pool history file to activate polling. Rollback removes that file or removes the temporary source wiring; existing hardcoded EVE/USDC history is unchanged.
