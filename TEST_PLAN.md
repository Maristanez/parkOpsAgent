# Test plan

## How the tests run

| Command | Runs | Framework |
|---|---|---|
| `bash run.sh check` | the 14 supplied public checks in `test/plain/PublicChecks.java` | plain Java (not JUnit) |
| `bash setup-junit.sh` (once), then `bash run.sh test` | `SuppliedChecksTest` (the same 14 checks), `InfrastructureTest` and `StudentTests` | JUnit Jupiter 6.0.3 |

Shared preconditions: every test builds fresh objects, so no test depends on another.
A `StudentApplication` starts from the supplied fixture: booking B1 at A17, 3 hours,
version 0, requires an accessible space; spaces A17 (closed, occupied), B12 (open, free),
C03 (open, free, accessible), D09 (open, occupied, accessible); policy version 1.

## D1 acceptance cases

| Case | Preconditions | Input | Expected result | Stays unchanged | Actual (2026-10-04) | Test methods |
|---|---|---|---|---|---|---|
| D1-A | none | `new Booking("B1", "A17", 1, 0, true)`; same with 24 | `getDurationHours()` returns 1; returns 24 | — | Pass | `StudentTests.bookingKeepsDurationOne`, `bookingKeepsDurationTwentyFour`; public check "duration endpoints" |
| D1-B | none | duration 0; duration 25 (public checks also try -1) | `IllegalArgumentException`; no `Booking` object is created | — | Pass | `bookingRejectsDurationZero`, `bookingRejectsDurationTwentyFive`; public checks "invalid duration 0 / 25 / -1" |
| D1-C | fresh application | `propose("B12")` | `ProposalView` with an application-generated id matching the identifier pattern, booking B1, target B12, booking version 0, policy version 1, status `PENDING`; exactly one stored proposal | booking snapshot (A17, version 0, all fields); B12 still not occupied; A17 still occupied | Pass | `proposeB12CreatesPendingProposalAndBookingStaysAtA17`; public check "D1-C pending B12, no booking change" |
| D1-D | fresh application | `propose("D09")` | `IllegalArgumentException` ("not eligible under policy version 1") | booking snapshot (A17, version 0); D09 snapshot (still occupied); no proposals stored | Pass | `proposeD09IsRejectedAndNothingChanges`; public check "D1-D occupied D09 rejected unchanged" |
| D1-E | fresh application | `propose("Z99")` | `IllegalArgumentException` ("Unknown target space") | booking snapshot (A17, version 0); no proposals stored | Pass | `proposeUnknownTargetIsRejectedAndNothingChanges`; public check "D1-E unknown target rejected unchanged" |
| D1-F | fresh application | `bookingSnapshot().put("spaceId", "D09")`; `proposals().clear()` after one proposal | `UnsupportedOperationException` for both | booking still at A17; still one stored proposal | Pass | `bookingSnapshotCannotChangeTheBooking`, `proposalListCannotBeModifiedByCallers`; public check "D1-F protected snapshot" |

## Further cases required by D1.4 and the domain rules

| Area | Input | Expected result | Stays unchanged | Test methods |
|---|---|---|---|---|
| Invalid identifiers | `null`, `""`, `"b1"`, `"1B"`, 17 characters | `IllegalArgumentException` | — | public checks "invalid identifier …"; `bookingRejectsNullId`, `bookingRejectsLowercaseId`, `bookingRejectsNullSpaceId`, `spaceRejectsNullId`, `spaceRejectsLowercaseId`, `proposalRejectsNullId`, `proposalRejectsLowercaseTarget` |
| Valid identifiers | `"B1"`, `"A_1-2"`, 16 characters | accepted | — | public check "valid identifiers" |
| Booking fields | version -1; valid booking | rejected; getters return constructor values | — | `bookingRejectsNegativeVersion`, `bookingKeepsAllFields` |
| Space availability | B12, D09, A17, closed-but-empty space | available only when open and not occupied | — | `spaceB12IsAvailable`, `spaceD09IsNotAvailableBecauseOccupied`, `spaceA17IsNotAvailableBecauseClosedAndOccupied`, `spaceClosedButEmptyIsNotAvailable`, `spaceKeepsAllFields` |
| Proposal | new proposal; invalid versions; view | starts `PENDING`; booking version < 0 and policy version < 1 rejected; view carries the same values | — | `newProposalIsPending`, `proposalKeepsAllFields`, `proposalRejectsNegativeBookingVersion`, `proposalRejectsPolicyVersionZero`, `proposalViewMatchesProposal` |
| Policy v1 | B1 with B12, C03, D09, A17; booking already at an available B12 | B12 and C03 eligible (accessibility ignored in v1); D09, A17 and the current space rejected; version 1 | — | `policyAcceptsB12EvenThoughB1NeedsAccessible`, `policyAcceptsC03`, `policyRejectsOccupiedD09`, `policyRejectsCurrentSpaceA17`, `policyRejectsCurrentSpaceEvenWhenAvailable`, `policyVersionIsOne` |
| Other rejected targets | `propose("A17")`; `propose(null)`; `propose("b12")` | `IllegalArgumentException` | booking snapshot; no proposals stored | `proposeCurrentSpaceA17IsRejectedAndNothingChanges`, `proposeMalformedTargetIsRejectedAndNothingChanges` |
| Proposal ids | `propose("B12")` then `propose("C03")` | two different application-generated ids, both stored in order | — | `eachProposalGetsItsOwnApplicationGeneratedId` |
| Application state | new application; unknown space lookup; two applications | starts from the fixture; unknown space rejected; proposals in one do not appear in the other | the other application's proposals | `applicationStartsFromFixture`, `spaceSnapshotRejectsUnknownSpace`, `applicationsAreIndependent` |

## Results

Recorded on 2026-10-04 with OpenJDK 21.0.12 on macOS (Darwin 25.5.0).
Full output with dates and commands is in `results/d1.txt`.

- `bash run.sh check`: `CHECKS 14; FAILED 0`
- `bash run.sh test`: 55 tests found, 55 successful, 0 failed

D2 and D3 acceptance cases will be added to this plan with those deliverables.
