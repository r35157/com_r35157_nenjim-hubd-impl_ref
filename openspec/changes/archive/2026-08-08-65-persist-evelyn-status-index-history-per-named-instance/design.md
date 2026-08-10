## Context

See `proposal.md` for motivation and the capability delta for the behavioral contract. Issue #64 established that Evelyn owns sampling and complete history while EMC is presentation-only. Ticker already demonstrates durable append with missing-final-newline handling, while configuration parsing demonstrates strict first-actual-entry format declarations. Issue #65 applies those repository patterns to a distinct Evelyn-owned format without changing either existing subsystem.

## Goals / Non-Goals

**Goals:**

- Make a normalized instance name a safe permanent storage identity with atomic JVM-local ownership.
- Make startup transactional from the caller's perspective: validate and parse fully before publishing history or starting a thread.
- Coordinate persistence and publication under Evelyn ownership with durable append ordering.
- Separate restartable stop from irreversible close without releasing a live writer's identity.

**Non-Goals:**

- Creating or repairing runtime storage, cross-process locking, migration, retention, rotation, compaction, additional record types, test creation, or changes to Ticker, Raydium, EMC file access, and unrelated composition.

## Decisions

### Use AutoCloseable for permanent lifecycle completion

`Evelyn` extends `AutoCloseable` and overrides `close()` without a checked exception. `start()` and `stop()` remain the operational lifecycle: stop is restartable and retains the name, while close is permanent and idempotent. This is preferable to an Evelyn-specific disposal name because Java callers can use standard resource ownership and the semantic distinction remains explicit in the contract.

### Validate completely before atomic reservation

Construction first validates every non-name argument and derives the NFC name, case-folded registry key, direct-child directory, and status path. Only after all fallible argument validation succeeds does a static concurrent set reserve the key atomically. The constructor performs no I/O. This prevents invalid construction from leaking reservations and keeps independently constructible components stopped.

### Treat close as release-after-confirmed-termination

Lifecycle state distinguishes stopped, started, stopping, closed, and termination-failed conditions. Stop interrupts and awaits the owned scheduler. Only confirmed termination returns the object to restartable stopped state. Close invokes the same safe stop path and removes the registry key only after confirmation; any failure is reported while the object retains both its non-startable safety state and reservation. Repeated successful close is a no-op.

### Make startup loading transactional

Every start rechecks the directory and file using both metadata/access predicates and actual UTF-8 reading. All lines are read, the dedicated status-format declaration is validated before record parsing, and records are decoded into a temporary list with non-decreasing timestamp checks. Only a successful complete load replaces the in-memory list and establishes the last persisted timestamp. The executor is created last. Failed startup leaves memory empty, storage untouched, and no sampling thread.

### Keep status format validation dedicated

The loader hardcodes `SUPPORTED_STATUS_HISTORY_FORMAT_VERSION = 1` and applies the same declaration convention as configuration files, but produces status-history-specific diagnostics and does not couple version numbers or parsing code to configuration or Ticker formats. Type codes are explicit constants rather than enum ordinals. Full original lines are retained for line diagnostics before comment removal.

### Serialize durable append and publication

The single sampling thread still coordinates the critical operation explicitly. Under the history lock it rejects timestamps earlier than the last persisted timestamp, opens the existing file without create/truncate options, adds a newline first when necessary, writes the encoded UTF-8 record, and forces the channel. Only then does it update the last timestamp and append to visible memory. I/O failures escape this critical operation into the sampling boundary, which logs instance/path/context and leaves scheduling alive.

## Risks / Trade-offs

- [Filesystem access predicates can race with actual operations] → perform the required predicates for useful diagnostics and still treat actual read/open/write/force failures as authoritative.
- [A blocked sampling task may ignore interruption] → never release the name unless executor termination is confirmed; retain the reservation and report failure.
- [JVM-local uniqueness cannot protect another process] → document cross-process locking as out of scope and rely on deployment ownership.
- [Full history loading is unbounded] → accept the issue-defined complete-load behavior; retention and compaction are future work.
- [NFC plus case folding can map spellings to one identity] → preserve the winning normalized display spelling on disk while reserving a `Locale.ROOT` lower-case key.

## Migration Plan

Before enabling each instance, operators create `data/evelyn/Production/status.log` and `data/evelyn/Test/status.log` with physical first line `FORMAT_VERSION=1`. Deployment then starts Ticker, constructs and starts both named Evelyn instances, and opens EMC. Rollback requires stopping/closing the instances but leaves status files untouched; older code will ignore them because it has no Evelyn persistence support.
