## Context

See `proposal.md` for motivation. Issues #48 through #50 already established `SolanaBlockChain` for RPC-backed account and mint metadata, and `SolanaWallet` for isolated transaction signing. The Swap V2 integration must compose these APIs with Jupiter's `/order` and `/execute` endpoints while retaining module-oriented API and implementation package boundaries.

Jupiter's `/execute` manages transaction landing and can return actual wallet-level totals. Once an execution request has left this process, a transport failure cannot distinguish an unexecuted request from a completed swap whose response was lost.

## Goals / Non-Goals

**Goals:**

- Keep the public API expressed in human-readable ValueTagged amounts while using raw integer units at the Jupiter boundary.
- Validate all locally knowable invariants before wallet signing and execution submission.
- Share the required order pacing across every reference-implementation instance in one JVM.
- Preserve interruption and expose actionable endpoint and response failures through the declared checked exceptions.

**Non-Goals:**

- Exact-output swaps, JupiterZ/RFQ, API-key configuration, retries, direct DEX integration, or a reusable throttling subsystem.
- Constructing an Evelyn burner or changing existing Solana wallet/blockchain responsibilities.
- Adding automated tests in this change; the existing repository verification suites will still be run.

## Decisions

### Compose the established Solana APIs

`JupiterSwapServiceImpl` receives `SolanaBlockChain` and `SolanaWallet`. It obtains the wallet address from the wallet, loads each mint account and supply from the blockchain, and passes Jupiter's Base64 transaction through `SolanaWallet.signTransaction`. Duplicating RPC or signing logic inside the Jupiter implementation was rejected because it would bypass the module contracts established by issues #48 through #50.

### Detect each mint's token program independently

For both input and output, the implementation reads the mint account owner and matches it against `SolanaSPLTokenProgram`. It then requests supply metadata with that program. This supports legacy SPL Token and Token-2022 pairs in any combination and prevents assumptions based on one side of the pair.

### Use exact decimal conversion at the boundary

The requested `BigDecimal` is shifted by the input mint's decimal count and converted with `toBigIntegerExact()`. Actual raw execution totals are parsed as non-negative integers and shifted left with their respective decimal counts. Rounding was rejected because it would silently change the amount authorized by the caller.

### Keep wire types private to the reference implementation

Small private records model only the `/order` and `/execute` fields needed for validation and results. Jackson ignores additional response fields, allowing Jupiter to add metadata without expanding the public API. The stable API package contains only `JupiterSwapService` and `JupiterSwapResult`.

### Use the keyless Swap V2 endpoint and explicitly exclude JupiterZ

The reference implementation calls `https://api.jup.ag/swap/v2/order` and `/execute` without API-key headers. Every order includes `swapMode=ExactIn` and `excludeRouters=jupiterz`; relying on a router default was rejected because JupiterZ/RFQ support is explicitly deferred to issue #66.

### Serialize only order-request starts with a JVM-wide gate

A private static monitor and monotonic timestamp separate request starts by two seconds. Callers wait interruptibly while holding the gate, and the gate remains held for that order exchange so a delayed thread cannot begin out of its reserved sequence. A generalized limiter was rejected as unnecessary scope, and `/execute` bypasses this gate.

### Treat execution submission as a non-retry boundary

The implementation performs one `HttpClient.send` for `/execute` and contains no retry loop around it or the whole swap. Any failure after submission propagates, with a message warning that execution may be unknown and balances must be reloaded. Retrying was rejected because it can duplicate a financially consequential action.

### Validate response integrity in phases

Mint and amount checks occur before `/order`; order identity and transaction metadata checks occur before signing; execution status, signature, and actual totals are checked before constructing the public result. Non-2xx responses retain status and body context, while malformed JSON is wrapped as `IOException`.

## Risks / Trade-offs

- [Jupiter may change its wire schema or endpoint behavior] → Decode a minimal tolerant DTO set, but strictly validate every field used for signing and result construction.
- [A submitted execution can succeed despite a local timeout or interruption] → Never retry and report the outcome as unknown so callers reload balances before deciding what to do.
- [A slow order exchange serializes later order callers] → This intentionally small implementation-specific gate prioritizes strict JVM-wide start spacing and remains completely separate from execution requests.
- [Keyless service availability or limits can change] → Surface HTTP response status/body clearly; API-key configuration remains intentionally out of scope.

## Migration Plan

This is an additive API and implementation. Downstream composition can instantiate the reference implementation with its existing blockchain and wallet instances. Rollback consists of removing the new package because no existing API or persisted data is changed.
