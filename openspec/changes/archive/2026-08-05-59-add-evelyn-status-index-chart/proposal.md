## Why

Evelyn exposes only an unstructured textual status report, which prevents Mission Control from plotting status changes over time. A typed historical API will make the six status indexes directly consumable by the existing Overview views.

## What Changes

- **BREAKING** Replace `Evelyn.getStatusReport()` with `List<EvelynStatusIndexPoint> getStatusIndexHistory()`.
- Add a typed measurement point containing a timestamp and the six Evelyn status indexes.
- Initially provide oldest-to-newest hardcoded sample history from the reference implementation.
- Give Mission Control separate Production and Test `Evelyn` references and plot each reference's history in its corresponding Overview tab using separate chart instances with dynamic Y-axes symmetric around a visible zero.

## Capabilities

### New Capabilities

- `evelyn-status-index-history`: Typed historical status-index access and Mission Control visualization.

### Modified Capabilities

None.

## Impact

- Affects the Evelyn public API and reference implementation in `com.fanitas.evelyn.core`.
- Affects Evelyn Mission Control's existing Overview content and introduces explicit Production/Test Evelyn wiring.
- Removes the textual status-report API; the repository currently has no callers beyond its declaration and implementation.
- Adds no persistence, real index calculation, environment discovery, dependency resolution, external dependency, or automated tests.
