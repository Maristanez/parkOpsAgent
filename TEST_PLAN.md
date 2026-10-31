# Test plan

## How the tests run

| Command | Runs | Framework |
|---|---|---|
| `bash run.sh check` | the 14 supplied public checks in `test/plain/PublicChecks.java` | plain Java (not JUnit) |
| `bash setup-junit.sh` (once), then `bash run.sh test` | `SuppliedChecksTest` (the same 14 checks), `InfrastructureTest` and `StudentTests` | JUnit Jupiter 6.0.3 |

Shared preconditions: every test builds fresh objects, so no test depends on another.
D2 rejection tests capture the **protected state** before and after with the helper
`StudentTests.protectedState(app)`: B1's snapshot, the snapshots of A17, B12, C03 and D09,
every proposal's status, the commit count and the notification count. "Unchanged" below means
that whole value is equal before and after.
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

## D2 acceptance cases

Replay providers are test doubles, not a model. A live model may choose C03 instead of B12; both
are eligible under policy 1, and the tests accept either where a model chooses.

| Case | Preconditions | Input | Expected result | Stays unchanged | Actual (2026-10-30) | Test methods |
|---|---|---|---|---|---|---|
| D2-A | fresh application | `run(Fixture.replay(), 6)`: read, list, propose B12 | `AWAITING_APPROVAL` after 3 calls, 0 invalid; one `PENDING` proposal B1 → B12 with versions 0/1 whose id is the result's `proposalId` | B1 at A17, version 0; A17 occupied; B12 free | Pass | `d2A_replayStopsAtPendingProposalWithoutMovingBooking`, `laterModelCallsSeeEarlierToolResults`, `proposingC03IsAlsoAccepted` |
| D2-B | one `PENDING` proposal for B12 | `execute(id)` | `NOT_APPROVED` | protected state; proposal still `PENDING` | Pass | `d2B_executingPendingProposalIsNotApprovedAndChangesNothing` |
| D2-C | `PENDING` proposal for B12, then `approve(id)` | `execute(id)` | `COMMITTED`; B1 at B12, version 1; A17 free; B12 occupied; proposal `EXECUTED`; 1 commit; 1 notification for B1 | C03 and D09 | Pass | `d2C_approvedProposalCommitsOnce` |
| D2-D | D2-C completed | `execute(id)` again | `DUPLICATE` | protected state after the commit: still 1 commit, 1 notification, B1 version 1 | Pass | `d2D_repeatedExecutionIsDuplicateWithNoSecondCommit` |
| D2-E | `PENDING` proposal, then `reject(id)` | `execute(id)` | `NOT_APPROVED`; no commit | protected state; B1 at A17; proposal `REJECTED` | Pass | `d2E_rejectedProposalNeverExecutes` |
| D2-F | approved proposal recorded booking version 0; then `changeBookingDuration(4)` makes B1 version 1 | `execute(id)` | `STALE` | protected state; B1 at A17; B12 free; proposal stays `APPROVED` | Pass | `d2F_changedBookingVersionMakesProposalStale`, `secondApprovedProposalIsStaleAfterFirstCommits` |
| D2-G | approved proposal for B12; then `occupySpace("B12")`, or separately `closeSpace("B12")` | `execute(id)` | `UNAVAILABLE_OR_INELIGIBLE` | protected state; B1 at A17, version 0; 0 commits | Pass | `d2G_occupiedTargetIsUnavailable`, `d2G_closedTargetIsUnavailable` |
| D2-H | fresh application | replies: malformed JSON; a proposal with `"approved":true`; then a valid read, list, propose | each bad reply answered `REJECTED` with its reason and counted: 5 calls, 2 invalid; the only proposal is the valid one, still `PENDING` | B1 at A17, version 0 | Pass | `d2H_malformedAndApprovedFieldAreCountedAndGrantNothing`, `parserRejectsMalformedJson`, `parserRejectsExtraApprovedField` and the other parser tests |
| D2-I | fresh application | replies: tool `DELETE_BOOKING`; `READ_BOOKING` for `B2`; `FINISH` | both rejected and counted (2 invalid); `NO_ACTION`; no proposal | B1 at A17, version 0 | Pass | `d2I_unknownToolAndWrongBookingRunNothing`, `parserRejectsUnknownTool`, `parserRejectsWrongGoalBooking` |
| D2-J | fresh application | six invalid replies (a seventh valid one is queued), limit 6 | `STEP_LIMIT`; exactly 6 provider calls; 6 invalid; no proposal | B1 at A17 | Pass | `d2J_sixInvalidResponsesStopAtStepLimit` |
| D2-K | fresh application | provider throws `IOException`; provider throws `HttpTimeoutException`; replay runs out mid-run | `PROVIDER_ERROR` each time; no proposal; no commit | B1 at A17, version 0 | Pass | `d2K_providerFailureEndsWithProviderError`, `providerFailureMidRunProposesNothing`, `interruptedProviderKeepsInterruptFlag` |
| D2-L | fresh application (B1 on closed A17) | `tick(0)`, `tick(59)`, `tick(60)` | one `WorkItem(B1, 0, 1, 0)`, then none (before the boundary), then none (boundary scan, pair already emitted). With a `retry` after tick 0, tick 59 still gives nothing and tick 60 gives one item, proving 59 does not scan and 60 does | no proposals; B1 at A17 | Pass | `d2L_boundaryTicksEmitAtMostOneWorkItem`, `tickBeforeBoundaryDoesNotScanButBoundaryTickDoes`, `scanBoundariesFollowTheSixtySecondGrid` |
| D2-M | `tick(60)` done | `tick(30)` | `IllegalArgumentException` ("clock went backwards") | tick state: `tick(119)` and `tick(120)` still behave as after `tick(60)` | Pass | `d2M_backwardsClockIsRejected` |
| D2-N | course-supported endpoint and model | `bash run.sh run live` | genuine transcript and permitted result | — | Pending: `LIVE_RUN_PENDING_COURSE_ACCESS` (no course endpoint configured) | `results/d2-live.txt` |

## Further D2 cases

| Area | Input | Expected result | Stays unchanged | Test methods |
|---|---|---|---|---|
| Proposal transitions | every status × `approve`, `reject`, `markExecuted` | only `PENDING → APPROVED/REJECTED` and `APPROVED → EXECUTED` succeed; every other call throws `IllegalStateException` | the status | `approvePendingStatusRequest`, `cannotApproveNonPendingRequest`, `rejectPendingStatusRequest`, `cannotRejectNonPendingRequest`, `executeApprovedStatusRequest`, `cannotExecutePendingRequest`, `cannotExecuteRejectedRequest`, `cannotRejectApprovedRequest`, `cannotApproveRejectedRequest`, `cannotApproveExecutedRequest`, `cannotRejectExecutedRequest`, `cannotMarkExecutedAnExecutedRequest` |
| Booking copies | `movedTo("C14")`; `movedTo("D18")` with lowercase/null ids; `withDuration(1/4/24/0/25)` | new booking with the new space or duration and version + 1; invalid values rejected | the original booking | `validMovedBooking`, `invalidMovedBookingSpaceId`, `withDurationReturnsCopyWithNewDurationAndVersion`, `withDurationKeepsTheOneToTwentyFourRule` |
| Space changes | `occupy`, `release`, `close`; `occupy` on a taken space | the one flag changes; `isAvailable()` follows; taken space throws | the other flags | `occupyTakesUpFreeSpace`, `cannotOccupyATakenSpace`, `releaseFreesSpace`, `closedSpaceNotAvailable` |
| Operator actions | approve/reject; unknown id; approve twice; reject an approved proposal; execute an unknown id | statuses change only from `PENDING`; unknown ids `IllegalArgumentException`; repeats `IllegalStateException` | protected state on every refusal | `operatorApprovesAndRejectsPendingProposals`, `operatorActionOnUnknownProposalIsRefusedAndChangesNothing`, `operatorCannotApproveTwiceOrRejectAnApprovedProposal`, `executingUnknownProposalIsRefusedAndChangesNothing`, `newApplicationHasNoCommitsOrNotifications` |
| Setup actions | `changeBookingDuration(4/25)`; `closeSpace`, `occupySpace` incl. unknown and taken spaces | version + 1 without moving; invalid input refused | protected state on refusal | `changeBookingDurationRaisesVersionWithoutMoving`, `changeBookingDurationRejectsInvalidHoursAndChangesNothing`, `closeAndOccupySpaceChangeOnlyThatSpace`, `setupActionsRefuseUnknownOrTakenSpaces` |
| Parser | each malformed shape; valid read, list, propose, finish | rejection with a reason, never an exception; valid requests accepted | — | `parserAccepts…`, `parserRejects…` (11 tests) |
| Loop rules | limit 0 and 7; `null` reply; list before read; propose before list; propose D09; versions changed between read and list, or between list and propose | limit refused; `null` counted invalid; out-of-order, ineligible and stale requests answered `TOOL_ERROR` (use a call, not invalid); after a stale answer the proposal records the fresh version | B1, D09 occupancy, proposals | `runRejectsLimitOutsideOneToSix`, `nullReplyCountsAsInvalid`, `outOfOrderToolsAreToolErrorsNotInvalidReplies`, `proposingIneligibleTargetIsToolErrorAndChangesNothing`, `staleVersionsAfterReadForceANewRead`, `staleVersionsBeforeProposeForceANewRead` |
| One active proposal per state | a second agent run proposing C03 while P1 is `PENDING`; a new run after P1 is rejected | `TOOL_ERROR` naming P1, `NO_ACTION`, still one proposal; after rejection a new `PENDING` proposal is allowed | the existing proposal | `agentRefusesSecondActiveProposalForSameState`, `agentMayProposeAgainAfterRejection` |
| Tick suppression and retry | pending proposal; after a commit; `retry("B1")`; version change; rejected proposal; `tick(-1)`; `retry("B9")` | no work while a proposal is active or B1 is on an open space; retry or a new version emits once more; negative time and unknown booking refused | tick state on refusal | `pendingProposalSuppressesWork`, `committedBookingIsNoLongerAffected`, `retryOrVersionChangeAllowsWorkAgain`, `rejectedProposalDoesNotSuppressRetriedWork`, `tickRejectsNegativeTimeAndRetryRejectsUnknownBooking` |

## Results

D1, recorded on 2026-10-04 (`results/d1.txt`): `check` 14 passed; `test` 55 of 55.

D2, recorded on 2026-10-30 with OpenJDK 21.0.12 on macOS (Darwin 25.5.0). Full output with
dates and commands is in `results/d2.txt`; the live-model status is in `results/d2-live.txt`.

- `bash run.sh check`: `CHECKS 14; FAILED 0`
- `bash run.sh test`: 129 tests found, 129 successful, 0 failed (114 in `StudentTests`, all D1 tests included)
- `bash run.sh run scripted`: shows D2-A, B, C, D, E, F, G, J, L and M with the state after each step
