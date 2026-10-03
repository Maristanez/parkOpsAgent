# D1 plan: ParkOps

Due **Friday 9 October 2026, 23:59 Toronto time** · 10% of course · Route A (ParkOps)

Personal checklist. Not part of the submission, so leave it out of the ZIP.

## Fixture reference

| Space | Open | Occupied | Accessible | Eligible under policy v1 for B1 |
|---|---|---|---|---|
| A17 | No | Yes | No | No (B1's current space) |
| B12 | Yes | No | No | **Yes** |
| C03 | Yes | No | Yes | **Yes** |
| D09 | Yes | Yes | Yes | No (occupied) |

Booking B1: space A17, 3 hours, version 0, requires accessible. Policy version 1.

## 1. Domain model (D1.1, 20 pts)

- [x] `DomainRules.requireDuration`: 1..24 inclusive, else `IllegalArgumentException`
- [x] `DomainRules.requireIdentifier`: non-null and matches `[A-Z][A-Z0-9_-]{0,15}`, else `IllegalArgumentException`
- [ ] `Booking`: private fields `id`, `spaceId`, `durationHours`, `version` (≥ 0), `requiresAccessible`. Validate in the constructor. Getters only, no setters.
- [ ] `Space`: `id`, `open`, `occupied`, `accessible`. `isAvailable()` returns `open && !occupied`.
- [ ] `ProposalStatus` enum: `PENDING`, `APPROVED`, `REJECTED`, `EXECUTED`
- [ ] `Proposal`: `proposalId`, `bookingId`, `targetId`, `observedBookingVersion`, `observedPolicyVersion`, `status`. Starts `PENDING`. No public `setStatus`. Converts to `ProposalView`.

## 2. Executable path (D1.3, 25 pts)

- [ ] Policy v1 check in one place: target is not the current space AND `isAvailable()`. Ignore accessibility until D3.
- [ ] Rewrite `StudentApplication` to own one `Booking`, the spaces (private map keyed by id), the proposals (private) and a proposal-ID counter
- [ ] `bookingSnapshot()` returns a fresh map that keeps the `"spaceId"` and `"version"` keys
- [ ] `propose(targetId)`: validate id → look up space → check policy v1 → create and store a PENDING proposal with versions 0/1 → return its view. The booking never moves.
- [ ] Reject with `IllegalArgumentException`: D09 (occupied), unknown target (`Z99`), A17 (current space), malformed id
- [ ] Extend `Main` so `bash run.sh run scripted` also shows D09 and an unknown target being rejected

## 3. Tests (D1.4, 20 pts)

- [ ] JUnit tests in `test/junit/StudentTests.java`:
  - durations 1 and 24 accepted; 0 and 25 rejected
  - null and malformed identifiers rejected
  - B12 gives a PENDING proposal; booking still A17 / version 0
  - D09 rejected; booking and occupancy unchanged
  - unknown target rejected; no proposal created; booking unchanged
  - a snapshot cannot be used to change internal state
- [ ] Every rejection test asserts the unchanged state, not only that an exception was thrown
- [ ] `bash run.sh check` prints `CHECKS 14; FAILED 0`
- [ ] `bash run.sh test` passes
- [ ] Save real output (date, command, `java -version`) to `results/d1.txt`

## 4. Diagrams (D1.2, 20 pts)

- [ ] Class diagram: domain and application classes, visibility, associations, multiplicities. Use the realization arrow (dashed, hollow triangle) for interfaces, not the inheritance arrow.
- [ ] Sequence diagram: read B1 → evaluate B12 → create pending proposal, with the approval boundary labelled
- [ ] Both diagrams match the final code (draw them after the code settles)

## 5. DESIGN.pdf (D1.5, 10 pts)

At most 4 pages, not counting the diagrams. Name exact classes and methods.

- [ ] Responsibilities of each class
- [ ] One alternative considered and why it was rejected (e.g. immutable `Booking` vs. one controlled mutating method)
- [ ] Two informal operation contracts (e.g. `propose`, `requireIdentifier`): preconditions, postconditions, what may change
- [ ] The most important current limitation
- [ ] Which starter infrastructure was reused and which parts were written

## 6. Docs and submission (5 pts)

- [ ] `README.md`: JDK, exact commands, supported scenarios, known limitations, reused starter files, mapping from the brief's terms to your class and method names
- [ ] `TEST_PLAN.md`: each case D1-A to D1-F with precondition, input, expected result, actual result, test method
- [ ] `CHANGELOG.md`: what changed in D1
- [ ] `AI_USE.md`: tool, date, task, files affected, what was changed or rejected, how it was verified
- [ ] ZIP named `EECS3311_D1_STUDENTNUMBER.zip` with one top-level folder containing `src/ test/ run.sh README.md DESIGN.pdf AI_USE.md TEST_PLAN.md CHANGELOG.md results/`
- [ ] Leave out `build/`, `lib/`, `.git/` and this plan
- [ ] Unzip into a fresh folder and run `bash run.sh check` and `bash run.sh run scripted` from there

## Schedule

| Days | Work |
|---|---|
| Sat 3 – Sun 4 | Domain classes, `propose()`, `Main` demo, JUnit tests |
| Mon 5 – Tue 6 | Diagrams, DESIGN.pdf |
| Wed 7 – Thu 8 | Docs, results capture, ZIP, fresh-unzip test |
| Fri 9 | Buffer; submit before 23:59 |

## Rules not to break

- Don't edit the fixture or delete failing checks to make tests pass
- Don't approve or execute a proposal in D1
- Don't use the accessibility flag until D3
- Don't fabricate or hand-edit captured output
- Don't label replay output as a live model run
