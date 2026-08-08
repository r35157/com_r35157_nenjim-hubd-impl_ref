## 1. Named Identity and Lifecycle

- [x] 1.1 Add NFC portable-name validation, direct-child path derivation, and atomic case-insensitive JVM-local reservation after all constructor validation.
- [x] 1.2 Extend the public Evelyn API with exception-free `AutoCloseable.close()` and implement restartable stop versus permanent idempotent close without releasing a possibly live writer.
- [x] 1.3 Update all required constructors and Production/Test composition to use independent named instances without enabling unrelated services.

## 2. Strict Storage Restoration

- [x] 2.1 Validate the externally provisioned per-instance directory and `status.log` type/access on every start without creating or modifying them.
- [x] 2.2 Validate dedicated status format version 1 as the first actual entry before parsing records, with path and declaration diagnostics.
- [x] 2.3 Strictly decode every type-1 record, reject unknown types and malformed field counts/values, enforce non-decreasing timestamps, and publish only a completely validated temporary history.

## 3. Durable Sampling Persistence

- [x] 3.1 Encode stable type-1 records with UTC millisecond timestamps and plain decimals, preserving blank/comment parsing semantics.
- [x] 3.2 Durably append before in-memory publication, including missing-final-line-separator handling and rejection of backward sampled timestamps.
- [x] 3.3 Isolate append/flush failures with instance, path, and sampling diagnostics so later fixed-delay attempts continue.

## 4. Verification and Review

- [x] 4.1 Compile main and test source sets without running or adding automated tests.
- [x] 4.2 Run strict validation for the active OpenSpec change and `git diff --check`.
- [x] 4.3 Review the complete diff for unrelated changes, behavioral gaps, generated Java edits, runtime data, and changes to Ticker/Raydium specifications or persistence.
