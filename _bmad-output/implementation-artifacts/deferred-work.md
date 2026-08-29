# Deferred Work

## Deferred from: code review of spec-1-1-enchant-proc-tier-content-loader.md (2026-08-29)

- No automated verification of the boot-load path — the repo has no test infrastructure; verification of `EnchantProcTable` loading is a one-off manual docker boot recorded in the spec, not re-run in any normal verification path. A missing or broken `enchant_proc` table leaves the feature silently off with only an ERROR log line. Evidence: no test dirs/`*Test*.java` in `src` (only in-game opcode-test packets); spec Verification section records manual checks only.
- `effect_id = 0` doubles as placeholder and potentially-real effect ID with no marker column or sentinel — after Story 1.3 partially updates rows via `update_088.sql`, placeholder and real semantics would be mixed with no way to tell them apart. Evidence: `db/update_087.sql` seed rows; spec Design Notes defer real ID selection to Story 1.3.
- Singleton is published even when the load fails (no retry; an empty table is indistinguishable from "no matching tier" at the API level) — codebase-wide table pattern shared by `WeaponSkillTable` and all sibling datatables, not introduced by this story. Evidence: `EnchantProcTable.getInstance()` mirrors `WeaponSkillTable.getInstance()`; `catch (SQLException)` logs and continues, `_instance` is set regardless.

## Deferred from: code review of spec-1-2-enchant-tier-proc-triggers-physical-damage-on-attack.md (2026-08-29)

- source_spec: `_bmad-output/implementation-artifacts/spec-1-2-enchant-tier-proc-triggers-physical-damage-on-attack.md`
  summary: The attack-path integration (gate operator, additivity, post-kiringku placement in `L1Attack.calcPcBaseDamage`) has no re-runnable automated verification — the scratch harness verifies a re-implementation of the gate expression, not the production line, and the core AC (bonus damage on a hit) is user-run with a client.
  evidence: No test infrastructure in the repo (no test dirs, no JUnit — pre-existing, see Story 1.1 deferral); flipping `>=` to `>`, moving the block before the kiringku replacement, or changing `damage +=` to `damage =` would pass compile, boot-log, and harness verification alike.
