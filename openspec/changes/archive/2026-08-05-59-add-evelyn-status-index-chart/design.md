## Context

Before this change, `Evelyn` declared `@NotNull String getStatusReport()`, `EvelynImpl` returned `"Hello World!"`, and no other local source called it. Mission Control presents parallel Production and Test tab sets. The API and reference implementations must remain separated by package.

## Goals / Non-Goals

**Goals:** Define a typed immutable measurement contract, replace the text API, and render each environment's Evelyn history in its corresponding existing Overview tab without disturbing the Production/Test tabs or styling.

**Non-Goals:** Real index calculation, persistence, refresh/polling after view creation, environment discovery, dependency resolution, final Nenjim environment wiring, and automated tests.

## Decisions

- Add `EvelynStatusIndexPoint` beside the Evelyn public API as an immutable value type. Use `Instant` for the timestamp and `BigDecimal` for each named index so time semantics are explicit and decimal values retain precision. A map or six-element list was rejected because it would lose compile-time names and completeness.
- Replace, rather than supplement, `getStatusReport()` with `@NotNull List<EvelynStatusIndexPoint> getStatusIndexHistory()`. The implementation returns an immutable hardcoded list in chronological order; callers must also tolerate an empty list. Keeping the text API was rejected because the issue intentionally replaces that contract.
- Make `EvelynMissionControlImpl` accept separate Production and Test Evelyn references. Fetch each reference's history while constructing its corresponding Overview so the two tabs remain independent even when the reference implementations currently return identical hardcoded values.
- Construct a distinct chart, axes, series, and data nodes for each Overview. Sharing a JavaFX `Node` is not valid because a node cannot have two parents.
- Have `NenjimHubImpl` create two separate `EvelynImpl` instances as temporary Production and Test placeholders and pass both to Mission Control. Future Nenjim wiring will supply the actual environment-specific Evelyn references; environment discovery and dependency resolution are outside issue #59.
- Use a time-capable numeric X-axis derived from each point's timestamp and a numeric Y-axis. Compute the largest absolute index magnitude across all series and use its negative and positive values as equal bounds; use a small symmetric fallback span when history is empty or all values are zero so zero remains visible. JavaFX chart construction remains in the reference implementation.

## Risks / Trade-offs

- [Hardcoded history can look authoritative] → Keep values clearly sample-only and isolate them in `EvelynImpl` for later replacement.
- [One extreme index compresses the other lines] → Accept a shared scale because direct comparison and a common zero baseline are required.
- [Breaking API removal affects downstream consumers outside this repository] → Publish the typed replacement in the same release and call out the method migration.

## Migration Plan

Add the value type, replace the interface method and reference implementation, construct distinct Production and Test Overview charts from their corresponding Evelyn references, and compile the project. Roll back the API and Overview changes together if needed; no stored data requires migration.
