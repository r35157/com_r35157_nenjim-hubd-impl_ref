## 1. Ticker Source API

- [x] 1.1 Add `PriceSourceName` under `String -> Name` in `conf/detag.conf` and retain required generated-type imports in `.tjava` sources.
- [x] 1.2 Add the `PriceSource` and functional `PriceSink` API contracts with typed metadata and `PriceSource.start(PriceSink)` lifecycle wiring.
- [x] 1.3 Extend `PriceObservation` with source identity and extend `TickerService` with stop lifecycle while keeping plugin management out of the public service API.

## 2. Source Registry and Persistence

- [x] 2.1 Implement internal handling of context-provided sources with identity validation, duplicate rejection, lifecycle state checks, and exact-instance callback validation in `TickerServiceImpl`.
- [x] 2.2 Implement centralized UUID/source/safe-symbol history paths without scanning, creating, or migrating filesystem content.
- [x] 2.3 Load active per-source histories with strict parsing, comments, source-context observations, clear malformed-line errors, and greatest-timestamp restoration across sources.
- [x] 2.4 Persist concurrent active-source announcements to independent histories with complete-line serialization and `FileChannel.force(true)` before atomic greatest-timestamp publication.
- [x] 2.5 Implement ticker start/stop orchestration so only active sources receive `start(this)`, every started source receives a stop attempt, and partial startup is rolled back.

## 3. Hardcoded Source and Wiring

- [x] 3.1 Add an independently constructible, stoppable `HardcodedPriceSource` whose `start(PriceSink)` preserves an existing sink on repeated start, announces `EVE_USDC = 14.85` immediately, and owns one-minute fixed-delay scheduling.
- [x] 3.2 Update NenjimHub temporary autorun wiring to construct the hardcoded source independently and pass it through `TickerServiceImpl(PriceSource...)` as a Cauldron simulation of future context discovery.

## 4. Verification

- [x] 4.1 Compile the project and manually verify API shape, constructor-independent sources, start-time sink wiring and repeated-start protection, safe and duplicate source handling, missing/empty/malformed histories, UUID paths, legacy-file exclusion, source lifecycle, per-source formatting, persistence failure, concurrent callbacks, and greatest-timestamp selection without adding automated tests.
- [x] 4.2 Validate the OpenSpec change, confirm `TickerServiceImpl` has no generator/scheduler constants, and confirm no unrelated behavior or generated Detag source was edited.
