# Student AI-use disclosure

Record your own work below. The course-provided starter has separate provenance.

| Date | Tool/model | Task | Files affected | What I changed or rejected | Verification |
|---|---|---|---|---|---|
| 2026-10-03 | Claude Code (VS Code extension), Claude Opus 5.5 | Drafted a D1 checklist from the D1 brief and project overview, then restructured it into work order with each item tagged by source: acceptance cases, fixture reference, schedule | `D1_PLAN.md` (personal planning file, not submitted) | Accepted as written; no edits yet | Compared items against `D1_Design_and_Executable_Path.pdf` and `00_Project_Overview.pdf` |
| 2026-10-03 | Claude Code (VS Code extension), Claude Opus 5.5 | Wrote the D1 changelog entries for the `DomainRules` validation work and for `Booking` and its tests | `CHANGELOG.md` | Accepted as written; no edits yet | Entries match `src/DomainRules.java`, `src/Booking.java` and the `bash run.sh check` / `bash run.sh test` results |
| 2026-10-03 | Claude Code (VS Code extension), Claude Opus 5.5 | Explained guard methods, regex and `Pattern.compile`; supplied example code for `requireDuration` (named constants) and `requireIdentifier` (pattern constant, null check, `IllegalArgumentException`) | `src/DomainRules.java` | Typed the code myself; chose my own error-message wording | `bash run.sh check`: all duration and identifier checks pass |
| 2026-10-03 | Claude Code (VS Code extension), Claude Opus 5.5 | Explained constructor validation, field assignment and getters; reviewed my `Booking` and reported compile errors (wrong field type, misspelled names, missing `spaceId` parameter) | `src/Booking.java` | Wrote the class myself and fixed the reported errors | `bash run.sh compile` prints `COMPILE_OK`; Booking tests pass |


If you used no generative AI for your own work, state that explicitly here.
Do not claim that the supplied starter was created without assistance.
