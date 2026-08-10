## Purpose

Defines safe exact-input SPL-token swaps through Jupiter Swap V2 using human-readable amounts and an existing Solana wallet.

## ADDED Requirements

### Requirement: Public exact-input swap contract
The service SHALL accept distinct input and output SPL mint addresses, a positive human-readable input amount, and a maximum slippage in basis points. It SHALL return the confirmed Solana transaction signature together with the actual human-readable input amount spent and output amount received.

#### Scenario: Successful exact-input swap
- **WHEN** a caller requests a valid exact-input swap that Jupiter executes successfully
- **THEN** the result contains the confirmed signature and the actual spent and received amounts converted with their respective mint decimal precision

#### Scenario: Invalid caller input
- **WHEN** either mint is blank, both mints are equal, the amount is null or non-positive, or the maximum slippage is outside 0 through 10000 basis points
- **THEN** the service rejects the request before requesting a Jupiter order

### Requirement: On-chain mint validation and exact amount conversion
Before requesting an order, the service SHALL load both mint accounts through the Solana blockchain, require each mint to exist and be owned by either the legacy SPL Token Program or Token-2022, and resolve each mint's decimal precision. The service SHALL convert the input amount exactly to a positive raw integer and SHALL reject values that cannot be represented with the input mint's precision.

#### Scenario: Supported legacy and Token-2022 mints
- **WHEN** the input and output mint accounts are owned by either supported SPL token program and their supplies provide valid decimal precision
- **THEN** the service uses those independently resolved precisions for raw request and human-readable result amounts

#### Scenario: Missing or unsupported mint
- **WHEN** either mint account is absent or is owned by an unsupported program
- **THEN** the service fails before requesting an order or signing a transaction

#### Scenario: Fraction smaller than the mint unit
- **WHEN** the requested input amount has a non-zero fraction beyond the input mint's decimal precision
- **THEN** the service rejects the amount rather than rounding it

### Requirement: Safe Jupiter order acquisition
The service SHALL use Jupiter's keyless Swap V2 order endpoint in `ExactIn` mode with the wallet as taker, the caller's slippage limit, and `excludeRouters=jupiterz`. Across all reference-implementation instances in one JVM, starts of order HTTP requests SHALL be separated by at least two seconds. This limiter SHALL remain local to the implementation and SHALL NOT delay execution requests.

#### Scenario: Every order excludes JupiterZ
- **WHEN** the service requests an order
- **THEN** the request identifies the exact input and output mints, raw input amount, wallet taker, exact-input mode, slippage limit, and excludes the `jupiterz` router

#### Scenario: Concurrent service instances request orders
- **WHEN** multiple reference-implementation instances concurrently need Jupiter orders
- **THEN** their order HTTP requests begin at least two seconds apart JVM-wide while execution requests remain unthrottled

#### Scenario: Order wait is interrupted
- **WHEN** a caller is interrupted while waiting for its permitted order-request time
- **THEN** the service propagates interruption without sending that order request

### Requirement: Order integrity validation before signing
Before signing, the service SHALL require a successful HTTP response containing a well-formed order whose input mint, output mint, raw input amount, and exact-input mode match the request. It SHALL also require a non-blank unsigned transaction, a non-blank request identifier, and a positive valid last block height. Any order build error, malformed body, mismatch, or missing required value SHALL fail before signing.

#### Scenario: Valid matching order
- **WHEN** Jupiter returns a well-formed order matching both requested mints and the exact raw input amount with all required transaction metadata
- **THEN** the service passes the returned unsigned transaction to the configured Solana wallet for signing

#### Scenario: Mismatching or incomplete order
- **WHEN** an order changes a mint, amount, or swap mode, or lacks a valid transaction, request identifier, or last valid block height
- **THEN** the service rejects the order without signing or executing it

#### Scenario: Jupiter order HTTP or decoding failure
- **WHEN** the order endpoint returns a non-success status or malformed JSON
- **THEN** the service reports an I/O failure with useful endpoint response context and does not sign a transaction

### Requirement: Single-attempt managed execution
After successful signing, the service SHALL reject a blank signed transaction and submit exactly one Swap V2 execution request containing the signed transaction, order request identifier, and last valid block height. The service SHALL never automatically retry after the execution request has been submitted, because transport failure, timeout, or interruption can leave the execution outcome unknown.

#### Scenario: Signed transaction is executed once
- **WHEN** wallet signing returns a non-blank transaction
- **THEN** the service sends one execution request without applying the order limiter

#### Scenario: Execution outcome is unknown
- **WHEN** the execution HTTP exchange times out, is interrupted, loses its response, or otherwise fails after submission
- **THEN** the service reports the failure and does not automatically request another order or resubmit execution

#### Scenario: Blank signed transaction
- **WHEN** wallet signing returns a blank serialized transaction
- **THEN** the service fails without submitting an execution request

### Requirement: Confirmed execution result validation
The service SHALL return success only for a successful execution response with a non-blank transaction signature and positive valid actual total input and output raw amounts. It SHALL convert each actual amount using the independently resolved precision of its mint. Non-success HTTP responses, malformed responses, expired or rejected swaps, failed status, blank signatures, and invalid result amounts SHALL be reported as failures.

#### Scenario: Successful execution response
- **WHEN** Jupiter reports `Success` with a signature and valid actual total input and output amounts
- **THEN** the service returns those actual amounts in human-readable units and the reported transaction signature

#### Scenario: Jupiter rejects or fails execution
- **WHEN** Jupiter returns a failed status, expiration, rejection, non-success HTTP status, malformed body, blank signature, or invalid actual amount
- **THEN** the service reports an I/O failure and does not represent the swap as successful

### Requirement: API and implementation separation
The stable service interface and result value SHALL reside in the Jupiter swap API package, while HTTP DTOs, endpoint handling, throttling, and the reference implementation SHALL remain in the reference-implementation package.

#### Scenario: Downstream API consumer
- **WHEN** downstream code depends only on the Jupiter swap API package
- **THEN** it can invoke swaps and inspect results without depending on private wire DTOs or reference-implementation mechanics
