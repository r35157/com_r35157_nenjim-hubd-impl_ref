## 1. Public API

- [x] 1.1 Add the ValueTagged `JupiterSwapService` exact-input method with complete public Javadoc and validation/failure contracts.
- [x] 1.2 Add the immutable `JupiterSwapResult` value containing the signature and actual human-readable amounts.

## 2. Reference Implementation

- [x] 2.1 Add the reference implementation with injected `SolanaBlockChain` and `SolanaWallet` dependencies and private Swap V2 wire DTOs.
- [x] 2.2 Validate arguments, both on-chain mint accounts and programs, mint decimal metadata, and exact raw input conversion before order acquisition.
- [x] 2.3 Implement keyless `/order` acquisition in exact-input mode, always excluding JupiterZ, with JVM-wide interruptible two-second request-start pacing.
- [x] 2.4 Validate order identity and transaction metadata before signing through `SolanaWallet`.
- [x] 2.5 Submit a signed transaction once through `/execute`, without retries or order throttling, and validate the confirmed result and actual amounts.

## 3. Verification

- [x] 3.1 Compile Detag-generated main and test source sets and run the repository's existing test/check tasks without adding automated tests.
- [x] 3.2 Run strict OpenSpec validation and `git diff --check`.
- [x] 3.3 Review the complete diff for stale imports, generated-file edits, unrelated refactors, tests, and any implementation that violates the module or retry boundaries.
