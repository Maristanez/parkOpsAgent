# Change log

## Starter 2026-09-15
Supplied infrastructure and unfinished exercise boundary.

## Student changes
Record D1 implementation, D1 feedback fixes, D2 additions and D3 changes here.

### D1
- 2026-10-03: Implemented `DomainRules.requireDuration` (1–24 hours inclusive) and
  `DomainRules.requireIdentifier` (non-null, `[A-Z][A-Z0-9_-]{0,15}`). Both reject invalid
  input with `IllegalArgumentException`. Public duration and identifier checks now pass.
- 2026-10-03: Added `Booking` with validated id, space id, duration (1–24) and version (≥ 0);
  private final fields with getters and no setters. Added nine JUnit tests in `StudentTests`
  covering D1-A, D1-B, invalid identifiers, negative version and getters.
- 2026-10-03: Added `Space` with a validated id and open, occupied and accessible flags;
  private final fields with getters and no setters. `isAvailable()` returns open and not
  occupied, matching the brief's definition. Added seven JUnit tests in `StudentTests`
  covering availability (B12, D09, A17, closed but empty), invalid identifiers and getters.
- 2026-10-04: Added `ProposalStatus` enum (`PENDING`, `APPROVED`, `REJECTED`, `EXECUTED`) and
  `Proposal` with validated proposal, booking and target ids, observed booking version (≥ 0)
  and observed policy version (≥ 1). Status always starts `PENDING` and has no setter.
  `toView()` converts to the supplied `ProposalView`. Added seven JUnit tests in `StudentTests`
  covering the initial status, getters, invalid input and the view.
