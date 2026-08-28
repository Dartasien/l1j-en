# Review — adversarial (two-units-built-incompatibly)

Target: `ARCHITECTURE-SPINE.md` (l1j-en)

Method: construct pairs of units one level down that obey every AD to the letter yet build incompatibly.

## Verdict
One HIGH hole (feature state ownership), two MEDIUM holes (content-kind tiebreaker, SQL file numbering), both fixed in the spine.

## Pairs constructed

### 1. [HIGH] Raid feature: event team vs GM-command team — two owners of one entity
- **Team A** builds a timed raid: controller in `controllers/` + `GeneralThreadPool` (convention), state in `model/` per AD-5.
- **Team B** builds the GM command to start/inspect that raid: `command/` + `GMCommands` (convention).
- Both obey every AD. Nothing tells B where A's state lives or that A must not keep a parallel copy in the controller. Code shows the fork already does both: `WarTimeController` owns war state in statics with static accessors, while castle entities live in `model/`. Two teams split one feature across those homes and the GM command reads stale state.
- **Fix applied:** AD-7 — single state owner per feature/event: world/player state → `model/`; event-scoped state → the controller singleton, reachable only through its accessors; no parallel copies.

### 2. [MEDIUM] Two content teams, same new kind, different layers
- AD-4 says follow the closest existing kind. For a brand-new content kind there is no closest kind: one team picks `db/` SQL, the other picks `data/xml/`. Both "follow the kind" vacuously.
- **Fix applied:** AD-4 tiebreaker — new kind defaults to DB (the dominant layer) unless it is code-adjacent logic matching the XML pattern; choice noted in the change.

### 3. [MEDIUM] Two content changes, same `update_*.sql` number
- AD-4 mandates versioned `update_*.sql` files but no numbering rule. Two independent changes both become `update_087.sql` → merge collision or double-apply.
- **Fix applied:** convention — a content change takes the next free `update_NNN.sql` number.

### 4. [LOW, not fixed] Two features both extend `CharacterTable` save/load
- Merge-conflict surface, not an architectural divergence; the single JDBC boundary (AD-2) is doing its job. Ignored.

### 5. [LOW, not fixed] Utility class in `utils/` vs `types/`
- AD-5's "matching concern subpackage" leaves taste-level ambiguity; not an incompatibility. Ignored.
