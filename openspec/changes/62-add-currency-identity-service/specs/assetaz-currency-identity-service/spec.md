## Purpose

Provides one authoritative AssetAZ identity model for currencies and translates stable UUIDs and namespaced external identifiers without treating display symbols as identity.

## ADDED Requirements

### Requirement: Stable AssetAZ currency identity
The system SHALL represent a currency as an immutable `CurrencyType` with a non-null UUID, name, and symbol. Its UUID is its complete stable identity. Equality and hash codes SHALL depend only on that UUID, regardless of name, symbol, instance identity, or metadata changes.

#### Scenario: Metadata changes for the same UUID
- **WHEN** two currency values have the same UUID but different names or symbols
- **THEN** they compare equal and have the same hash code

#### Scenario: Matching metadata for different UUIDs
- **WHEN** two currency values have different UUIDs but identical names and symbols
- **THEN** they do not compare equal

#### Scenario: Missing canonical metadata
- **WHEN** construction omits a currency name or symbol
- **THEN** construction fails without producing an incomplete currency value

### Requirement: Resolve canonical currencies
The Currency Identity Service SHALL resolve a known AssetAZ UUID to a non-null current currency value and SHALL fail with a clear exception for an unknown UUID.

#### Scenario: Resolve known UUID
- **WHEN** a client resolves a configured AssetAZ currency UUID
- **THEN** the service returns the current currency metadata for that UUID

#### Scenario: Resolve unknown UUID
- **WHEN** a client resolves an unconfigured UUID
- **THEN** the service throws an exception that clearly identifies the unknown UUID

### Requirement: Resolve namespaced external identities
The Currency Identity Service SHALL resolve external currencies by the pair `namespace + externalId`. External symbols MAY be absent and SHALL be retained as metadata when present, but SHALL NOT participate in lookup identity or equality. A namespace SHALL describe the external identification system rather than an observing price source.

#### Scenario: Resolve a Solana mint
- **WHEN** a client supplies a configured Solana-mint namespace and mint address with symbol metadata
- **THEN** the service returns the currency mapped to that namespace and mint address

#### Scenario: Symbol differs from configured metadata
- **WHEN** the namespace and external identifier are configured but the supplied symbol differs
- **THEN** the external references compare equal and the service resolves the same currency because the symbol is not part of external identity

#### Scenario: External symbol is absent
- **WHEN** a client supplies a configured namespace and external identifier without symbol metadata
- **THEN** the service resolves the currency and returns its non-null canonical symbol

#### Scenario: Unknown external identity
- **WHEN** a client supplies an unconfigured namespace and external identifier
- **THEN** the service throws an exception that clearly identifies the unknown external identity

### Requirement: Reverse external-reference lookup
The Currency Identity Service SHALL return all configured external references for a known AssetAZ UUID as a non-null immutable set, including an empty set when none exist, and SHALL fail clearly for an unknown UUID.

#### Scenario: Currency has multiple references
- **WHEN** a known UUID has references in multiple external systems
- **THEN** reverse lookup returns every reference without exposing mutable registry state

#### Scenario: Known currency has no references
- **WHEN** a known UUID has no configured external references
- **THEN** reverse lookup returns an empty immutable set

#### Scenario: Reverse lookup Evelyn IOU
- **WHEN** a client requests external references for the Evelyn IOU UUID
- **THEN** the result contains its configured Solana mint reference

### Requirement: Immutable conflict-free hardcoded catalogue
The hardcoded Currency Identity Service SHALL preserve the existing AssetAZ currency UUIDs, maintain one current currency value for each UUID, support multiple external references per UUID, reject conflicting UUID or external-identity mappings during initialization, expose no public mutation operation, and be safe for concurrent reads after construction. NenjimHub SHALL create exactly one hardcoded service instance for its autorun context and SHALL pass that same instance to every autorun component that requires currency identities.

#### Scenario: Conflicting external mapping
- **WHEN** initialization maps the same namespace and external identifier to two different AssetAZ UUIDs
- **THEN** initialization fails with a clear conflict exception

#### Scenario: Conflicting currency metadata
- **WHEN** initialization provides conflicting current values for the same UUID
- **THEN** initialization fails with a clear conflict exception

#### Scenario: Compose autorun components
- **WHEN** NenjimHub constructs its autorun component graph
- **THEN** it creates one hardcoded Currency Identity Service and injects that same instance into every component that requires it

### Requirement: Trading pairs use service-owned identities
Production code SHALL construct trading pairs from currency values obtained from the current Currency Identity Service and SHALL NOT maintain a parallel static registry of canonical currency or trading-pair instances.

#### Scenario: Construct a configured pair
- **WHEN** production code needs a trading pair for configured currencies
- **THEN** it resolves both UUIDs or external identities through the current service before constructing the pair

### Requirement: Stable UUID identifiers are implementation-independent
The API SHALL expose stable AssetAZ currency UUID constants without names, symbols, external mappings, or `CurrencyType` instances. Consumers that require a known AssetAZ UUID SHALL use these API identifiers and SHALL NOT import the hardcoded Currency Identity Service implementation.

#### Scenario: Consumer resolves a known UUID
- **WHEN** a consumer needs a known AssetAZ currency by UUID
- **THEN** it obtains the UUID from the API identifier class and resolves it through its injected Currency Identity Service

### Requirement: Pool amounts follow actual mint identities
Pool accounting SHALL resolve mint A and mint B through the Currency Identity Service using their actual namespaced Solana mint addresses and SHALL preserve the pool's A/B order when assigning currency types to amounts.

#### Scenario: State loads a pool
- **WHEN** State receives pool information with mint A and mint B
- **THEN** amount A uses the currency resolved from mint A and amount B uses the currency resolved from mint B

### Requirement: Raydium resolves response currencies
Raydium SHALL receive a Currency Identity Service dependency and SHALL build a fetched pool price's trading pair in Raydium response order by resolving `mintA` and `mintB` as namespaced Solana mint identities. It SHALL NOT resolve currencies by symbol alone.

#### Scenario: Fetch a configured Raydium pool price
- **WHEN** Raydium returns a price plus configured mint A and mint B identifiers, with or without symbols
- **THEN** the returned asset price uses a trading pair whose base is the service resolution of mint A and whose quote is the service resolution of mint B

#### Scenario: Raydium returns an unknown mint
- **WHEN** either returned mint identity is not configured
- **THEN** pool-price creation fails with the Currency Identity Service's clear unknown-identity exception
