## Purpose

Provide typed historical Evelyn status measurements and make all six indexes visible over time in Mission Control.

## ADDED Requirements

### Requirement: Evelyn provides historical status-index measurements
Evelyn SHALL expose a non-null list of typed measurement points ordered from oldest to newest. Each point SHALL contain a timestamp, Evelyn Price Index, EVE_SYRUP Pool Depth Index, EVE_SYRUP Pool Balance Index, AAZDKK_USDT Pool Balance Index, AAZDKK_USDT Pool Price Index, and AAZDKK_USDT Pool Depth Index; the list MAY be empty.

#### Scenario: Status-index history is requested
- **WHEN** a caller requests Evelyn's status-index history
- **THEN** Evelyn returns a non-null, oldest-to-newest list whose points contain a timestamp and all six index values

### Requirement: Evelyn Mission Control visualizes status-index measurements
Evelyn Mission Control SHALL display historical measurements from separate Production and Test Evelyn references in their corresponding Overview tabs as six-line graphs, with timestamp on the X-axis and index value on the Y-axis. Each environment SHALL retrieve its own history and SHALL have a separate graph instance with a dynamic Y-axis range that is symmetric around zero and always displays zero as the desired state. The two histories MAY contain identical values but SHALL remain separate data sources.

#### Scenario: Production and Test Overviews are created with history
- **WHEN** Mission Control creates the Production and Test Overview views
- **THEN** it retrieves history from the corresponding Production and Test Evelyn references and creates a separate six-line graph for each environment whose symmetric Y-axis contains zero

#### Scenario: An environment has empty history
- **WHEN** Mission Control creates an environment's Overview view and that environment's Evelyn history is empty
- **THEN** it displays an empty six-series graph for that environment whose Y-axis still contains zero without substituting the other environment's history
