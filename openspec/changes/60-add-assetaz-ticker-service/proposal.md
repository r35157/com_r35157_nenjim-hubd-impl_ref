## Why

AssetAZ has only empty ticker service shells, so callers cannot obtain a typed latest price or retain observations across restarts. A minimal reusable ticker establishes that service contract and a durable reference implementation before real market sources are introduced.

## What Changes

- Define an AssetAZ ticker API that exposes the latest persisted observation by `TradingPair` using existing price ValueTypes and throws clear exceptions when no price is available.
- Add a reference implementation for the single supported `EVE_USDC` pair, producing the hardcoded price `14.85` immediately at startup and once per minute.
- Make an existing `.prices` file the explicit activation mechanism, load its valid history, and append new observations durably before publishing them as latest.
- Start the ticker through NenjimHub's current temporary autorun mechanism.
- Leave the pair unavailable when its history file is missing, reject malformed history at startup, and retain the prior published observation when a new observation cannot be persisted safely.

## Capabilities

### New Capabilities

- `assetaz-ticker-service`: Typed latest-price access, file-backed activation and history loading, and durable periodic publication for the initial AssetAZ trading pair.

### Modified Capabilities

None.

## Impact

- Extends the existing empty API and reference implementation under `com.r35157.assetaz.core.service.ticker` and adds `PriceObservation` there.
- Reuses `AssetPrice`, `TradingPair`, and `ΩPriceΩ`; no datasource-plugin or consumer integration is introduced.
- Adds temporary startup wiring to `NenjimHubImpl`, without transferring ownership of the ticker to NenjimHub.
- Uses `data/assetaz/ticker/EVE_USDC.prices` as operator-controlled persistent activation and history.
- Adds no automated tests.
