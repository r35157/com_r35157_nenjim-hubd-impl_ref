## ADDED Requirements

### Requirement: Stable Tether USD identity
The Currency Identity Service SHALL provide Tether USD as a stable AssetAZ currency with canonical name `Tether USD`, canonical symbol `USDT`, and an implementation-independent UUID identifier. It SHALL resolve that currency through both the stable UUID and official Solana mint `Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB`. USDT and USDC SHALL remain distinct currency identities, and resolution SHALL NOT imply conversion between them.

#### Scenario: Resolve USDT by stable UUID
- **WHEN** a client resolves the stable USDT UUID
- **THEN** the service returns the canonical Tether USD currency with symbol USDT

#### Scenario: Resolve USDT by official Solana mint
- **WHEN** a client resolves the official USDT mint in the Solana-mint namespace
- **THEN** the service returns the same canonical USDT identity as UUID resolution

#### Scenario: Compare USDT and USDC
- **WHEN** a client resolves both USDT and USDC
- **THEN** they have different UUIDs and no implicit conversion is performed

