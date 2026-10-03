# Change log

## Starter 2026-09-15
Supplied infrastructure and unfinished exercise boundary.

## Student changes
Record D1 implementation, D1 feedback fixes, D2 additions and D3 changes here.

### D1
- 2026-10-03: Implemented `DomainRules.requireDuration` (1–24 hours inclusive) and
  `DomainRules.requireIdentifier` (non-null, `[A-Z][A-Z0-9_-]{0,15}`). Both reject invalid
  input with `IllegalArgumentException`. Public duration and identifier checks now pass.
