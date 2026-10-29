# ParkOps plan: D1 and D2

Route A (ParkOps) · Personal checklist. Not part of the submission, so leave it out of the ZIP.

| Release | Due (Toronto time) | Weight | Status | Section |
|---|---|---|---|---|
| D1 | Friday 9 October 2026, 23:59 | 10% | Done | [D1 plan](#d1-plan-design-and-executable-path) (Steps 1–14) |
| D2 | **Friday 6 November 2026, 23:59** | 10% | **In progress** | [D2 plan](#d2-plan-working-supervised-agent) (parts D2.1–D2.5) |

D1 uses Steps 1–14. D2 is organised by the brief's parts, D2.1 to D2.5, with tasks numbered like 2.2.14.

## How to read this plan

Every item says where it comes from:

| Tag | Meaning | Source file |
|---|---|---|
| `[D1.1]`–`[D1.5]` | **Required.** A numbered section of the D1 brief | `../D1_Design_and_Executable_Path.pdf` |
| `[D1-A]`–`[D1-F]` | **Required.** A row of the D1 acceptance table | `../D1_Design_and_Executable_Path.pdf`, p.1 |
| `[D2.1]`–`[D2.5]` | **Required.** A numbered section of the D2 brief | `D2_Working_Supervised_Agent.pdf` (repo root: the amended copy, two amendment pages then the brief) |
| `[D2-A]`–`[D2-N]` | **Required.** A row of the D2 acceptance table | same file, brief p.1–2 |
| `[D2 intro]` | **Required.** The D2 brief's opening paragraph | same file, brief p.1 |
| `[D2 note]` | **Required.** The paragraph under the D2 acceptance table | same file, brief p.2 |
| `[D2 report]` | **Required.** "Report and submission additions" | same file, brief p.2 |
| `[Amendment]` | **Required.** The 18 September amendment pages | same file, amendment p.1–2 |
| `[Overview]` | **Required.** Project-wide rule | `../00_Project_Overview.pdf` |
| `[Starter]` | **Required by the starter code** (public checks, TODOs) | `test/plain/PublicChecks.java`, `src/StudentApplication.java`, `src/Main.java`, `START_HERE.md` |
| `[Suggestion]` | **Optional.** A design choice you may change | none |

---

# D1 plan: design and executable path

## What "done" means

The acceptance table from the D1 brief is the definition of done for the code:

| Case | Input | Required observation | Step |
|---|---|---|---|
| D1-A | `new Booking` with 1 or 24 hours | Object keeps that duration | 2 |
| D1-B | 0 or 25 hours | Clear rejection; no usable invalid booking | 2 |
| D1-C | B1, proposed B12, policy 1 | Pending proposal; booking still A17 / version 0 | 7 |
| D1-D | B1, proposed D09 | Rejection; occupancy and booking unchanged | 7 |
| D1-E | Unknown target | Controlled error; no proposal created, booking unchanged | 7 |
| D1-F | Snapshot obtained from outside | Caller cannot change protected internal state | 6 |

D1 is finished when all six cases pass in **your own** JUnit tests, the diagrams and DESIGN.pdf are written, and the ZIP runs from a fresh unzip.

## Fixture `[Overview]` p.2

| Space | Open | Occupied | Accessible | Eligible for B1 under policy v1 |
|---|---|---|---|---|
| A17 | No | Yes | No | No (B1's current space) |
| B12 | Yes | No | No | **Yes** |
| C03 | Yes | No | Yes | **Yes** |
| D09 | Yes | Yes | Yes | No (occupied) |

Booking B1: space A17, 3 hours, version 0, requires accessible. Policy version 1.
Do not change this fixture to make a test pass `[Overview]`.

---

## Step 1: Validation rules ✅

- [x] Duration must be 1..24 inclusive `[Overview]` `[D1.1]`
- [x] Identifier must be non-null and match `[A-Z][A-Z0-9_-]{0,15}` `[Overview]` `[D1.1]`
- [x] Both throw `IllegalArgumentException` `[Starter]`

Implemented in `src/DomainRules.java`.

## Step 2: `Booking` class ✅

- [x] Fields: id, assigned space id, duration, booking version, requires-accessible flag `[Overview]`
- [x] Constructor validates id, space id and duration before storing anything, so no invalid booking can exist `[D1.1]` `[D1-B]`
- [x] Version must be ≥ 0 `[Suggestion]`
- [x] Private final fields, getters, no setters `[Suggestion]` (the requirement is "controlled state access" `[D1.1]`)
- [x] Tests: durations 1 and 24 kept `[D1-A]`; 0 and 25 rejected `[D1-B]`; null and malformed ids rejected `[D1.4]`

Done when: `bash run.sh compile` prints `COMPILE_OK` and the tests pass.

## Step 3: `Space` class ✅

- [x] Fields: id, open, occupied, accessible `[Overview]`
- [x] "Available" means open and not occupied `[Overview]`; put it in an `isAvailable()` method `[Suggestion]`
- [x] Validate the id in the constructor `[D1.1]`
- [x] Tests: B12 available; A17 and D09 not available; invalid id rejected `[Suggestion]`

## Step 4: `Proposal` class and status ✅

- [x] Fields: proposal id, booking id, target space id, observed booking version, observed policy version, status `[Overview]`
- [x] Statuses: `PENDING`, `APPROVED`, `REJECTED`, `EXECUTED` `[Overview]`, as a `ProposalStatus` enum `[Suggestion]`
- [x] A new proposal starts as `PENDING`; no public way to set the status `[Suggestion]` (the model never supplies approval `[Overview]`)
- [x] Keep proposals separate from bookings: a proposal is not a completed change `[D1.1]`
- [x] Method that converts a `Proposal` to the supplied `ProposalView` record `[Starter]`
- [x] Tests: new proposal is `PENDING` and keeps the versions it was given; invalid ids rejected `[Suggestion]`

## Step 5: Policy v1 eligibility check ✅

- [x] Eligible means: target is not the booking's current space **and** target is available `[Overview]`
- [x] Ignore the accessibility flag; that is the D3 change `[Overview]`
- [x] Put the rule in one method with the policy version (1) beside it `[Suggestion]`
- [x] Tests: B12 and C03 eligible; A17 and D09 not `[Suggestion]`

## Step 6: `StudentApplication` storage and snapshot ✅

- [x] Replace the raw `Map` with your `Booking`, the spaces, and the stored proposals `[Starter]`
  - spaces in a private map keyed by id, proposals in a private list, a counter for proposal ids `[Suggestion]`
- [x] Never return an internal collection that callers could change `[D1.1]`
- [x] `bookingSnapshot()` returns a fresh map that keeps the `"spaceId"` and `"version"` keys `[Starter]`
- [x] Two `StudentApplication` objects do not share state ("separate object instances") `[D1.1]`
- [x] A read-only way to see a space's state and how many proposals exist, so tests can prove nothing changed `[Suggestion]`
- [x] Tests: changing a snapshot does not change the booking `[D1-F]`; two applications are independent `[D1.1]`

## Step 7: `propose(targetId)` ✅

- [x] Retrieve B1, check policy v1, record booking version 0 and policy version 1 `[D1.3]`
- [x] Create and store a `PENDING` proposal with an id the application generates `[D1.3]`, which must match the identifier pattern `[Starter]`
- [x] The booking stays at A17, version 0. Never approve or execute `[D1.3]`
- [x] Reject D09 and unknown targets without changing anything `[D1.3]`, using `IllegalArgumentException` `[Starter]`
- [x] Also reject A17 (current space) and malformed ids `[Suggestion]`
- [x] Tests, each checking **every** protected field afterwards, not just that an exception was thrown `[D1.4]` (the rubric says exception-only tests are insufficient):
  - B12 gives a `PENDING` proposal for B1 → B12 with versions 0/1; booking still A17 / 0 `[D1-C]`
  - D09 rejected; booking, D09's occupancy and proposal count unchanged `[D1-D]`
  - unknown target rejected; no proposal stored; booking unchanged `[D1-E]`

Done when: `bash run.sh check` prints `CHECKS 14; FAILED 0`.

## Step 8: Demonstration command ✅

- [x] `bash run.sh run scripted` feeds the scripted target B12 and displays a structured result `[D1.3]`
- [x] The same command also shows D09 and an unknown target being rejected `[D1.3]`; today it only runs B12, so extend `Main` `[Starter]`
- [x] After each attempt, print the booking to show it is still A17 / version 0 `[Suggestion]`
- [x] Keep the "not a live model run" label `[Overview]`

## Step 9: Capture evidence ✅

- [x] Save genuine output with date, command and environment to `results/d1.txt` `[Overview]` `[D1.4]`
- [x] It must show how the assessor runs the D1-A–F cases `[D1.4]`
- [x] Don't label plain checks as JUnit, or replay as a live model `[Overview]`; don't hand-edit output `[Starter]`

One way to capture everything at once `[Suggestion]`:

```
{
  echo "Date: $(date)"; uname -sr; java -version
  echo; echo '$ bash run.sh check'; bash run.sh check
  echo; echo '$ bash run.sh test'; bash run.sh test
  echo; echo '$ bash run.sh run scripted'; bash run.sh run scripted
} > results/d1.txt 2>&1
```

## Step 10: `TEST_PLAN.md` ✅

- [x] For each case D1-A to D1-F: preconditions, input, expected result, actual result, test method name `[Starter]`
- [x] For each rejection case, list the outcome **and** every field that stays unchanged `[Starter]`

## Step 11: Diagrams (after the code stops changing) ✅

- [x] **Class diagram:** your domain and application classes, visibility, important associations, multiplicities `[D1.2]`
- [x] Use the realization arrow (dashed line, hollow triangle) for interfaces, not the inheritance arrow `[D1.2]`
- [x] **Sequence diagram:** read B1 → evaluate B12 → create pending proposal `[D1.2]`
- [x] Mark the **approval boundary** on the sequence diagram, even though approval comes in D2 `[D1.2]`
- [x] Both match the submitted code and are readable in PDF `[D1.2]`
- [x] Drawn by `design/build.py` (SVG, printed to PDF by Chrome). Re-run `python3 design/build.py` after code changes so the diagrams keep matching `[Suggestion]`

## Step 12: `DESIGN.pdf` ✅

At most 4 pages, not counting the two diagrams if they are on their own pages `[D1.5]`. Refer to exact class and method names `[D1.5]`.

- [x] Responsibilities of each class `[D1.5]`
- [x] One alternative you considered and why you rejected it `[D1.5]` (e.g. immutable `Booking` vs. one controlled method that changes it `[Suggestion]`)
- [x] Two informal operation contracts: preconditions, postconditions, what may change `[D1.5]` (e.g. `propose`, `Booking` constructor `[Suggestion]`)
- [x] The most important current limitation `[D1.5]`
- [x] Which starter infrastructure you reused and which parts you wrote `[D1.5]`
- [x] Include the two diagrams `[D1.2]`

### Design decisions to write up

Recorded as they are made, so DESIGN.pdf has material ready. Each one can serve as the "alternative considered" `[D1.5]`.

| Decision | Where | Alternative rejected | Why |
|---|---|---|---|
| Proposal status is an enum with exactly `PENDING`, `APPROVED`, `REJECTED`, `EXECUTED` | `ProposalStatus`, `Proposal.getStatus()` | Store the status as a `String` | The compiler only accepts the four real statuses, so a typo like `"PENDNG"` cannot compile, and a proposal can never hold a status the brief doesn't define. `Proposal.toView()` converts it with `status.name()` because `ProposalView` needs a `String`. |
| A proposal's status is set to `PENDING` by its constructor, never passed in, and has no setter | `Proposal` constructor | Take the status as a constructor parameter or add `setStatus` | No caller can create an already-approved proposal; the brief says the model never supplies approval. D2 adds controlled transitions. |
| The eligibility rule lives in its own class with its version number beside it | `EligibilityPolicy.isEligible`, `getVersion` | An `if` inside `StudentApplication.propose`, or a method on `Space` | One place to change for policy v2 in D3; `propose` records `getVersion()` in each proposal; D2's list-alternatives step can reuse `isEligible`. |
| Callers get read-only copies: snapshot maps and `ProposalView` lists, never the domain objects or internal collections | `bookingSnapshot()`, `spaceSnapshot(String)`, `proposals()` | Return the `Booking`, `Space` and `List<Proposal>` directly | Callers cannot bypass validation or reorder/clear stored proposals (D1-F); once D2 makes occupancy change, the copies stay safe. |
| `propose` reports a rejected target with `IllegalArgumentException` | `StudentApplication.propose` | A result object with distinct outcomes | Matches the starter's public checks for D1; D2 needs distinct outcomes (`STALE`, `UNAVAILABLE_OR_INELIGIBLE`, …) and will replace it. Good "current limitation" for DESIGN.pdf. |

## Step 13: README, CHANGELOG, AI_USE

- [x] `README.md`: JDK version, exact commands, supported scenarios, known limitations, starter files reused `[Overview]`
- [x] `README.md`: how your class and method names map to the brief's terms `[Starter]`
- [x] `CHANGELOG.md`: one line per completed step `[Starter]`
- [x] `AI_USE.md`: a row for each AI-assisted task with tool, date, task, files, what you changed or rejected, and how you checked it `[Overview]`
  - still missing: the `DomainRules` row

## Step 14: Package and submit

- [x] ZIP named `EECS3311_D1_STUDENTNUMBER.zip` with a single top-level folder `[Overview]`
- [x] That folder contains `src/ test/ run.sh README.md DESIGN.pdf AI_USE.md TEST_PLAN.md CHANGELOG.md results/` `[Overview]`
- [x] Leave out `.class` files, IDE caches, secrets and downloaded libraries (`build/`, `lib/`) `[Overview]`
- [x] Also leave out `.git/`, `.DS_Store` and this plan; keep `setup-junit.sh` and `data/` `[Suggestion]`
- [x] Unzip into a fresh folder and run `bash run.sh check` and `bash run.sh run scripted` from its root `[Overview]`
- [ ] Submit on eClass before 23:59; keep the receipt and the ZIP `[Overview]`
- [ ] If anything is unfinished, submit anyway and name the affected requirement `[Overview]`

---

## Schedule `[Suggestion]`

| Day | Steps |
|---|---|
| Sat 3 – Sun 4 | 2–7: classes, `propose()`, tests |
| Mon 5 | 8–10: demo, evidence, test plan |
| Tue 6 – Wed 7 | 11–12: diagrams, DESIGN.pdf |
| Thu 8 | 13–14: docs, ZIP, fresh-unzip test |
| Fri 9 | Buffer; submit before 23:59 |

## Rubric `[D1 brief p.2]`

| Criterion | Points | Steps |
|---|---|---|
| Domain validity and encapsulation | 20 | 1–6 |
| Class and sequence diagrams | 20 | 11 |
| Executable proposal path | 25 | 7–8 |
| Test design and genuine results | 20 | 2–7, 9–10 |
| Design explanation and alternatives | 10 | 12 |
| Reproducibility and attribution | 5 | 13–14 |

---

# D2 plan: working supervised agent

Due **Friday 6 November 2026, 23:59 Toronto time** · 10% of course · about 18–22 hours of work

## The big picture

D2 turns D1's single `propose` call into a small supervised agent:

1. A **model** (or a replay script standing in for one) sends JSON requests such as `{"tool":"READ_BOOKING","bookingId":"B1","targetId":""}`.
2. **Java** checks each request, runs the tool, and sends the result back. It gives up after at most 6 tries.
3. The agent can only **propose** a move. A **human operator** approves or rejects it.
4. **Execute** re-checks everything, then moves B1 exactly once and records one notification.
5. A **tick** (pretend clock, every 60 seconds) spots bookings stuck on closed spaces.

The brief splits this into five parts, D2.1 to D2.5. Do them in this order, because each part uses the one before it:

| Order | Part | What you build | Points | When |
|---|---|---|---|---|
| 0 | [Setup](#0-setup) | Clean start, D1 feedback | – | Thu 29 Oct |
| 1 | [D2.4 first pass](#d24-first-pass-decide-the-classes) | Decide which classes you'll add (sketch only) | – | Thu 29 Oct |
| 2 | [D2.2](#d22-approval-and-execution--25-points) | Approve, reject, execute | 25 | Fri 30 – Sat 31 Oct |
| 3 | [D2.1](#d21-the-modeltool-loop--20-points) | The model/tool loop | 20 | Sun 1 – Mon 2 Nov |
| 4 | [D2.3](#d23-ticks--10-points) | The pretend clock | 10 | Mon 2 Nov |
| 5 | [D2.5](#d25-demo-live-run-evidence--30-points) | Demo, live run, saved output | 20 + 10 | Tue 3 Nov |
| 6 | [D2.4 second pass](#d24-second-pass-diagrams-and-designpdf--15-points) | Diagrams and DESIGN.pdf | 15 | Wed 4 Nov |
| 7 | [Submit](#submit) | README, ZIP, eClass | – | Thu 5 – Fri 6 Nov |

Tests are not a separate part. Every task that adds behaviour ends with its test, written in the same part. Testing is worth 20 points because those tests count across every part.

Task numbers follow the brief: task 2.2.14 is task 14 of D2.2.

## Acceptance cases at a glance

| Case | What happens | Expected | Task |
|---|---|---|---|
| D2-A | Replay: read, list, propose B12 | One `PENDING` proposal; B1 still A17 / version 0 | 2.1.14 |
| D2-B | Execute a pending proposal | `NOT_APPROVED`; nothing changed | 2.2.17 |
| D2-C | Approve, then execute | `COMMITTED`; B1 at B12 / version 1; one notification | 2.2.18 |
| D2-D | Execute the same proposal again | `DUPLICATE`; still one commit and one notification | 2.2.19 |
| D2-E | Reject, then execute | No commit; nothing changed | 2.2.20 |
| D2-F | B1's version changes after the proposal | `STALE`; no move | 2.2.21 |
| D2-G | Target closed or occupied after the proposal | `UNAVAILABLE_OR_INELIGIBLE`; no move | 2.2.22 |
| D2-H | Broken JSON, or an extra `"approved":true` | Rejected; counts as a try; nothing approved | 2.1.17 |
| D2-I | Unknown tool, or a booking other than B1 | Rejected; nothing else runs | 2.1.18 |
| D2-J | Six bad replies | `STEP_LIMIT` after exactly six calls | 2.1.19 |
| D2-K | Provider throws or times out | `PROVIDER_ERROR`; no commit | 2.1.20 |
| D2-L | Ticks 0, 59, 60, nothing changed | At most one work item | 2.3.8 |
| D2-M | Tick earlier than the last one | Clear clock error | 2.3.9 |
| D2-N | Real model run | Real transcript, or `LIVE_RUN_PENDING_COURSE_ACCESS` | 2.5.6 |

A model may pick C03 instead of B12. Both are allowed, so tests must accept either and must not check exact wording.

**"Nothing changed"** means checking all of these before and after: B1's space and version; the open and occupied flags of A17, B12, C03 and D09; every proposal's status; the number of commits; the number of notifications. Task 2.2.11 builds one helper that captures them all, so each test compares before against after.

---

## 0. Setup

**Why:** D2 extends D1, so start from a D1 that passes and is committed. Source: `[D2 intro]`.

- [ ] 0.1 Run `bash run.sh check`; expect `CHECKS 14; FAILED 0`
- [ ] 0.2 Run `bash run.sh test`; expect every test to pass
- [ ] 0.3 Commit the current `CHANGELOG.md` and plan changes
- [ ] 0.4 Check eClass for D1 feedback. List each defect below, fix it, and add a CHANGELOG line starting `D1 feedback:`
- [ ] 0.5 Check eClass for an announced course model endpoint (this decides task 2.5.6)
- [ ] 0.6 Add the missing `DomainRules` row to `AI_USE.md`

D1 feedback defects:

- `DESIGN.pdf` in the D1 ZIP was unreadable (blank pages): after Chrome printed it on 4 Oct, something re-saved it as text, replacing every non-ASCII byte. Rebuilt on 29 Oct with `python3 design/build.py`; the content is unchanged (`design/DESIGN.html` is identical to commit `07f55c4`). Add a CHANGELOG line, and ask the instructor on eClass whether the rebuilt PDF can be assessed for D1
- (no D1 feedback released yet)

---

## D2.4 first pass: decide the classes

**In plain words:** the brief wants four kinds of code kept apart: (1) reading the model's JSON, (2) running the workflow, (3) the parking rules, (4) where data is kept. Decide now which class does what, so later parts know where their code goes. About 1 hour. Source: `[D2.4]`.

Suggested classes (change any you like):

| Kind | Classes | Job |
|---|---|---|
| Model protocol | `Tool`, `ToolRequest`, `ToolRequestParser` | Turn reply text into a checked request, or a reason it was rejected |
| Coordination | `AgentRunner`, `StudentApplication` | Run the loop; operator actions; `tick` |
| Domain rules | `Booking`, `Space`, `Proposal`, `EligibilityPolicy`, `ExecutionOutcome` | The parking rules; know nothing about JSON or storage |
| Storage | `ParkingStore` | Hold the booking, spaces, proposals, commits and notifications |

- [ ] 2.4.1 Read the table and rename or split any class you'd design differently
- [ ] 2.4.2 Fill in the **Your choice** column below for at least the first four rows
- [ ] 2.4.3 Keep one rule from now on: domain classes never import parser, runner or store classes

Two of these decisions become the "two design decisions" in DESIGN.pdf:

| Question | Options | Suggested | Your choice |
|---|---|---|---|
| How a commit changes `Booking` | A new copy with `movedTo`, or change it in place with `moveTo` | New copy; keeps D1's read-only booking | |
| How `execute` reports a result | Return an `ExecutionOutcome` enum, or throw one exception per failure | Enum; the brief wants named outcomes | |
| How the parser reports bad input | Return a result value, or throw | Result value; the loop has to count it and keep going | |
| Where data lives | A `ParkingStore` class, or fields in `StudentApplication` | `ParkingStore`; D2.4 asks for separate storage | |
| How time reaches `tick` | `tick(long seconds)`, or an injected `Clock` | `tick(long seconds)`; simplest | |
| What `run scripted` shows | The D2 demo (rename the D1 demo), or the D1 demo (D2 under `agent-scripted`) | D2 demo; the brief names `scripted` | |

---

## D2.2 Approval and execution · 25 points

**In plain words:** a proposal waits as `PENDING`. A human calls `approve` or `reject`. Then `execute` checks everything again and either refuses with a named reason or moves B1 once. Executing it a second time must not move it again. Source: `[D2.2]`, `[Overview]` p.2–3.

**Done when:** tests for D2-B to D2-G pass, each checking either "nothing changed" or the exact changes.

### Part A: let the domain objects change, safely

- [ ] 2.2.1 `Proposal.approve()`: allowed only when `PENDING`, otherwise throw `IllegalStateException`
- [ ] 2.2.2 `Proposal.reject()`: allowed only when `PENDING`
- [ ] 2.2.3 `Proposal.markExecuted()`: allowed only when `APPROVED`
- [ ] 2.2.4 Test: each allowed change works; each disallowed one throws and leaves the status as it was
- [ ] 2.2.5 `Booking.movedTo(spaceId)` returns a new booking on that space with version + 1; the old object is unchanged
- [ ] 2.2.6 `Space` gains `occupy()`, `release()` and `close()`, needed for a commit and to set up D2-G
- [ ] 2.2.7 A way to raise B1's version without moving it, for D2-F, e.g. `changeDuration(hours)`
- [ ] 2.2.8 New enum `ExecutionOutcome`: `NOT_APPROVED`, `STALE`, `UNAVAILABLE_OR_INELIGIBLE`, `COMMITTED`, `DUPLICATE`

### Part B: store the data and make it visible to tests

- [ ] 2.2.9 Move the booking, spaces and proposals into `ParkingStore`; add a commit list and a notification list
- [ ] 2.2.10 Read-only `commitCount()` and `notifications()` on `StudentApplication`
- [ ] 2.2.11 Test helper `protectedState()` that returns everything in the "nothing changed" list as one value
- [ ] 2.2.12 Run `bash run.sh check` again; D1 must still pass

### Part C: the operator actions

- [ ] 2.2.13 `approve(proposalId)` and `reject(proposalId)` on `StudentApplication`; an unknown id gives a clear error
- [ ] 2.2.14 `execute(proposalId)` runs these checks in this order and stops at the first one that fails:

| # | Check | If it fails |
|---|---|---|
| 1 | The proposal exists and is for B1 | Clear error |
| 2 | Is it already `EXECUTED`? | `DUPLICATE`, change nothing |
| 3 | Is it anything other than `APPROVED`? | `NOT_APPROVED` |
| 4 | Has B1's version or the policy version changed since the proposal? | `STALE` |
| 5 | Is the target not eligible right now? | `UNAVAILABLE_OR_INELIGIBLE` |
| 6 | All passed | Commit, return `COMMITTED` |

- [ ] 2.2.15 A commit does exactly this: free A17, occupy the target, move B1 (version + 1), mark the proposal `EXECUTED`, add one commit record, add one notification record
- [ ] 2.2.16 Nothing inside `execute` calls the model or anything outside the program

### Part D: one test per case

- [ ] 2.2.17 D2-B: propose B12, execute → `NOT_APPROVED`; nothing changed
- [ ] 2.2.18 D2-C: propose, approve, execute → `COMMITTED`; B1 at B12 / version 1; A17 free; B12 occupied; 1 commit; 1 notification
- [ ] 2.2.19 D2-D: execute the same proposal again → `DUPLICATE`; still 1 commit and 1 notification
- [ ] 2.2.20 D2-E: propose, reject, execute → `NOT_APPROVED`; nothing changed
- [ ] 2.2.21 D2-F: propose, approve, raise B1's version, execute → `STALE`; B1 not moved
- [ ] 2.2.22 D2-G: propose, approve, occupy B12, execute → `UNAVAILABLE_OR_INELIGIBLE`; repeat with B12 closed instead
- [ ] 2.2.23 Commit ("D2.2: approval and execution") and add a CHANGELOG line

---

## D2.1 The model/tool loop · 20 points

**In plain words:** `run(provider, limit)` asks the provider for a reply, checks it, runs the tool, adds the result to the conversation, and asks again, up to `limit` times (1 to 6). It stops when a proposal is made (`AWAITING_APPROVAL`), the model says `FINISH` (`NO_ACTION`), the tries run out (`STEP_LIMIT`), or the provider fails (`PROVIDER_ERROR`). It never approves or executes. Source: `[D2.1]`, `[Overview]` p.2.

The four tools:

| Tool | Must come after | Returns |
|---|---|---|
| `READ_BOOKING` | – | B1's details and the policy version |
| `LIST_ALTERNATIVES` | a read | The spaces eligible right now (B12, C03) |
| `PROPOSE_REASSIGNMENT` | a read and a list | Creates one `PENDING` proposal, returns `AWAITING_APPROVAL` |
| `FINISH` | – | `NO_ACTION` |

**Done when:** tests for D2-A, D2-H, D2-I, D2-J and D2-K pass.

### Part A: check the reply (the parser, no loop yet)

- [ ] 2.1.1 `Tool` enum with the four tool names
- [ ] 2.1.2 `ToolRequest` record: `tool`, `bookingId`, `targetId`
- [ ] 2.1.3 `ToolRequestParser.parse(text)` returns either a request or a rejection reason. It rejects:
  - text that isn't valid JSON (the supplied `MiniJson` already catches this)
  - anything other than exactly the keys `tool`, `bookingId`, `targetId` (this is what rejects `"approved":true`)
  - a value that isn't a string
  - an unknown tool name
  - a `bookingId` other than `B1`
  - a non-empty `targetId` for read, list or finish; an unknown space for propose
- [ ] 2.1.4 Test each rejection above, plus one valid request for each tool

### Part B: the loop

- [ ] 2.1.5 `run` rejects a limit outside 1..6
- [ ] 2.1.6 The conversation starts with a system message giving the goal and the JSON format
- [ ] 2.1.7 Each turn: call the provider (one try) → save the reply as an `assistant` message → parse it → run the tool or note the rejection → save the result as a `user` message
- [ ] 2.1.8 A rejected reply is counted as invalid, and the loop keeps going while tries remain
- [ ] 2.1.9 Enforce the "must come after" column; a call out of order gets an error message back, not a crash
- [ ] 2.1.10 List and propose check that B1's version and the policy version haven't changed since the read
- [ ] 2.1.11 Propose reuses D1's `propose` logic
- [ ] 2.1.12 The provider throws (including a timeout) → `PROVIDER_ERROR`
- [ ] 2.1.13 `run` returns a result record: outcome, calls made, invalid count, proposal id, transcript

### Part C: tests

- [ ] 2.1.14 D2-A: `Fixture.replay()` → `AWAITING_APPROVAL`; one `PENDING` B1 → B12 proposal with versions 0/1; B1 still A17 / version 0
- [ ] 2.1.15 The second and third provider calls saw the earlier results (check `replay.histories()`)
- [ ] 2.1.16 The same run proposing C03 is accepted
- [ ] 2.1.17 D2-H: broken JSON, and a reply with `"approved":true` → each counted invalid; nothing approved
- [ ] 2.1.18 D2-I: an unknown tool, and `bookingId` `B2` → rejected; nothing changed
- [ ] 2.1.19 D2-J: six bad replies with limit 6 → `STEP_LIMIT`; exactly 6 calls; no proposal
- [ ] 2.1.20 D2-K: provider throws `IOException`, and separately `HttpTimeoutException` → `PROVIDER_ERROR`; nothing changed
- [ ] 2.1.21 Commit ("D2.1: model/tool loop") and add a CHANGELOG line

---

## D2.3 Ticks · 10 points

**In plain words:** `tick(seconds)` is a pretend clock. The first tick, and any tick at least 60 seconds after the last scan, looks for bookings on closed spaces (B1 on A17) and emits a "work item" for it. It never emits the same work twice, and a clock that goes backwards is an error. Nothing actually waits. Source: `[D2.3]`, `[Overview]` p.3.

**Done when:** tests for D2-L and D2-M pass.

- [ ] 2.3.1 `tick(long seconds)`: the time comes in as an argument; no `sleep` and no system clock
- [ ] 2.3.2 A time lower than the last tick → `IllegalArgumentException` saying the clock went backwards; nothing changes
- [ ] 2.3.3 Scan on the first tick, then only when `seconds >= lastScan + 60`
- [ ] 2.3.4 A scan finds bookings on closed spaces
- [ ] 2.3.5 Remember each (booking, booking version, policy version) already emitted, and don't emit it again
- [ ] 2.3.6 Also emit nothing while a `PENDING` or `APPROVED` proposal exists for that booking and versions
- [ ] 2.3.7 After a failed attempt, emit again only if the operator calls `retry(bookingId)` or a version changes
- [ ] 2.3.8 Test D2-L: ticks 0, 59, 60 → 1 item, then 0, then 0
- [ ] 2.3.9 Test D2-M: tick 60 then tick 30 → rejected; tick 120 afterwards still works
- [ ] 2.3.10 Tests: a pending proposal → no item; after a commit → no item; `retry` → one new item at the next scan
- [ ] 2.3.11 Commit ("D2.3: periodic ticks") and add a CHANGELOG line

---

## D2.5 Demo, live run, evidence · 30 points

**In plain words:** show everything working with commands the TA can run, keep the D1 tests passing, and either run a real model or record that course access isn't available yet. Covers "testing" (20) and "reproducibility" (10). Source: `[D2.5]`, `[D2 report]`.

- [ ] 2.5.1 Settle the `run scripted` decision from the D2.4 table
- [ ] 2.5.2 In `Main.java`, the D2 scripted demo prints the state after each step:
  1. ticks 0, 59, 60 → one work item, then none
  2. replay read → list → propose B12 → `AWAITING_APPROVAL`
  3. execute → `NOT_APPROVED`
  4. operator approves → execute → `COMMITTED`
  5. execute again → `DUPLICATE`
  6. tick 30 → clock rejected
- [ ] 2.5.3 Every replay output says "not a live model run"
- [ ] 2.5.4 `run live`: after the loop, ask the operator `approve P1? [y/N]`, defaulting to no; it never commits by itself
- [ ] 2.5.5 Run all the D1 tests again; they still pass
- [ ] 2.5.6 Live model:
  - endpoint announced → run it; save the model name, full transcript, the proposal and the operator's decision to `results/d2-live.txt`, with any credentials removed
  - not announced → `results/d2-live.txt` says `LIVE_RUN_PENDING_COURSE_ACCESS`, with the date and what you checked
  - never pay for access or fake a transcript
- [ ] 2.5.7 `results/d2.txt`: date, OS, `java -version`, then the output of `check`, `test` and `run scripted` (reuse the D1 Step 9 block with the file name changed)
- [ ] 2.5.8 The tick trace is in `results/` (inside the scripted output is fine)
- [ ] 2.5.9 `TEST_PLAN.md`: add D2-A to D2-N rows (preconditions, input, expected, actual, test name; for rejections, list what stays unchanged)

---

## D2.4 second pass: diagrams and DESIGN.pdf · 15 points

**In plain words:** draw what you built and explain your choices. Do this once the code stops changing. Source: `[D2.4]`, `[D2 report]`.

- [ ] 2.4.4 Class diagram: the new classes, with the four kinds of code visible
- [ ] 2.4.5 Sequence diagram: loop → pending proposal → approval boundary → approve → execute with re-checks → commit
- [ ] 2.4.6 New proposal-state diagram: `PENDING → APPROVED → EXECUTED` and `PENDING → REJECTED`; executing again gives `DUPLICATE` with no state change
- [ ] 2.4.7 Re-run `python3 design/build.py`
- [ ] 2.4.8 DESIGN.pdf, at most 5 pages plus diagrams, contains:
  - [ ] which class does what, and what depends on what
  - [ ] a state transition table (state × approve/reject/execute → result)
  - [ ] two design decisions, each with an alternative you rejected
  - [ ] one rejected model/tool interaction and what Java did about it
  - [ ] why failed work needs an operator retry (task 2.3.7)
  - [ ] limitations: one process, in memory, lost on restart, live-run status, accessibility still ignored
  - [ ] starter code reused, and D1 feedback fixed

---

## Submit

- [ ] S.1 README: JDK 21; commands (`run scripted`, `run live` with `OLLAMA_CHAT_URL` and `OLLAMA_MODEL`, the D1 demo); scenarios; limitations; how your names map to the brief's terms
- [ ] S.2 CHANGELOG: one line per part, with D1 fixes marked
- [ ] S.3 AI_USE: one row per AI-assisted task
- [ ] S.4 Build `EECS3311_D2_221524855.zip` in the course folder: same exclusions as D1, plus the repo-root `D2_Working_Supervised_Agent.pdf`
- [ ] S.5 From a fresh unzip: `bash run.sh check`, `bash run.sh run scripted`, `bash setup-junit.sh`, `bash run.sh test`, and open `DESIGN.pdf` to check every page shows text and diagrams
- [ ] S.6 Submit on eClass before 23:59 Friday 6 November; keep the receipt and the ZIP. If something is unfinished, submit anyway and name it

---

## Rules not to break

- Don't change the fixture to make a test pass `[Overview]`
- Don't delete failing tests or edit captured output `[Overview]` `[Starter]`
- Don't approve or execute a proposal in D1 `[D1.3]`
- Don't let the agent loop approve or execute, and never accept approval from model output `[D2.2]` `[Overview]`
- Don't rely on the prompt for a control; enforce it in Java `[D2.2]`
- Don't use the accessibility flag until D3 `[Overview]`
- Don't use `sleep` or real time for ticks `[D2.3]`
- Don't label replay output as a live model run, or plain checks as JUnit `[Overview]`
- Don't buy model access or fabricate a live transcript `[D2.5]` `[Overview]`
- Don't pre-fill `results/` `[Starter]`
- Don't break the D1 regression tests `[D2.5]`
