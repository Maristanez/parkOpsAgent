# D1 plan: ParkOps

Due **Friday 9 October 2026, 23:59 Toronto time** · 10% of course · Route A (ParkOps)

Personal checklist. Not part of the submission, so leave it out of the ZIP.

## How to read this plan

Every item says where it comes from:

| Tag | Meaning | Source file |
|---|---|---|
| `[D1.1]`–`[D1.5]` | **Required.** A numbered section of the D1 brief | `../D1_Design_and_Executable_Path.pdf` |
| `[D1-A]`–`[D1-F]` | **Required.** A row of the D1 acceptance table | `../D1_Design_and_Executable_Path.pdf`, p.1 |
| `[Overview]` | **Required.** Project-wide rule | `../00_Project_Overview.pdf` |
| `[Starter]` | **Required by the starter code** (public checks, TODOs) | `test/plain/PublicChecks.java`, `src/StudentApplication.java`, `START_HERE.md` |
| `[Suggestion]` | **Optional.** A design choice you may change | none |

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

## Step 2: `Booking` class (in progress)

- [ ] Fields: id, assigned space id, duration, booking version, requires-accessible flag `[Overview]`
- [ ] Constructor validates id, space id and duration before storing anything, so no invalid booking can exist `[D1.1]` `[D1-B]`
- [ ] Version must be ≥ 0 `[Suggestion]`
- [ ] Private final fields, getters, no setters `[Suggestion]` (the requirement is "controlled state access" `[D1.1]`)
- [ ] Tests: durations 1 and 24 kept `[D1-A]`; 0 and 25 rejected `[D1-B]`; null and malformed ids rejected `[D1.4]`

Done when: `bash run.sh compile` prints `COMPILE_OK` and the tests pass.

## Step 3: `Space` class

- [ ] Fields: id, open, occupied, accessible `[Overview]`
- [ ] "Available" means open and not occupied `[Overview]`; put it in an `isAvailable()` method `[Suggestion]`
- [ ] Validate the id in the constructor `[D1.1]`
- [ ] Tests: B12 available; A17 and D09 not available; invalid id rejected `[Suggestion]`

## Step 4: `Proposal` class and status

- [ ] Fields: proposal id, booking id, target space id, observed booking version, observed policy version, status `[Overview]`
- [ ] Statuses: `PENDING`, `APPROVED`, `REJECTED`, `EXECUTED` `[Overview]`, as a `ProposalStatus` enum `[Suggestion]`
- [ ] A new proposal starts as `PENDING`; no public way to set the status `[Suggestion]` (the model never supplies approval `[Overview]`)
- [ ] Keep proposals separate from bookings: a proposal is not a completed change `[D1.1]`
- [ ] Method that converts a `Proposal` to the supplied `ProposalView` record `[Starter]`
- [ ] Tests: new proposal is `PENDING` and keeps the versions it was given; invalid ids rejected `[Suggestion]`

## Step 5: Policy v1 eligibility check

- [ ] Eligible means: target is not the booking's current space **and** target is available `[Overview]`
- [ ] Ignore the accessibility flag; that is the D3 change `[Overview]`
- [ ] Put the rule in one method with the policy version (1) beside it `[Suggestion]`
- [ ] Tests: B12 and C03 eligible; A17 and D09 not `[Suggestion]`

## Step 6: `StudentApplication` storage and snapshot

- [ ] Replace the raw `Map` with your `Booking`, the spaces, and the stored proposals `[Starter]`
  - spaces in a private map keyed by id, proposals in a private list, a counter for proposal ids `[Suggestion]`
- [ ] Never return an internal collection that callers could change `[D1.1]`
- [ ] `bookingSnapshot()` returns a fresh map that keeps the `"spaceId"` and `"version"` keys `[Starter]`
- [ ] Two `StudentApplication` objects do not share state ("separate object instances") `[D1.1]`
- [ ] A read-only way to see a space's state and how many proposals exist, so tests can prove nothing changed `[Suggestion]`
- [ ] Tests: changing a snapshot does not change the booking `[D1-F]`; two applications are independent `[D1.1]`

## Step 7: `propose(targetId)`

- [ ] Retrieve B1, check policy v1, record booking version 0 and policy version 1 `[D1.3]`
- [ ] Create and store a `PENDING` proposal with an id the application generates `[D1.3]`, which must match the identifier pattern `[Starter]`
- [ ] The booking stays at A17, version 0. Never approve or execute `[D1.3]`
- [ ] Reject D09 and unknown targets without changing anything `[D1.3]`, using `IllegalArgumentException` `[Starter]`
- [ ] Also reject A17 (current space) and malformed ids `[Suggestion]`
- [ ] Tests, each checking **every** protected field afterwards, not just that an exception was thrown `[D1.4]` (the rubric says exception-only tests are insufficient):
  - B12 gives a `PENDING` proposal for B1 → B12 with versions 0/1; booking still A17 / 0 `[D1-C]`
  - D09 rejected; booking, D09's occupancy and proposal count unchanged `[D1-D]`
  - unknown target rejected; no proposal stored; booking unchanged `[D1-E]`

Done when: `bash run.sh check` prints `CHECKS 14; FAILED 0`.

## Step 8: Demonstration command

- [ ] `bash run.sh run scripted` feeds the scripted target B12 and displays a structured result `[D1.3]`
- [ ] The same command also shows D09 and an unknown target being rejected `[D1.3]`; today it only runs B12, so extend `Main` `[Starter]`
- [ ] After each attempt, print the booking to show it is still A17 / version 0 `[Suggestion]`
- [ ] Keep the "not a live model run" label `[Overview]`

## Step 9: Capture evidence

- [ ] Save genuine output with date, command and environment to `results/d1.txt` `[Overview]` `[D1.4]`
- [ ] It must show how the assessor runs the D1-A–F cases `[D1.4]`
- [ ] Don't label plain checks as JUnit, or replay as a live model `[Overview]`; don't hand-edit output `[Starter]`

One way to capture everything at once `[Suggestion]`:

```
{
  echo "Date: $(date)"; uname -sr; java -version
  echo; echo '$ bash run.sh check'; bash run.sh check
  echo; echo '$ bash run.sh test'; bash run.sh test
  echo; echo '$ bash run.sh run scripted'; bash run.sh run scripted
} > results/d1.txt 2>&1
```

## Step 10: `TEST_PLAN.md`

- [ ] For each case D1-A to D1-F: preconditions, input, expected result, actual result, test method name `[Starter]`
- [ ] For each rejection case, list the outcome **and** every field that stays unchanged `[Starter]`

## Step 11: Diagrams (after the code stops changing)

- [ ] **Class diagram:** your domain and application classes, visibility, important associations, multiplicities `[D1.2]`
- [ ] Use the realization arrow (dashed line, hollow triangle) for interfaces, not the inheritance arrow `[D1.2]`
- [ ] **Sequence diagram:** read B1 → evaluate B12 → create pending proposal `[D1.2]`
- [ ] Mark the **approval boundary** on the sequence diagram, even though approval comes in D2 `[D1.2]`
- [ ] Both match the submitted code and are readable in PDF `[D1.2]`
- [ ] Tool: draw.io or PlantUML, exported to PDF `[Suggestion]`

## Step 12: `DESIGN.pdf`

At most 4 pages, not counting the two diagrams if they are on their own pages `[D1.5]`. Refer to exact class and method names `[D1.5]`.

- [ ] Responsibilities of each class `[D1.5]`
- [ ] One alternative you considered and why you rejected it `[D1.5]` (e.g. immutable `Booking` vs. one controlled method that changes it `[Suggestion]`)
- [ ] Two informal operation contracts: preconditions, postconditions, what may change `[D1.5]` (e.g. `propose`, `Booking` constructor `[Suggestion]`)
- [ ] The most important current limitation `[D1.5]`
- [ ] Which starter infrastructure you reused and which parts you wrote `[D1.5]`
- [ ] Include the two diagrams `[D1.2]`

## Step 13: README, CHANGELOG, AI_USE

- [ ] `README.md`: JDK version, exact commands, supported scenarios, known limitations, starter files reused `[Overview]`
- [ ] `README.md`: how your class and method names map to the brief's terms `[Starter]`
- [ ] `CHANGELOG.md`: one line per completed step `[Starter]`
- [ ] `AI_USE.md`: a row for each AI-assisted task with tool, date, task, files, what you changed or rejected, and how you checked it `[Overview]`
  - still missing: the `DomainRules` row

## Step 14: Package and submit

- [ ] ZIP named `EECS3311_D1_STUDENTNUMBER.zip` with a single top-level folder `[Overview]`
- [ ] That folder contains `src/ test/ run.sh README.md DESIGN.pdf AI_USE.md TEST_PLAN.md CHANGELOG.md results/` `[Overview]`
- [ ] Leave out `.class` files, IDE caches, secrets and downloaded libraries (`build/`, `lib/`) `[Overview]`
- [ ] Also leave out `.git/`, `.DS_Store` and this plan; keep `setup-junit.sh` and `data/` `[Suggestion]`
- [ ] Unzip into a fresh folder and run `bash run.sh check` and `bash run.sh run scripted` from its root `[Overview]`
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

## Rules not to break

- Don't change the fixture to make a test pass `[Overview]`
- Don't delete failing tests or edit captured output `[Overview]` `[Starter]`
- Don't approve or execute a proposal in D1 `[D1.3]`
- Don't use the accessibility flag until D3 `[Overview]`
- Don't label replay output as a live model run, or plain checks as JUnit `[Overview]`
- Don't pre-fill `results/` `[Starter]`
