# ParkOps plan

Route A (ParkOps) · Personal checklist. Not part of the submission, so leave it out of the ZIP.

| Release | Due (Toronto time) | Weight | Status |
|---|---|---|---|
| D1 | Friday 9 October 2026, 23:59 | 10% | Done ([archive](#archive-d1-plan-done)) |
| **D2** | **Friday 6 November 2026, 23:59** | 10% | **In progress** (this page) |

---

# D2: working supervised agent

## Where things stand

| # | Part | Points | Done | Next |
|---|---|---|---|---|
| 1 | [D2.2 Approval and execution](#1-d22-approval-and-execution--25-points) | 25 | 21 / 22 | Commit |
| 2 | [D2.1 Model/tool loop](#2-d21-the-modeltool-loop--20-points) | 20 | 20 / 21 | Commit |
| 3 | [D2.3 Ticks](#3-d23-ticks--10-points) | 10 | 10 / 11 | Commit |
| 4 | [D2.5 Demo and evidence](#4-d25-demo-live-run-evidence--30-points) | 30 | 5 / 5 | Live run when the course announces an endpoint |
| 5 | [D2.4 Diagrams and DESIGN.pdf](#5-d24-diagrams-and-designpdf--15-points) | 15 | 6 / 6 | Confirm the [decision table](#design-decisions) choices |
| 6 | [Submit](#6-submit) | – | 2 / 5 | **S.3–S.5: build the ZIP, check it from a fresh unzip, submit (you)** |

**QA on 30 Oct:** 129 / 129 tests and 14 / 14 checks pass (also from a trial ZIP and fresh unzip); an independent review against the D2 brief found no blocking defect, and its findings were fixed. **Left for you:** commit and push; read `AgentRunner`, `StudentApplication.execute` and `PeriodicChecker` until you can explain every check; review the `AI_USE.md` rows; ask the instructor about the D1 PDF (Appendix A); confirm on eClass that no course model endpoint has been announced (if one has, run `run live` and replace `results/d2-live.txt`), build the ZIP (S.3–S.4); submit.

Setup is done ([Appendix A](#appendix-a-setup-done)). Tasks marked *(Claude)* were done by Claude ([Appendix B](#appendix-b-work-done-by-claude-for-ai_usemd)).

### Schedule

| Day | Work |
|---|---|
| Fri 30 – Sat 31 Oct | D2.2 Parts A–D; then 2.1.10 and the D2.1 commit |
| Sun 1 Nov | D2.3 ticks |
| Mon 2 – Tue 3 Nov | D2.5 demo, live run or pending status, evidence, test plan |
| Wed 4 Nov | D2.4 diagrams and DESIGN.pdf |
| Thu 5 Nov | Submit tasks: README, AI_USE, ZIP, fresh-unzip check |
| Fri 6 Nov | Buffer; submit before 23:59 |

## The big picture

D2 turns D1's single `propose` call into a small supervised agent:

1. A **model** (or a replay script standing in for one) sends JSON requests such as `{"tool":"READ_BOOKING","bookingId":"B1","targetId":""}`.
2. **Java** checks each request, runs the tool, and sends the result back. It gives up after at most 6 calls.
3. The agent can only **propose** a move. A **human operator** approves or rejects it.
4. **Execute** re-checks everything, then moves B1 exactly once and records one notification.
5. A **tick** (pretend clock, every 60 seconds) spots bookings stuck on closed spaces.

Every task that adds behaviour ends with its test, in the same part. Testing is worth 20 points across all parts.

## Acceptance cases

| Case | What happens | Expected | Task | Done |
|---|---|---|---|---|
| D2-A | Replay: read, list, propose B12 | One `PENDING` proposal; B1 still A17 / version 0 | 2.1.14 | ✅ |
| D2-B | Execute a pending proposal | `NOT_APPROVED`; nothing changed | 2.2.16 | ✅ |
| D2-C | Approve, then execute | `COMMITTED`; B1 at B12 / version 1; one notification | 2.2.17 | ✅ |
| D2-D | Execute the same proposal again | `DUPLICATE`; still one commit and one notification | 2.2.18 | ✅ |
| D2-E | Reject, then execute | No commit; nothing changed | 2.2.19 | ✅ |
| D2-F | B1's version changes after the proposal | `STALE`; no move | 2.2.20 | ✅ |
| D2-G | Target closed or occupied after the proposal | `UNAVAILABLE_OR_INELIGIBLE`; no move | 2.2.21 | ✅ |
| D2-H | Broken JSON, or an extra `"approved":true` | Rejected; counts as a call; nothing approved | 2.1.17 | ✅ |
| D2-I | Unknown tool, or a booking other than B1 | Rejected; nothing else runs | 2.1.18 | ✅ |
| D2-J | Six bad replies | `STEP_LIMIT` after exactly six calls | 2.1.19 | ✅ |
| D2-K | Provider throws or times out | `PROVIDER_ERROR`; no commit | 2.1.20 | ✅ |
| D2-L | Ticks 0, 59, 60, nothing changed | At most one work item | 2.3.8 | ✅ |
| D2-M | Tick earlier than the last one | Clear clock error | 2.3.9 | ✅ |
| D2-N | Real model run | Real transcript, or `LIVE_RUN_PENDING_COURSE_ACCESS` | 2.5.3 | Pending status recorded |

A model may pick C03 instead of B12. Both are allowed, so tests must accept either and must not check exact wording.

## Terms used below

- **Nothing changed:** before and after, all of these are equal: B1's space and version; the open and occupied flags of A17, B12, C03 and D09; every proposal's status; the number of commits; the number of notifications. Task 2.2.11 builds one helper for this.
- **Invalid reply:** the parser rejected it (broken JSON, wrong keys, unknown tool, wrong booking). Counted in `invalidResponses`.
- **Tool error:** a valid request that broke a tool rule (list before read, ineligible target). Uses a call, but is not an invalid reply.

---

## 1. D2.2 Approval and execution · 25 points

A proposal waits as `PENDING`. A human calls `approve` or `reject`. Then `execute` checks everything again and either refuses with a named reason or moves B1 once; a second `execute` must not move it again. Source: `[D2.2]`, `[Overview]` p.2–3.

**Done when:** tests for D2-B to D2-G pass, each checking "nothing changed" or the exact changes.

### Part A: let the domain objects change, safely

- [x] 2.2.1 `Proposal.approve()`: only from `PENDING`, otherwise `IllegalStateException`
- [x] 2.2.2 `Proposal.reject()`: only from `PENDING`
- [x] 2.2.3 `Proposal.markExecuted()`: only from `APPROVED`; sets `EXECUTED`
- [x] 2.2.4 Tests for every status × method; each refused call throws **and** leaves the status unchanged
- [x] 2.2.5 `Booking.movedTo(spaceId)`: new booking on that space, version + 1; the original is unchanged
- [x] 2.2.6 `Space`: `open` and `occupied` non-final, changed only by `occupy()`, `release()`, `close()`
- [x] 2.2.7 `Booking.withDuration(hours)`: new booking with that duration, version + 1 (needed for D2-F) *(Claude)*
  - Test: 1 and 24 accepted; 0 and 25 rejected; 4 gives version + 1 on the same space; the original is unchanged
- [x] 2.2.8 Enum `ExecutionOutcome`: `NOT_APPROVED`, `STALE`, `UNAVAILABLE_OR_INELIGIBLE`, `COMMITTED`, `DUPLICATE` *(Claude)*

### Part B: store the data and make it visible to tests

- [x] 2.2.9 `ParkingStore` holds the booking, spaces, proposals, a commit list and a notification list *(Claude)*
  - It needs a method that swaps in the new booking (e.g. `replaceBooking`), since `Booking` is read-only
  - The existing `StudentApplication` methods read from the store; all tests still pass
- [x] 2.2.10 Read-only `commitCount()` and `notifications()` on `StudentApplication` *(Claude)*
- [x] 2.2.11 Test helper `protectedState(app)` in `StudentTests` that captures everything in "nothing changed" *(Claude)*

### Part C: the operator actions

- [x] 2.2.12 `approve(proposalId)` and `reject(proposalId)` on `StudentApplication`; an unknown id gives a clear error *(Claude)*
- [x] 2.2.13 Setup actions D2-F and D2-G need: `changeBookingDuration(hours)` (raises B1's version via `withDuration`), `closeSpace(spaceId)`, `occupySpace(spaceId)`. Test each *(Claude)*
- [x] 2.2.14 `execute(proposalId)` runs these checks in order and stops at the first that fails: *(Claude)*

  | # | Check | If it fails |
  |---|---|---|
  | 1 | The proposal exists and is for B1 | Clear error |
  | 2 | Already `EXECUTED`? | `DUPLICATE`, change nothing |
  | 3 | Anything other than `APPROVED`? | `NOT_APPROVED` |
  | 4 | B1's version or the policy version changed since the proposal? | `STALE` |
  | 5 | Target not eligible right now? | `UNAVAILABLE_OR_INELIGIBLE` |
  | 6 | All passed | Commit, return `COMMITTED` |

  The policy version stays 1 in D2, so only the booking half of check 4 is testable until D3.
- [x] 2.2.15 A commit does exactly this: free B1's old space, occupy the target, move B1 (version + 1), mark the proposal `EXECUTED`, add one commit record, add one notification record *(Claude)*

### Part D: one test per case

- [x] 2.2.16 D2-B: propose B12, execute → `NOT_APPROVED`; nothing changed *(Claude)*
- [x] 2.2.17 D2-C: propose, approve, execute → `COMMITTED`; B1 at B12 / version 1; A17 free; B12 occupied; 1 commit; 1 notification *(Claude)*
- [x] 2.2.18 D2-D: execute the same proposal again → `DUPLICATE`; still 1 commit and 1 notification *(Claude)*
- [x] 2.2.19 D2-E: propose, reject, execute → `NOT_APPROVED`; nothing changed *(Claude)*
- [x] 2.2.20 D2-F: propose, approve, raise B1's version, execute → `STALE`; B1 not moved *(Claude)*
- [x] 2.2.21 D2-G: propose, approve, occupy B12, execute → `UNAVAILABLE_OR_INELIGIBLE`; repeat with B12 closed *(Claude)*
- [ ] 2.2.22 Commit ("D2.2: approval and execution") with a CHANGELOG line (CHANGELOG lines written; the commit is yours)

---

## 2. D2.1 The model/tool loop · 20 points

`run(provider, limit)` asks the provider for a reply, checks it, runs the tool, adds the result to the conversation, and asks again, up to `limit` (1–6) calls. It ends with `AWAITING_APPROVAL` (proposal made), `NO_ACTION` (`FINISH`), `STEP_LIMIT` (calls used up) or `PROVIDER_ERROR`. It never approves or executes. Source: `[D2.1]`, `[Overview]` p.2.

| Tool | Must come after | Returns |
|---|---|---|
| `READ_BOOKING` | – | B1's details and the policy version |
| `LIST_ALTERNATIVES` | a read | The spaces eligible right now (B12, C03) |
| `PROPOSE_REASSIGNMENT` | a read and a list | Creates one `PENDING` proposal, returns `AWAITING_APPROVAL` |
| `FINISH` | – | `NO_ACTION` |

**Done when:** tests for D2-A, D2-H, D2-I, D2-J and D2-K pass. ✅

Built in `Tool`, `ToolRequest`, `ToolRequestParser`, `AgentRunner`, `AgentOutcome`, `AgentResult`, plus `StudentApplication.run`, `policyVersion()`, `alternatives()`, `spaceIds()`.

### Part A: check the reply (the parser)

- [x] 2.1.1 `Tool` enum with the four tool names *(Claude)*
- [x] 2.1.2 `ToolRequest` record: `tool`, `bookingId`, `targetId` *(Claude)*
- [x] 2.1.3 `ToolRequestParser.parse(text, goalBookingId, knownSpaceIds)` returns a request or a rejection reason *(Claude)*. It rejects:
  - text that isn't a JSON object
  - anything other than exactly `tool`, `bookingId`, `targetId` (so `"approved":true` is rejected)
  - a value that isn't a string, or an unknown tool name
  - a `bookingId` other than `B1`
  - a non-empty `targetId` for read, list or finish; an unknown space for propose
- [x] 2.1.4 Tests for each rejection, plus one valid request per tool *(Claude)*

### Part B: the loop

- [x] 2.1.5 `run` rejects a limit outside 1..6 *(Claude)*
- [x] 2.1.6 The conversation starts with a system message giving the goal and the JSON format *(Claude)*
- [x] 2.1.7 Each turn: call the provider → save the reply (`assistant`) → parse → run the tool → save the result (`user`) *(Claude)*
- [x] 2.1.8 An invalid reply is counted; its reason goes back and the loop continues *(Claude)*
- [x] 2.1.9 A tool error goes back as the observation; it never crashes the run *(Claude)*
- [x] 2.1.10 List and propose check that B1's version and the policy version haven't changed since the read *(Claude)*
- [x] 2.1.11 Propose reuses D1's `propose`; its `IllegalArgumentException` becomes a tool error *(Claude)*
- [x] 2.1.12 The provider throws or times out → `PROVIDER_ERROR` *(Claude)*
- [x] 2.1.13 `run` returns `AgentResult`: outcome, calls, invalid count, proposal id, transcript *(Claude)*

### Part C: tests

- [x] 2.1.14 D2-A: `Fixture.replay()` → `AWAITING_APPROVAL`; one `PENDING` B1 → B12 proposal (versions 0/1); B1 still A17 / 0 *(Claude)*
- [x] 2.1.15 Later provider calls saw the earlier results (`replay.histories()`) *(Claude)*
- [x] 2.1.16 Proposing C03 is accepted *(Claude)*
- [x] 2.1.17 D2-H: broken JSON and `"approved":true` → counted invalid; nothing approved *(Claude)*
- [x] 2.1.18 D2-I: unknown tool and `bookingId` `B2` → rejected; nothing changed *(Claude)*
- [x] 2.1.19 D2-J: six bad replies, limit 6 → `STEP_LIMIT`; exactly 6 calls; no proposal *(Claude)*
- [x] 2.1.20 D2-K: `IOException` and `HttpTimeoutException` → `PROVIDER_ERROR`; nothing changed *(Claude)*
- [ ] 2.1.21 Commit ("D2.1: model/tool loop") with a CHANGELOG line (CHANGELOG lines written; the commit is yours)

---

## 3. D2.3 Ticks · 10 points

`tick(seconds)` is a pretend clock. The first tick, and any tick at least 60 seconds after the last scan, looks for bookings on closed spaces (B1 on A17) and emits a work item. It never emits the same work twice, and a clock that goes backwards is an error. Nothing actually waits. Source: `[D2.3]`, `[Overview]` p.3.

**Done when:** tests for D2-L and D2-M pass.

- [x] 2.3.1 `tick(long seconds)`: the time is an argument; no `sleep`, no system clock *(Claude)*
- [x] 2.3.2 A time lower than the last tick → `IllegalArgumentException` saying the clock went backwards; nothing changes *(Claude)*
- [x] 2.3.3 Scan on the first tick, then on the first tick at or past each 60-second boundary from it (first + 60, first + 120, …) *(Claude)*
- [x] 2.3.4 A scan finds bookings on closed spaces *(Claude)*
- [x] 2.3.5 Remember each (booking, booking version, policy version) emitted; never emit it again *(Claude)*
- [x] 2.3.6 Emit nothing while a `PENDING` or `APPROVED` proposal exists for that booking and versions *(Claude)*
- [x] 2.3.7 `retry(bookingId)` (operator action) forgets the remembered pair, so the next scan may emit it again *(Claude)*
  - That's all a retry needs: 2.3.5 already stops re-emitting failed work, and a version change makes a new pair
- [x] 2.3.8 Test D2-L: ticks 0, 59, 60 → 1 item, then 0, then 0 *(Claude)*
- [x] 2.3.9 Test D2-M: tick 60 then 30 → rejected; tick 120 afterwards still works *(Claude)*
- [x] 2.3.10 Tests: pending proposal → no item; after a commit → no item; `retry` → one new item at the next scan *(Claude)*
- [ ] 2.3.11 Commit ("D2.3: periodic ticks") with a CHANGELOG line (CHANGELOG lines written; the commit is yours)

---

## 4. D2.5 Demo, live run, evidence · 30 points

Show everything working with commands the TA can run, and either run a real model or record that course access isn't available yet. Covers "testing" (20) and "reproducibility" (10). Source: `[D2.5]`, `[D2 report]`.

- [x] 2.5.1 `run scripted` in `Main.java` (per the decision table) runs the D2 demo, labelled "not a live model run", printing state after each step: *(Claude)*
  1. ticks 0, 59, 60 → one work item, then none
  2. replay read → list → propose B12 → `AWAITING_APPROVAL`
  3. execute → `NOT_APPROVED`
  4. operator approves → execute → `COMMITTED`
  5. execute again → `DUPLICATE`
  6. tick 30 → clock rejected
- [x] 2.5.2 `run live`: after the loop, ask the operator `approve P1? [y/N]`, default no; never commits by itself *(Claude)*
- [x] 2.5.3 Live model (D2-N): *(Claude)*
  - endpoint announced → run it; save the model name, transcript, proposal and operator decision to `results/d2-live.txt`, credentials removed
  - not announced → `results/d2-live.txt` says `LIVE_RUN_PENDING_COURSE_ACCESS`, with the date and what you checked
  - never pay for access or fake a transcript
- [x] 2.5.4 `results/d2.txt`: date, OS, `java -version`, then the output of `check`, `test` and `run scripted` (the D1 Step 9 block, renamed). The scripted ticks are the required tick trace *(Claude)*
- [x] 2.5.5 `TEST_PLAN.md`: D2-A to D2-N rows (preconditions, input, expected, actual, test name; for rejections, what stays unchanged) *(Claude)*

---

## 5. D2.4 Diagrams and DESIGN.pdf · 15 points

Draw what you built and explain your choices, once the code stops changing. Source: `[D2.4]`, `[D2 report]`.

- [x] 2.4.1 Fill in **Your choice** in the [decision table](#design-decisions) *(Claude: filled with what was built; change any you disagree with)*
- [x] 2.4.2 Class diagram: the new classes, with the four kinds of code visible *(Claude)*
- [x] 2.4.3 Sequence diagram: loop → pending proposal → approval boundary → approve → execute with re-checks → commit *(Claude)*
- [x] 2.4.4 Proposal-state diagram: `PENDING → APPROVED → EXECUTED`, `PENDING → REJECTED`; executing again gives `DUPLICATE` with no state change *(Claude)*
- [x] 2.4.5 Update `design/build.py` for D2 (its text is D1's; the third diagram needs SVG code). Budget most of a day; re-run it and open the PDF *(Claude)*
- [x] 2.4.6 DESIGN.pdf, at most 5 pages plus diagrams, contains: *(Claude)*
  - [x] which class does what, and what depends on what
  - [x] a state transition table (state × approve/reject/execute → result)
  - [x] two design decisions, each with an alternative you rejected
  - [x] one rejected model/tool interaction and what Java did about it
  - [x] why failed work needs an operator retry (2.3.7)
  - [x] limitations: one process, in memory, lost on restart, live-run status, accessibility still ignored
  - [x] starter code reused, and the D1 `DESIGN.pdf` fix

### Classes

The four kinds of code D2.4 asks to keep apart:

| Kind | Classes | Job |
|---|---|---|
| Model protocol | `Tool`, `ToolRequest`, `ToolRequestParser` | Turn reply text into a checked request, or a rejection reason |
| Coordination | `StudentApplication`, `AgentRunner` | Operator actions, the loop, `tick` |
| Domain rules | `Booking`, `Space`, `Proposal`, `EligibilityPolicy`, `ExecutionOutcome` | The parking rules; no JSON, no storage |
| Storage | `ParkingStore`, `CommitRecord`, `Notification` | Booking, spaces, proposals, commits, notifications |

### Design decisions

Two of these become DESIGN.pdf's "two design decisions".

| Question | Options | Suggested | Your choice |
|---|---|---|---|
| How a commit changes `Booking` | A new copy (`movedTo`), or change it in place | New copy; keeps D1's read-only booking | New copy (built) |
| How a commit changes `Space` | Non-final flags changed by `occupy`/`release`/`close`, or a new copy | Methods; a space has no version, so a copy adds code for nothing | Methods (built) |
| How `execute` reports a result | `ExecutionOutcome` enum, or one exception per failure | Enum; the brief wants named outcomes | Enum (built) |
| How the parser reports bad input | Result value, or throw | Result value; the loop counts it and continues *(built this way)* | Result value (built) |
| How the loop reaches the domain | Through `StudentApplication`'s public methods, or direct object access | Public methods; the model can't bypass operator checks *(built this way)* | Public methods (built) |
| Where data lives | A `ParkingStore` class, or fields in `StudentApplication` | `ParkingStore`; D2.4 asks for separate storage | `ParkingStore` (built) |
| How time reaches `tick` | `tick(long seconds)`, or an injected `Clock` | `tick(long seconds)`; simplest | `tick(long seconds)` (built) |
| What `run scripted` shows | The D2 demo (D1 demo renamed), or the D1 demo (D2 under `agent-scripted`) | D2 demo; the brief names `scripted` | D2 demo; D1 → `scripted-d1` (built) |

---

## 6. Submit

- [x] S.1 README: JDK 21; commands (`run scripted`, `run live` with `OLLAMA_CHAT_URL` and `OLLAMA_MODEL`, the D1 demo); scenarios; limitations; your names → the brief's terms *(Claude)*
- [x] S.2 AI_USE: one row per AI-assisted task ([Appendix B](#appendix-b-work-done-by-claude-for-ai_usemd) lists Claude's) *(Claude)*
- [ ] S.3 Build `EECS3311_D2_221524855.zip` in the course folder: same exclusions as D1, plus the repo-root `D2_Working_Supervised_Agent.pdf` and `Learnings.md` (you build the ZIP; Claude does not)
- [ ] S.4 From a fresh unzip: `bash run.sh check`, `bash run.sh run scripted`, `bash setup-junit.sh`, `bash run.sh test`; open `DESIGN.pdf` and check every page (you build the ZIP; Claude does not)
- [ ] S.5 Submit on eClass before 23:59 Friday 6 November; keep the receipt and the ZIP. If something is unfinished, submit anyway and name it

---

## Rules not to break

- Don't change the fixture to make a test pass `[Overview]`
- Don't delete failing tests or edit captured output `[Overview]` `[Starter]`
- Run `bash run.sh test` before every commit; D1 tests must keep passing `[D2.5]`
- Don't let the agent loop approve or execute, and never accept approval from model output `[D2.2]` `[Overview]`
- Don't rely on the prompt for a control; enforce it in Java `[D2.2]`
- Don't call the model or anything outside the program inside `execute` `[Overview]`
- Don't import parser, runner or store classes into domain classes `[D2.4]`
- Don't use the accessibility flag until D3 `[Overview]`
- Don't use `sleep` or real time for ticks `[D2.3]`
- Don't label replay output as a live model run, or plain checks as JUnit `[Overview]`
- Don't buy model access or fabricate a live transcript `[D2.5]` `[Overview]`
- Don't pre-fill `results/` `[Starter]`

---

## Appendix A: Setup (done)

- [x] 0.1 `bash run.sh check` → `CHECKS 14; FAILED 0`
- [x] 0.2 `bash run.sh test` → all pass
- [x] 0.3 Committed the CHANGELOG and plan changes
- [x] 0.4 Checked eClass for D1 feedback
- [x] 0.5 Checked eClass for a course model endpoint (decides 2.5.3)

D1 defects:

- `DESIGN.pdf` in the D1 ZIP was unreadable: re-saved as text after Chrome printed it on 4 Oct. Rebuilt 29 Oct with `python3 design/build.py`, content unchanged (`design/DESIGN.html` identical to commit `07f55c4`); CHANGELOG entry in commit `ee4a899`
  - [ ] Ask the instructor on eClass whether the rebuilt PDF can be assessed for D1
- No D1 feedback released yet

## Appendix B: Work done by Claude (for `AI_USE.md`)

- 29 Oct: wrote the D2 plan from the D2 brief, overview and starter; reorganised it by D2.1–D2.5
- 29 Oct: found why `DESIGN.pdf` was blank, rebuilt it, added the CHANGELOG "D1 corrections" entry
- 29–30 Oct: example code for `Proposal.approve()`, `Booking.movedTo()`, `Space.occupy()/release()/close()` and the `Space` tests; reviews of your Part A code and tests
- 30 Oct: fixed plan gaps (setup actions for D2-F/D2-G, invalid replies vs. tool errors, parser inputs, retry, `build.py` effort) and removed redundant tasks
- 30 Oct: tidied comments and the `markExecuted()` message in `Proposal.java`; fixed three test-name typos; added `ExecutionOutcome`
- 30 Oct: built D2.1 Parts A–C test-first (all but 2.1.10): `Tool`, `ToolRequest`, `ToolRequestParser`, `AgentOutcome`, `AgentResult`, `AgentRunner`; `StudentApplication.run`, `policyVersion()`, `alternatives()`, `spaceIds()`; 24 tests in `StudentTests.java`
- 30 Oct: reorganised this plan (status table, renumbered tasks, appendices, D1 archived)
- 30 Oct: wrote the rest of D2 test-first: `withDuration`, `ParkingStore`, `CommitRecord`, `Notification`, operator `approve`/`reject`/`execute`, setup actions, the stale check (2.1.10), `PeriodicChecker`, `WorkItem`, `tick`/`retry`; 114 tests in `StudentTests`
- 30 Oct: rewrote `Main` (D2 `scripted`, `scripted-d1`, operator prompt in `live`); wrote the D2 `DESIGN.pdf` and its five diagrams; kept the D1 design as `design/DESIGN_D1.pdf` (`design/build_d1.py`); updated `README.md`, `TEST_PLAN.md`, `CHANGELOG.md`, `AI_USE.md`; captured `results/d2.txt` and `results/d2-live.txt`
- 30 Oct: QA: an independent review against the D2 brief, fixes for its findings (second active proposal, tick grid, boundary and stale tests), the ZIP built and checked from a fresh unzip

## Appendix C: Source tags

| Tag | Meaning | Source file |
|---|---|---|
| `[D1.1]`–`[D1.5]`, `[D1-A]`–`[D1-F]` | **Required.** D1 brief section / acceptance row | `../D1_Design_and_Executable_Path.pdf` |
| `[D2.1]`–`[D2.5]` | **Required.** A numbered section of the D2 brief | `D2_Working_Supervised_Agent.pdf` (repo root: amended copy, two amendment pages then the brief) |
| `[D2-A]`–`[D2-N]` | **Required.** A row of the D2 acceptance table | same file, brief p.1–2 |
| `[D2 intro]`, `[D2 note]`, `[D2 report]` | **Required.** Opening paragraph / note under the table / "Report and submission additions" | same file, brief p.1–2 |
| `[Amendment]` | **Required.** The 18 September amendment pages | same file, amendment p.1–2 |
| `[Overview]` | **Required.** Project-wide rule | `../00_Project_Overview.pdf` |
| `[Starter]` | **Required by the starter code** | `test/plain/PublicChecks.java`, `src/StudentApplication.java`, `src/Main.java`, `START_HERE.md` |
| `[Suggestion]` | **Optional.** A design choice you may change | none |

---

# Archive: D1 plan (done)

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
