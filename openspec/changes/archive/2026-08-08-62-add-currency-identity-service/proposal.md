## Why

Currency identities are currently duplicated in static registries and coupled to callers such as Raydium. AssetAZ needs one service-owned UUID identity model that can resolve external identifiers without making symbols or integrations the canonical identity.

## What Changes

- Add a public AssetAZ Currency Identity Service API and immutable external-reference value type.
- Publish stable AssetAZ currency UUID constants in an API-only identifier class so consumers never depend on the hardcoded implementation package.
- Add a hardcoded reference implementation containing the current currency definitions and Solana mint mappings.
- **BREAKING** Move `CurrencyType` from the shared basic value-types package to `com.r35157.assetaz.valuetypes` and define equality by UUID alone.
- **BREAKING** Remove `WellKnownCurrencyTypes` and `WellKnownTradingPairs`; construct trading pairs from identities resolved by the service.
- Inject the Currency Identity Service into `RaydiumImpl` and resolve pool mint identities when producing prices and ranges.
- Create the hardcoded service exactly once in NenjimHub's autorun composition root and pass that shared service instance to every component that requires currency identities.
- Preserve canonical currency metadata as non-null while allowing external references to omit symbol metadata and allowing currencies to have no external references.
- Do not add or rewrite unit tests or Nenjim Test Tool code as part of this change.

## Capabilities

### New Capabilities

- `assetaz-currency-identity-service`: Defines UUID-based currency identity, external-identifier resolution, the immutable hardcoded catalogue, and its use by Raydium.

### Modified Capabilities

None.

## Impact

The change affects the AssetAZ value-type and service packages, public UUID identifiers, all production imports of `CurrencyType`, NenjimHub dependency wiring, hardcoded ticker data, Raydium construction and pricing, State pool accounting, and callers that construct identity-dependent components. It removes the two legacy static currency/trading-pair registries. No external dependency or ValueTag configuration change is required.
