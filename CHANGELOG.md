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
- 2026-10-04: Added `EligibilityPolicy` (policy version 1): a target is eligible when it is not
  the booking's current space and is available. The accessibility flag is deliberately unused
  until D3. Added six JUnit tests, including the current space being rejected even when available.
- 2026-10-04: Replaced `StudentApplication`'s raw fixture map with `Booking`, `Space` and
  `Proposal` objects built from `Fixture`. Implemented `propose(String)`: it validates the target,
  rejects unknown and ineligible targets with `IllegalArgumentException`, and stores a `PENDING`
  proposal with an application-generated id and observed booking/policy versions 0/1, without
  moving the booking. Added read-only `bookingSnapshot()`, `spaceSnapshot(String)` and
  `proposals()`. Added eleven JUnit tests covering D1-C to D1-F, other rejected targets and
  independent instances. All 14 public checks now pass.
- 2026-10-04: `bash run.sh run scripted` now demonstrates B12 (pending proposal), D09 and Z99
  (rejected), printing the booking after each attempt.
- 2026-10-04: Wrote `README.md`, `TEST_PLAN.md` and `results/d1.txt` for D1.
