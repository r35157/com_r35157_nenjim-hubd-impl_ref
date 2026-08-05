## 1. AssetAZ Ticker API

- [ ] 1.1 Add stable EVE and USDC currency ValueTypes and the well-known `EVE_USDC` `TradingPair`.
- [ ] 1.2 Add immutable `PriceObservation` with a timestamp and `AssetPrice` in the ticker API package.
- [ ] 1.3 Extend `TickerService` with `start()` and `PriceObservation getLatestPrice(TradingPair)`, using clear exceptions for unsupported/inactive pairs and enabled pairs without a persisted observation.

## 2. File-Backed Reference Implementation

- [ ] 2.1 Implement explicit activation from the existing `data/assetaz/ticker/EVE_USDC.prices` file without creating a missing file or directory.
- [ ] 2.2 Implement the strict UTC `uuuuMMddHHmmssSSS'Z':<price>` format and line-by-line history parsing with empty-line and full-line/inline comment support.
- [ ] 2.3 Report malformed data with filename, one-based physical line number, and identifying invalid content, and restore the valid observation with the greatest `observedAt` timestamp as latest.
- [ ] 2.4 Implement thread-safe append-and-`FileChannel.force(true)` behavior that persists every generated observation, never exposes an unpersisted observation, preserves the prior latest observation on persistence failure, and replaces latest after successful persistence only when the new `observedAt` timestamp is later.
- [ ] 2.5 Generate the hardcoded `14.85` startup observation immediately and use fixed-delay scheduling only for the enabled pair so each subsequent attempt begins one minute after the preceding attempt completes.

## 3. Temporary Autorun Wiring

- [ ] 3.1 Start `TickerServiceImpl` from NenjimHub's current direct autorun mechanism without wiring it to any consumer.

## 4. Verification

- [ ] 4.1 Compile the project and manually verify missing, empty/comment-only, out-of-order valid, future-dated, malformed, and unwritable history-file behavior, required exception cases, timestamp/price formatting, fixed-delay timing without catch-up attempts, persist-before-publish ordering, and greatest-`observedAt` latest selection without adding automated tests.
