## Why

Nenjim needs a reusable service that can exchange an exact, human-readable SPL-token amount through Jupiter while preserving the repository's existing Solana wallet and blockchain boundaries. Jupiter Swap V2 supplies the quote, transaction construction, and managed execution required for this flow.

## What Changes

- Add a public `JupiterSwapService` API and immutable `JupiterSwapResult` value describing the confirmed transaction and actual amounts spent and received.
- Add a reference implementation for exact-input Jupiter Swap V2 `/order`, wallet signing, and `/execute` processing.
- Resolve both mint programs and decimal precision through `SolanaBlockChain`, supporting the legacy SPL Token Program and Token-2022.
- Validate caller input, mint metadata, order integrity, transaction metadata, and execution results before returning success.
- Exclude the `jupiterz` router from every order while JupiterZ/RFQ support remains deferred.
- Enforce a small JVM-wide two-second minimum interval between `/order` requests, without delaying `/execute` or automatically retrying submitted executions.

## Capabilities

### New Capabilities

- `jupiter-swap-service`: Exact-input SPL-token swaps through Jupiter Swap V2, including validation, signing, managed execution, throttling, and failure semantics.

### Modified Capabilities

None.

## Impact

This adds public API types under `com.r35157.libs.jupiter.swap` and a reference implementation under `com.r35157.libs.jupiter.swap.impl.ref`. It depends on the existing `SolanaBlockChain` and `SolanaWallet` APIs and Jupiter's keyless Swap V2 HTTP endpoints; it does not add API-key configuration, Evelyn burning, RFQ/JupiterZ support, or a general throttling framework.
