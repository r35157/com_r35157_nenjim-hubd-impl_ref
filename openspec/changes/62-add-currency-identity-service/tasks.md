## 1. Currency Identity API and Catalogue

- [x] 1.1 Move `CurrencyType` into the AssetAZ value-types package and make equality and hashing UUID-only.
- [x] 1.2 Add the annotated immutable `ExternalCurrencyReference` and read-only `CurrencyIdentityService` API.
- [x] 1.3 Implement the immutable hardcoded catalogue with preserved UUIDs, reverse lookup, Solana mint mappings, and construction-time conflict detection.

## 2. Production Migration

- [x] 2.1 Migrate every production `CurrencyType` import and replace hardcoded ticker pair construction with identities from the current service.
- [x] 2.2 Inject the Currency Identity Service into `RaydiumImpl` and resolve Raydium mint identities and response-ordered trading pairs.
- [x] 2.3 Update production construction/configuration sites for the new dependencies and remove `WellKnownCurrencyTypes` and `WellKnownTradingPairs`.

## 3. Verification

- [x] 3.1 Compile every affected module without running unit tests.
- [x] 3.2 Run strict OpenSpec validation and `git diff --check`.
- [x] 3.3 Review the complete diff and production tree for incomplete migrations, old imports, parallel currency registries, and unintended test or Nenjim Test Tool changes.

## 4. Composition-Root Correction

- [x] 4.1 Create the hardcoded Currency Identity Service exactly once in `NenjimHubImpl.startAutoRunProcesses()` and pass it into every autorun construction chain that needs it.
- [x] 4.2 Replace identity consumers' local hardcoded-service construction with required `CurrencyIdentityService` constructor dependencies.
- [x] 4.3 Compile main and test source sets, strict-validate OpenSpec, run `git diff --check`, and confirm no hardcoded service construction remains outside the NenjimHub composition root.

## 5. Public API Documentation

- [x] 5.1 Document the public Currency Identity Service, external-reference, and currency value APIs, including identity semantics, nullability, and failure behavior.
- [x] 5.2 Compile main and test source sets and rerun strict OpenSpec validation and `git diff --check`.
