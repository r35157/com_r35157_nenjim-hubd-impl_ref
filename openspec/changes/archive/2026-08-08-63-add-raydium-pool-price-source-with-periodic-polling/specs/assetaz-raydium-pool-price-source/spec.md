## Purpose

Provides independently lifecycle-managed price observations for one Raydium pool through the AssetAZ PriceSource plugin contract.

## ADDED Requirements

### Requirement: One source represents one stable Raydium pool
A Raydium Pool PriceSource instance SHALL represent exactly one Raydium pool, SHALL expose the expected trading pair supplied at construction, and SHALL expose the stable source name `Raydium-<poolId>`. Construction SHALL validate all dependencies and identity values, leave the source stopped, perform no network access, and require no price sink until startup.

#### Scenario: Construct a pool source
- **WHEN** a client constructs a source with a Raydium service, pool ID, and expected trading pair
- **THEN** the stopped source reports that trading pair and a source name containing that exact pool ID without performing a price retrieval

#### Scenario: Reject invalid construction
- **WHEN** a required constructor argument is absent or invalid
- **THEN** construction fails without starting polling or accessing the network

### Requirement: Poll immediately with fixed delay
On startup, the source SHALL begin its first retrieval attempt immediately and SHALL begin each subsequent attempt one minute after the preceding attempt completes. Polling SHALL execute on a daemon scheduler thread owned exclusively by that source instance.

#### Scenario: Start a source
- **WHEN** a stopped source is started with a price sink
- **THEN** its first pool-price retrieval begins immediately on its source-owned daemon scheduler

#### Scenario: Complete a periodic attempt
- **WHEN** a retrieval attempt completes
- **THEN** the next attempt begins after the configured one-minute delay without fixed-rate catch-up attempts

### Requirement: Validate and publish pool observations
The source SHALL retrieve the configured pool price and compare the response trading pair with its expected pair. For a matching response, it SHALL create the observation timestamp after the successful response, truncate it to milliseconds, and publish the returned price value through the sink supplied at startup.

#### Scenario: Receive the expected pair
- **WHEN** a successful pool response contains the expected trading pair
- **THEN** the source publishes its price with a post-response timestamp truncated to milliseconds

#### Scenario: Receive a different pair
- **WHEN** a pool response contains a trading pair different from the expected pair
- **THEN** the source publishes no observation, logs the pool ID plus expected and received pairs, and schedules the next attempt normally

### Requirement: Isolate temporary polling failures
Temporary network, Raydium, parsing, interruption, or downstream persistence failures SHALL be logged with the pool ID and expected trading pair and SHALL NOT cancel later polling attempts or stop the Ticker service.

#### Scenario: A polling attempt fails temporarily
- **WHEN** a retrieval or publication attempt throws an exception
- **THEN** the source logs the contextual failure and retries on the next fixed-delay interval

### Requirement: Restartable source lifecycle
The source SHALL reject a repeated startup before replacing its existing sink. Stopping SHALL be idempotent, terminate source-owned scheduling resources, correctly preserve interruption, and permit a later startup with a newly supplied sink.

#### Scenario: Start an already-started source
- **WHEN** startup is invoked while the source is already running
- **THEN** startup fails and the currently active sink remains unchanged

#### Scenario: Stop a running source
- **WHEN** stop is invoked on a running source
- **THEN** its scheduler terminates and no further polling begins

#### Scenario: Stop an already-stopped source
- **WHEN** stop is invoked while the source is stopped
- **THEN** it completes without error

#### Scenario: Restart a stopped source
- **WHEN** a previously stopped source is started with a sink
- **THEN** it creates fresh source-owned scheduling resources and begins an immediate retrieval attempt

### Requirement: Activation uses independent Ticker history
The pool source SHALL participate in the existing Ticker activation contract under its exact trading pair and pool-based source name. It SHALL be active only when that exact history file already exists and SHALL NOT create or migrate history files.

#### Scenario: Pool history exists
- **WHEN** the EVE/USDT history file exists beneath the EVE UUID, USDT UUID, and exact pool-based source-name path
- **THEN** the Ticker activates the Raydium source independently of the hardcoded EVE/USDC history

#### Scenario: Pool history is absent
- **WHEN** the exact Raydium source history file is absent
- **THEN** the Ticker leaves that source inactive and creates no file or directory

