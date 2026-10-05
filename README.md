# ParkOps: supervised parking-operations agent

EECS 3311 B, Fall 2026 · Individual project · Route A (ParkOps) · Deliverable 1

D1 builds the validated domain model and the executable proposal path. Given the supplied
fixture, the application reads booking B1, checks a target space against policy version 1,
and creates a `PENDING` proposal. The booking itself never moves. Ineligible and unknown
targets are rejected without changing any state. Approval and execution come in D2.

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
| `bash run.sh compile` | Compiles `src/` with `-Xlint:all` |
| `bash run.sh run scripted` | **D1 demonstration.** Replays three scripted proposal requests: B12 (accepted as `PENDING`), D09 (rejected, occupied) and Z99 (rejected, unknown). Prints the booking after each one. This is replay, not a live model run. |
| `bash run.sh run fixture` | Prints the synthetic fixture |
| `bash run.sh check` | The 14 supplied public checks, as plain Java (not JUnit) |
| `bash setup-junit.sh` | Once per machine: downloads and verifies the JUnit jar |
| `bash run.sh test` | JUnit Jupiter: the supplied checks, the infrastructure tests and `StudentTests` |

Expected results: `check` prints `CHECKS 14; FAILED 0`; `test` reports 55 tests, 0 failed;
`run scripted` ends with one stored proposal and booking B1 still at A17, version 0.

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

## Where the brief's terms live in the code

| Brief term | Code |
|---|---|
| Identifier and duration rules | `DomainRules.requireIdentifier(String)`, `DomainRules.requireDuration(int)` |
| Booking | `Booking` (validated in its constructor; getters only) |
| Space, "available" | `Space`, `Space.isAvailable()` (open and not occupied) |
| Proposal and its statuses | `Proposal`, `ProposalStatus` (`PENDING`, `APPROVED`, `REJECTED`, `EXECUTED`) |
| Policy version 1 | `EligibilityPolicy.isEligible(Booking, Space)`, `EligibilityPolicy.getVersion()` |
| Propose a reassignment | `StudentApplication.propose(String)`, returning the supplied `ProposalView` record |
| Rejected target | `IllegalArgumentException` from `propose` (the starter's API) |
| Application-generated proposal id | `P1`, `P2`, … in creation order |
| Protected snapshots | `StudentApplication.bookingSnapshot()`, `spaceSnapshot(String)`, `proposals()`; all read-only |

## Known limitations

- **D1 stops at a pending proposal.** Operator approval, rejection, execution, duplicate
  protection and the periodic `tick(time)` are D2 work. Nothing reads the stored proposals yet.
- **No model/tool loop yet.** `StudentApplication.run` is still the D2 TODO, so the
  `agent-scripted` and `live` modes print `STARTER_NOT_IMPLEMENTED`.
- **Policy v1 ignores accessibility, as the brief requires.** B12 is eligible even though B1
  requires an accessible space. Policy version 2 (D3) will change this.
- **All rejections are the same exception type.** D09, Z99, A17 and malformed ids all raise
  `IllegalArgumentException`, told apart only by the message. D2 introduces distinct outcomes.
- **One fixture, in memory, single process.** No persistence across restarts and no safety
  under concurrent access.

## Starter files reused

Unchanged from the 2026-09-15 starter (see `STARTER_PROVENANCE.md`): `ChatMessage`,
`Fixture`, `MiniJson`, `ModelProvider`, `OllamaProvider`, `ProposalView`, `ReplayProvider`,
`run.sh`, `run.ps1`, `setup-junit.sh`, `setup-junit.ps1`, `data/`, `test/plain/PublicChecks.java`,
`test/plain/InfrastructureChecks.java`, `test/junit/SuppliedChecksTest.java` and
`test/junit/InfrastructureTest.java`.

Starter files completed or changed:
- `DomainRules`: the two TODO validators are implemented.
- `StudentApplication`: the raw fixture map is replaced by domain objects; `propose` is
  implemented; `run` remains the D2 TODO.
- `Main`: `scripted` mode now demonstrates B12, D09 and an unknown target.
- `test/junit/StudentTests.java`: 40 tests added.

New classes: `Booking`, `Space`, `Proposal`, `ProposalStatus`, `EligibilityPolicy`.

## Evidence

- `results/d1.txt`: dated output of `check`, `test` and `run scripted`
- `TEST_PLAN.md`: how each D1 acceptance case maps to tests
- `DESIGN.pdf`: design explanation and diagrams
