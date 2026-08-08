## Why

AssetAZ Ticker currently has only a hardcoded price source and therefore cannot observe Evelyn IOU's real Raydium market price. A pool-specific plugin is needed to poll the EVE/USDT pool while preserving the PriceSource lifecycle and future Nenjim module boundaries.

## What Changes

- Complete the existing Raydium Pool PriceSource scaffold as a lifecycle-managed, fixed-delay polling source representing one Raydium pool.
- Add Tether USD (USDT) as a stable, externally resolvable AssetAZ currency distinct from USDC.
- Temporarily compose the EVE/USDT Raydium source in NenjimHub alongside the existing hardcoded EVE/USDC source.
- Preserve history activation, persistence-before-publication, package ownership, and future artifact boundaries without adding discovery, configuration, streaming, conversion, or automatic history creation.

## Capabilities

### New Capabilities

- `assetaz-raydium-pool-price-source`: Defines pool identity, polling, validation, publication, failure isolation, and restartable lifecycle behavior for a Raydium-backed PriceSource.

### Modified Capabilities

- `assetaz-currency-identity-service`: Adds the stable canonical USDT identity and official Solana mint mapping while keeping it distinct from USDC.

## Impact

The change affects the AssetAZ Currency Identity Service catalogue and identifiers, the existing Raydium Pool PriceSource implementation scaffold, and temporary NenjimHub composition. It uses the existing Raydium and PriceSource APIs and does not change the generic Ticker service contract or create new Gradle subprojects.
