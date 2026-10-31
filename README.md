# ParkOps: supervised parking-operations agent

EECS 3311 B, Fall 2026 · Individual project · Route A (ParkOps) · Deliverable 2

D2 makes the D1 proposal path into a supervised agent. A bounded model/tool loop reads
booking B1, lists the eligible spaces and creates one `PENDING` proposal, then stops. Only
separate operator commands approve, reject or execute it. Execution re-checks the booking
version, policy version, target eligibility and proposal identity, then commits once and
records one simulated notification; repeating it returns `DUPLICATE`. A periodic
`tick(time)` with caller-supplied time detects bookings on closed spaces. Policy version 1
is still in force.

## Requirements

- **JDK 21.** Tested with OpenJDK 21.0.12 on macOS (Darwin 25.5.0). `COURSE_JAVA_HOME` or
  `JAVA_HOME` may point to a JDK 21 folder; the scripts always compile with `--release 21`.
- **JUnit Jupiter 6.0.3** console standalone jar, for `bash run.sh test` only.
  `bash setup-junit.sh` downloads it into `lib/` and checks its SHA-256, or set `JUNIT_JAR`
  to a TA-provided copy.
- **bash** (macOS, Linux, or Git Bash on Windows). PowerShell users can replace `bash run.sh`
  with `./run.ps1` and `bash setup-junit.sh` with `./setup-junit.ps1`.

## Commands

Run from the project root:

| Command | What it does |
|---|---|
| `bash run.sh run scripted` | **D2 demonstration** (replay, not a live model run). Ticks 0, 59, 60; the replayed agent run read → list → propose B12 stopping at `AWAITING_APPROVAL`; execute before approval (`NOT_APPROVED`); operator approve then execute (`COMMITTED`); execute again (`DUPLICATE`); a backwards tick (rejected); then the rejected, stale, unavailable and six-invalid-replies paths. Prints the protected state after each step. |
| `bash run.sh run agent-scripted` | Alias of `scripted` |
| `bash run.sh run scripted-d1` | The D1 regression demonstration: B12 accepted as `PENDING`, D09 and Z99 rejected |
| `OLLAMA_CHAT_URL=… OLLAMA_MODEL=… bash run.sh run live` | **Live model run** through the same loop. Prints the transcript and state, then asks the operator: `y` approves and executes, `r` rejects, anything else (or no input) leaves the proposal `PENDING`. Never commits without that answer. Without both variables it prints `LIVE_RUN_PENDING_COURSE_ACCESS` and exits with status 2. Use only the course-supported endpoint; `OLLAMA_CHAT_URL` is the full URL ending `/api/chat`. |
| `bash run.sh run fixture` | Prints the synthetic fixture |
| `bash run.sh compile` | Compiles `src/` with `-Xlint:all` |
| `bash run.sh check` | The 14 supplied public checks, as plain Java (not JUnit) |
| `bash setup-junit.sh` | Once per machine: downloads and verifies the JUnit jar |
| `bash run.sh test` | JUnit Jupiter: the supplied checks, the infrastructure test and `StudentTests` |

Expected results: `check` prints `CHECKS 14; FAILED 0`; `test` reports 129 tests, 0 failed;
`run scripted` ends step 5 with B1 at B12, version 1, one commit and one notification.

## Supported scenario

Only the supplied fixture is supported. It is loaded from `Fixture` into fresh domain
objects each time a `StudentApplication` is created.

| Space | Open | Occupied | Accessible | Eligible for B1 under policy v1 |
|---|---|---|---|---|
| A17 | No | Yes | No | No (B1's current space) |
| B12 | Yes | No | No | Yes |
| C03 | Yes | No | Yes | Yes |
| D09 | Yes | Yes | Yes | No (occupied) |

Booking B1: space A17, 3 hours, version 0, requires an accessible space. Policy version 1.
The goal given to the model is "Resolve the booking affected by the closure of A17."

## Where the brief's terms live in the code

| Brief term | Code |
|---|---|
| Four-tool protocol | `Tool` (`READ_BOOKING`, `LIST_ALTERNATIVES`, `PROPOSE_REASSIGNMENT`, `FINISH`), `ToolRequest` |
| Strict request validation | `ToolRequestParser.parse(text, goalBookingId, knownSpaceIds)`: exactly three string properties, a known tool, goal booking B1, `targetId` empty except for a proposal naming a known space |
| Model/tool loop, observation history | `StudentApplication.run(ModelProvider, int)` → `AgentRunner`; returns `AgentResult` (outcome, calls, invalid responses, proposal id, transcript) |
| 1–6 call limit | `run` rejects other limits with `IllegalArgumentException`; every provider call counts |
| Loop outcomes | `AgentOutcome`: `AWAITING_APPROVAL`, `NO_ACTION`, `STEP_LIMIT`, `PROVIDER_ERROR` |
| One active proposal per state | The propose tool answers `TOOL_ERROR` while a `PENDING` or `APPROVED` proposal exists for B1's current version and policy version; ticks emit no work item for that pair either |
| Invalid response vs. tool-validation error | Parser rejection → `REJECTED` observation, counted in `invalidResponses`; a valid request that breaks a tool rule (order, stale versions, ineligible target) → `TOOL_ERROR` observation, not counted as invalid |
| Replay mode / model-connected mode | `ReplayProvider` (test double) / `OllamaProvider` (supplied adapter), both `ModelProvider` |
| Operator approve / reject | `StudentApplication.approve(String)`, `reject(String)`; legal only from `PENDING` (`IllegalStateException` otherwise); unknown id `IllegalArgumentException` |
| Execute with re-checks | `StudentApplication.execute(String)` returning `ExecutionOutcome` |
| `NOT_APPROVED`, `STALE`, `UNAVAILABLE_OR_INELIGIBLE`, `COMMITTED`, `DUPLICATE` | `ExecutionOutcome` constants of the same names |
| Commit record, simulated notification | `CommitRecord`, `Notification`; `commitCount()`, `notifications()` |
| Changing state after a proposal (for STALE / unavailable cases) | `changeBookingDuration(int)` (raises B1's version), `closeSpace(String)`, `occupySpace(String)` |
| Periodic tick, work item, operator retry | `StudentApplication.tick(long seconds)` → `List<WorkItem>`; `retry(String bookingId)`; implemented by `PeriodicChecker`. The first tick scans; then the first tick at or past each 60-second boundary (first + 60, first + 120, …) scans |
| Backwards clock | `tick` throws `IllegalArgumentException("clock went backwards: …")` |
| Data storage | `ParkingStore` (package-private) |
| Protected snapshots | `bookingSnapshot()`, `spaceSnapshot(String)`, `proposals()`, `notifications()`: copies only |
| D1 terms | `DomainRules`, `Booking`, `Space`, `Proposal`, `ProposalStatus`, `EligibilityPolicy`, `propose(String)` as in D1 |

## Known limitations

- **No live-model transcript yet.** No course-supported endpoint was configured, so
  `results/d2-live.txt` records `LIVE_RUN_PENDING_COURSE_ACCESS`. The live command is
  implemented and will be run in the announced supported session.
- **One fixture, in memory, single process.** Nothing persists across restarts; the
  check-then-commit in `execute` is sequential only, not safe under concurrent callers.
- **Policy version never changes in D2,** so only the booking-version half of `STALE` is
  exercised. Accessibility is still ignored, as policy version 1 requires.
- **Work items are reported, not acted on.** `tick` returns them; starting an agent run for
  one is an operator decision. After a failed attempt the same booking/policy versions are
  not re-emitted until the operator calls `retry` or a version changes.
- **D1's `propose` still throws `IllegalArgumentException`** for a bad target, because the
  supplied public checks expect it; the agent loop turns it into a tool error.

## Starter files reused

Unchanged from the 2026-09-15 starter (see `STARTER_PROVENANCE.md`): `ChatMessage`,
`Fixture`, `MiniJson`, `ModelProvider`, `OllamaProvider`, `ProposalView`, `ReplayProvider`,
`run.sh`, `run.ps1`, `setup-junit.sh`, `setup-junit.ps1`, `data/`, `test/plain/PublicChecks.java`,
`test/plain/InfrastructureChecks.java`, `test/junit/SuppliedChecksTest.java` and
`test/junit/InfrastructureTest.java`.

Starter files completed or changed:
- `DomainRules`: the two TODO validators (D1).
- `StudentApplication`: domain objects, `propose` (D1); `run`, operator actions, `tick`,
  `retry`, setup actions and storage through `ParkingStore` (D2).
- `Main`: `scripted` is the D2 demonstration (`agent-scripted` is its alias), the D1
  demonstration moved to `scripted-d1`, and `live` asks the operator before any commit.
- `test/junit/StudentTests.java`: 114 tests.

New classes: `Booking`, `Space`, `Proposal`, `ProposalStatus`, `EligibilityPolicy` (D1);
`ExecutionOutcome`, `ParkingStore`, `CommitRecord`, `Notification`, `Tool`, `ToolRequest`,
`ToolRequestParser`, `AgentRunner`, `AgentResult`, `AgentOutcome`, `PeriodicChecker`,
`WorkItem` (D2).

## Evidence

- `results/d2.txt`: dated output of `compile`, `check`, `test` and `run scripted` (includes the tick trace)
- `results/d2-live.txt`: the live-model status (`LIVE_RUN_PENDING_COURSE_ACCESS`)
- `results/d1.txt`: the D1 evidence, unchanged
- `TEST_PLAN.md`: how each D1 and D2 acceptance case maps to tests
- `DESIGN.pdf`: the D2 design explanation and five diagrams (built by `python3 design/build.py`)
- `design/DESIGN_D1.pdf`: the D1 design document, kept separately for reference (built by `python3 design/build_d1.py`)
