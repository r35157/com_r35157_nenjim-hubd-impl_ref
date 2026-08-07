## Context

See `proposal.md` for motivation. Currency metadata currently lives in `WellKnownCurrencyTypes`, while `WellKnownTradingPairs` captures static pairs and `RaydiumImpl` hardcodes one of them. The repository temporarily hosts both logical AssetAZ API and implementation packages, and Nenjim runtime discovery is outside this issue.

## Goals / Non-Goals

**Goals:**

- Separate the public identity API from its hardcoded implementation package.
- Make UUID identity robust across service instances and metadata revisions.
- Migrate every production caller in one step so no parallel authoritative registry remains.
- Let Raydium translate the pool response's actual mint identities into its returned pair.

**Non-Goals:**

- Moving value types other than `CurrencyType`.
- Runtime service discovery, remote/database catalogues, public registration, or constructor restriction.
- Unit tests or Nenjim Test Tool changes.

## Decisions

### External identity is a private composite key

`ExternalCurrencyReference` is the public immutable carrier, while the implementation indexes it using a private `namespace + externalId` key. This makes symbol changes irrelevant to lookup and prevents the public record's generated equality—which includes metadata—from defining identity accidentally. Using the full record as the map key was rejected for that reason.

### Catalogue construction validates before freezing

The hardcoded implementation builds local maps, detects duplicate UUIDs with conflicting metadata and duplicate external identities targeting different UUIDs, then stores immutable copies. Reverse-reference sets are also immutable. This gives lock-free concurrent reads and contains mutation entirely within construction. A public registration API was rejected because version 1 is deliberately read-only.

### NenjimHub owns the service instance

`NenjimHubImpl.startAutoRunProcesses()` is the temporary Cauldron composition root. It creates exactly one `HardcodedCurrencyIdentityService`, then passes the interface-typed instance through constructors to the ticker source, Solana, Jupiter, Raydium, Evelyn, and any other autorun component that needs currency identities. Identity consumers never instantiate the hardcoded implementation themselves and do not use a global singleton. Multiple local service instances were rejected because they create independent lifecycles and prevent Nenjim from replacing the context's implementation coherently.

### Existing UUIDs move with their metadata

The four existing UUID/name/symbol definitions move intact from `WellKnownCurrencyTypes` to the hardcoded implementation. Production consumers resolve those UUIDs from their current service rather than holding enum-owned objects. Static UUID constants may identify requested currencies, but no static `CurrencyType` or `TradingPair` objects remain.

### Raydium uses response mint order

`RaydiumImpl` receives `CurrencyIdentityService` beside `SolanaBlockChain`. `fetchPoolPrice` extracts both response mint addresses and their symbol metadata, creates references in a Solana-mint namespace, resolves each through the service, and constructs `TradingPair(mintA, mintB)`. Other Raydium calculations that require a pair likewise derive it from their pool info rather than a hardcoded pair. Constructor injection is the temporary integration seam until Nenjim provides dynamic dependencies.

## Risks / Trade-offs

- [Existing constructors and imports break during migration] → Update all production call sites and compile every affected Gradle module before completion.
- [Raydium response shape differs between endpoints] → Reuse the existing pool-node extraction path and validate address/symbol fields explicitly with clear IO errors.
- [A second registry survives unnoticed] → Search the complete production tree for old types, static pairs, and legacy imports during final review.
- [No new automated regression coverage] → Respect the explicit issue scope and rely on compilation, strict OpenSpec validation, and focused diff/static review.

## Migration Plan

1. Introduce the AssetAZ API and hardcoded implementation with the preserved catalogue and required Solana mint mappings.
2. Create one implementation instance in NenjimHub's autorun composition root and pass it through every identity-dependent construction chain.
3. Move `CurrencyType`, migrate production imports, and replace static trading-pair use with resolutions from the injected service.
4. Inject the service into Raydium and resolve response mint identities.
5. Delete the legacy registries only after all production references are gone.
6. Compile affected modules without running tests, validate OpenSpec strictly, and inspect the complete diff.

Rollback consists of reverting the change as one unit because the package move, registry deletion, and constructor change are intentionally atomic.
