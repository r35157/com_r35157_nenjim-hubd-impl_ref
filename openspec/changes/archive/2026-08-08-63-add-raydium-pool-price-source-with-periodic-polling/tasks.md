## 1. Currency Identity

- [x] 1.1 Add the API-owned stable USDT UUID and hardcoded canonical Tether USD catalogue entry with the official Solana mint.
- [x] 1.2 Verify USDT resolution by UUID and mint, distinct identity from USDC, and absence of conversion or parallel registry data.

## 2. Raydium Pool PriceSource

- [x] 2.1 Complete the existing `impl.raydiumpool.RaydiumPoolPriceSource` scaffold with validated pool identity and no construction-time sink or network access.
- [x] 2.2 Implement immediate source-owned daemon polling with one-minute fixed delay, response-pair validation, millisecond observation timestamps, sink publication, and contextual failure isolation.
- [x] 2.3 Implement repeated-start rejection, idempotent resource-terminating stop, interruption handling, and restart with fresh scheduling resources.

## 3. Temporary Composition

- [x] 3.1 Construct the EVE/USDT pair and Raydium pool source in `NenjimHubImpl` using the shared Currency Identity Service and hardcoded pool ID.
- [x] 3.2 Inject the Raydium source alongside the unchanged running `HardcodedPriceSource` through the temporary Ticker constructor.

## 4. Boundary and Behavior Review

- [x] 4.1 Verify the `2fc553b` package and future module boundaries: Ticker API, PriceSource/PriceSink plugin API, Ticker reference implementation, and separate hardcoded and Raydium source implementations.
- [x] 4.2 Verify exact pool-based source naming and EVE/USDT history path, existing-file-only activation, persistence before publication, polling recovery, and stop/restart semantics without adding automated tests.
- [x] 4.3 Review the complete diff for stale imports, duplicate implementations, incomplete wiring, unintended HardcodedPriceSource changes, and excluded features.

## 5. Build and Specification Verification

- [x] 5.1 Compile main and test source sets without running unit tests.
- [x] 5.2 Run strict OpenSpec validation and `git diff --check`.
- [x] 5.3 Sync both deltas into canonical specifications, archive the completed change, and rerun strict OpenSpec validation.
