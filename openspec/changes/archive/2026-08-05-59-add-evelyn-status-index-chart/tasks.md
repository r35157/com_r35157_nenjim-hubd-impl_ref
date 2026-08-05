## 1. Typed Status History

- [x] 1.1 Add the immutable `EvelynStatusIndexPoint` API value type with an `Instant` timestamp and six named `BigDecimal` index values.
- [x] 1.2 Replace `Evelyn.getStatusReport()` with the non-null `getStatusIndexHistory()` list contract.
- [x] 1.3 Implement an immutable, oldest-to-newest hardcoded sample history in `EvelynImpl`.

## 2. Mission Control Overview

- [x] 2.1 Update Mission Control to accept separate Production and Test Evelyn references and fetch each reference's history for its corresponding Overview tab.
- [x] 2.2 Update NenjimHub to supply two separate `EvelynImpl` placeholder instances and keep distinct six-series chart instances for the two histories.
- [x] 2.3 Configure a dynamic symmetric Y-axis around zero, including a usable zero-containing fallback range for empty or all-zero history.

## 3. Verification

- [x] 3.1 Compile the project and manually confirm each Overview tab renders its corresponding Evelyn history in a separate chart without adding automated tests.
