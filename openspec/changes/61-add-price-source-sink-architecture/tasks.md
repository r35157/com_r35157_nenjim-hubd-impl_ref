## 1. Ticker Source API

- [x] 1.1 Add `PriceSourceName` under `String -> Name` in `conf/detag.conf` and retain required generated-type imports in `.tjava` sources.
- [x] 1.2 Add the `PriceSource` and functional `PriceSink` API contracts with typed source name, price, trading pair, and timestamp values.
- [x] 1.3 Extend `PriceObservation` with source identity and extend `TickerService` with pre-start source registration plus stop lifecycle.

## 2. Source Registry and Persistence

- [x] 2.1 Implement source identity validation, duplicate rejection, lifecycle state checks, and exact registered-instance callback validation in `TickerServiceImpl`.
- [x] 2.2 Implement centralized UUID/source/safe-symbol history paths without scanning, creating, or migrating filesystem content.
- [x] 2.3 Load active per-source histories with strict parsing, comments, source-context observations, clear malformed-line errors, and greatest-timestamp restoration across sources.
- [x] 2.4 Persist concurrent active-source announcements to independent histories with complete-line serialization and `FileChannel.force(true)` before atomic greatest-timestamp publication.
- [x] 2.5 Implement ticker start/stop orchestration so only active sources start, every started source receives a stop attempt, and partial startup is rolled back.

## 3. Hardcoded Source and Wiring

- [x] 3.1 Add a stoppable `HardcodedPriceSource` for `EVE_USDC`, source `Hardcoded`, immediate `14.85` announcement, and one-minute fixed-delay scheduling owned entirely by the source.
- [x] 3.2 Update NenjimHub temporary autorun wiring to construct one ticker, pass it as `PriceSink`, register the hardcoded source, and start the ticker.

## 4. Verification

- [x] 4.1 Compile the project and manually verify API shape, safe and duplicate registration, missing/empty/malformed histories, UUID paths, legacy-file exclusion, source lifecycle, per-source formatting, persistence failure, concurrent callbacks, and greatest-timestamp selection without adding automated tests.
- [x] 4.2 Validate the OpenSpec change, confirm `TickerServiceImpl` has no generator/scheduler constants, and confirm no unrelated behavior or generated Detag source was edited.
