---
title: 'Story 1.1: Enchant Proc Tier Content & Loader'
type: 'feature'
created: '2026-08-28'
status: 'done'
review_loop_iteration: 0
baseline_commit: 3640591a7178ae572df8ae9c571fce7b62f0695b
context:
  - {project-root}/_bmad-output/implementation-artifacts/epic-1-context.md
  - {project-root}/_bmad-output/planning-artifacts/architecture/architecture-l1j-en-2026-08-28/ARCHITECTURE-SPINE.md
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Enchant-scaled weapon procs (Epic 1) need their tier definitions — enchant range, trigger chance, min/max damage, damage type, signifying effect — as data-driven DB content, loaded at boot, so tiers can be tuned with zero code changes. Nothing exists yet: no table, no loader.

**Approach:** Ship a versioned `db/update_087.sql` creating an `enchant_proc` table with the 7 draft seed tiers, and add an `EnchantProcTable` datatables singleton (following the `WeaponSkillTable` precedent) that loads all rows at boot, skips malformed rows with a warning, and exposes a lookup by enchant level. This story is inert: no attack-path, packet, or config changes.

## Boundaries & Constraints

**Always:**
- SQL ships as `db/update_087.sql` (next free number), applied stop-the-world; table + seed rows in one file (AD-4).
- New classes only in matching subpackages: table in `datatables/`, row holder in `model/` (AD-5). No flat-root classes.
- slf4j only, one logger per class; no `System.out`.
- Table is boot-loaded and read-only at runtime — no new shared mutable state, no locking needed (AD-1).
- No player-state SQL, no changes to save/load paths (AD-2).

**Ask First:**
- Choosing real 2009 US client effect IDs for the seed rows (plan: seed `effect_id = 0` placeholders now; Story 1.3 selects real IDs and ships a follow-up `update_088.sql`).
- Any deviation from the 7 draft tier values (enchant ranges, 25% chance, damage ranges).

**Never:**
- No attack-path, damage, packet, opcode, or `Config` changes in this story (those are Stories 1.2/1.3).
- No schema changes to existing tables; no moving content between DB/XML layers.
- No new third-party dependencies.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| BOOT_LOAD | `enchant_proc` table with 7 valid seed rows | All rows loaded; log reports count loaded | N/A |
| LOOKUP_IN_TIER | enchant level 3 (tier 1: +1–+5) | Returns tier 1 row (25%, 3–7, physical, effect 0) | N/A |
| LOOKUP_ABOVE_TOP | enchant level 12+ (tier 7: +12+) | Returns tier 7 row | N/A |
| LOOKUP_NO_TIER | enchant level 0 or negative | Returns null (no proc) | N/A |
| MALFORMED_ROW | row with min_enchant > max_enchant, or negative damage, or unknown damage_type | Row skipped, slf4j warning naming the row; boot continues | warning logged |
| EMPTY_TABLE | table exists, zero rows | Boots normally; every lookup returns null | info log |

</frozen-after-approval>

## Code Map

- `src/l1j/server/server/datatables/WeaponSkillTable.java` -- the precedent: singleton `getInstance()`, private constructor loads via `L1DatabaseFactory.getInstance().getConnection()`, `SQLUtil.close(rs/pstm/con)` in finally, slf4j `_log`, `HashMap` index, `getTemplate(weaponId)` accessor. Mirror this shape.
- `src/l1j/server/server/GameServerThread.java:297` -- boot table-load block (`WeaponSkillTable.getInstance();` … `_log.info("Database tables loaded successfully!")`); register the new table here.
- `src/l1j/server/server/model/L1WeaponSkill.java` -- row-holder precedent for a datatables row (lives in `model/`, `L1*` naming, plain getters).
- `db/l1jdb_m10.sql:2144` -- `weapon_skill` CREATE TABLE: house style is backticked identifiers, `int(11) unsigned NOT NULL DEFAULT '0'`.
- `db/update_086.sql` -- latest versioned update; `db/update_087.sql` is the next free number.
- `src/l1j/server/server/model/Instance/L1ItemInstance.java:48` -- `_enchantLevel` (the value future lookups key on; read-only here).
- `src/l1j/server/server/utils/SQLUtil.java` -- connection/statement close helper used by all tables.

## Tasks & Acceptance

**Execution:**
- [x] `db/update_087.sql` -- create `enchant_proc` table (`tier_id` PK, `min_enchant`, `max_enchant`, `probability`, `damage_type` varchar, `min_damage`, `max_damage`, `effect_id`) in `l1jdb_m10.sql` house style; insert the 7 draft seed tiers (all `probability` 25, `damage_type` 'physical', `effect_id` 0 placeholder) -- AD-4 versioned content, tunable without code
- [x] `src/l1j/server/server/model/L1EnchantProcTier.java` -- new row holder (ctor from row values + getters), `L1*` model convention -- mirrors `L1WeaponSkill` row-holder precedent
- [x] `src/l1j/server/server/datatables/EnchantProcTable.java` -- new singleton `*Table`: load all rows at construction, validate each (skip + slf4j warning on min>max, negative damage, unknown damage type — known set is currently just `physical`), index for lookup; `getTier(int enchantLevel)` returns the tier whose min–max range contains the level (first match by `tier_id` order if ranges ever overlap), else null -- boot-loaded read-only, no locking (AD-1)
- [x] `src/l1j/server/server/GameServerThread.java` -- call `EnchantProcTable.getInstance();` in the boot table-load block next to `WeaponSkillTable.getInstance()` -- table must be loaded before the server opens

**Acceptance Criteria:**
- Given the server is stopped, when `db/update_087.sql` is applied, then the `enchant_proc` table and 7 seed rows exist in MariaDB and the file takes the next free update number
- Given the updated database, when the server boots, then `EnchantProcTable` loads all valid rows and logs the loaded count
- Given an enchant level within a tier's range, when `getTier` is called, then the matching tier is returned; given a level matching no tier (incl. ≤ 0), then null is returned
- Given a malformed row, when the table loads, then the row is skipped with an slf4j warning and the server boots normally
- When the change is complete, then it compiles under the existing Ant build and changes no runtime behavior (no attack-path, packet, or config edits)

## Design Notes

Seed `effect_id` as 0 (placeholder) deliberately: Story 1.1 is inert — nothing reads `effect_id` yet — and real 2009 US effect IDs are a Story 1.3 decision (they must render on the stock client, AD-3). Story 1.3 will ship `db/update_088.sql` with the real IDs rather than re-editing 087.

`damage_type` is a varchar validated against a known set (currently `{physical}`) so future non-physical tiers are content-only changes, per the epic's resolved design decisions.

Lookup is a linear scan over ≤ a handful of tiers — no index structure needed; keep it as simple as `WeaponSkillTable`'s single `HashMap`.

## Verification

**Commands:**
- `ant compile` -- expected: BUILD SUCCESSFUL, no new warnings from the changed files
- `grep -c "^('" db/update_087.sql` -- expected: 7 (seed value rows; single multi-row INSERT, house style per `update_086.sql`)

**Manual checks (if no CLI):**
- Apply `db/update_087.sql` to a scratch MariaDB, boot the server, confirm the load log line reports 7 tiers and the boot completes to "Database tables loaded successfully!"
- Confirm no diff touches `L1Attack.java`, `serverpackets/`, `clientpackets/`, or `Config.java`

**Verification results (08-28-2026):**
- `ant compile` in `eclipse-temurin:11-jdk` + Ant container: BUILD SUCCESSFUL (777 sources)
- Live boot test via `docker compose` (fresh MariaDB 12.0.2 initialized from `db/`): log shows `EnchantProcTable ... List of enchant proc tiers: 7 Loaded` followed by `Database tables loaded successfully!`
- Malformed-row test: inserted rows with min>max and unknown damage type `'plasma'`, rebooted — both skipped with slf4j warnings naming the tier, 7 valid tiers loaded, boot completed
- Diff audit: only `GameServerThread.java` modified (import + one boot line); no attack-path, packet, or `Config` changes

**Verification results after review patches (08-29-2026):**
- `ant compile` in `eclipse-temurin:11-jdk` + Ant container: BUILD SUCCESSFUL (2 changed sources recompiled)
- `grep -c "^('" db/update_087.sql` = 7 (unchanged)
- Live boot test via `docker compose` (fresh MariaDB 12.0.2 initialized from `db/`): `List of enchant proc tiers: 7 Loaded (0 skipped)`, no self-check errors, `Database tables loaded successfully!`
- Idempotency: re-applied `db/update_087.sql` to the initialized DB — still exactly 7 rows (`ON DUPLICATE KEY UPDATE` verified on MariaDB 12.0.2)
- New validation paths: inserted `probability=150`, `min_damage=9/max_damage=3`, and `damage_type='Physical'` rows, rebooted — first two skipped with warnings naming the tier, `'Physical'` loaded via case-insensitive normalization (`8 Loaded (2 skipped)`), boot completed

### Review Findings

_Code review 2026-08-29 (layers: blind-hunter, edge-case-hunter, verification-gap, acceptance-auditor; inline passes — same model/session, not independent LLMs). 21 unique findings after dedup; 3 dismissed as noise._

#### Decision-needed

- [x] [Review][Decision] Negative-damage AC is vacuous — RESOLVED 2026-08-29: keep `unsigned` (house style); check stays as documented defense-in-depth — `min_damage`/`max_damage` are `unsigned` in `db/update_087.sql`, so the loader's negative-damage check (`EnchantProcTable.java:96`) can never fire and the MALFORMED_ROW "negative damage" acceptance case cannot occur or be tested via the DB. Decide: keep unsigned (house style) and document the vacuous AC, or make the damage columns signed so the check is live.
- [x] [Review][Decision] `probability = 0` semantics — RESOLVED 2026-08-29: 0 is a legitimate "disable this tier" tuning knob; stays loadable, loader emits an info line — a 0% tier loads as valid and silently covers its enchant range with a proc that can never fire (`EnchantProcTable.java:91-107`). Decide: is 0 a legitimate "disable this tier" tuning knob (keep, maybe log), or a malformed row to skip with a warning?
- [x] [Review][Decision] `update_087.sql` re-apply/failure policy — RESOLVED 2026-08-29: idempotent guard — keep `IF NOT EXISTS`, pin `tier_id` 1–7, add `ON DUPLICATE KEY UPDATE` — the `INSERT` is unguarded while `CREATE` uses `IF NOT EXISTS` (a house-style deviation; no other `update_*.sql` uses it): re-applying the file duplicates all 7 seed rows (boot then logs `14 Loaded`), and a pre-existing differently-shaped table or mid-file failure leaves partial state with no documented rollback. Decide: house-style apply-once (drop `IF NOT EXISTS`, document apply-once) or idempotent guard (explicit `tier_id` + `ON DUPLICATE KEY UPDATE` / `WHERE NOT EXISTS`).
- [x] [Review][Decision] `getTier` contract verification — RESOLVED 2026-08-29: add boot-time self-check with `ERROR` log on violation (content-relative probes so it survives content tuning) — the lookup (the entire point of this story) has zero callers and zero tests; an inverted comparison or dropped `ORDER BY` would ship inert and detonate in Story 1.2. Decide: add a boot-time self-check (assert `getTier(3)`→tier 1, `getTier(12)`→tier 7, `getTier(0)`→null; log `ERROR` on violation — a new pattern for this codebase) or accept the review + manual-boot gate (the repo's current verification convention).

#### Patch

- [x] [Review][Patch] Skip rows with `min_damage > max_damage` (currently loads; inverted range would reach Story 1.2's damage roll) [src/l1j/server/server/datatables/EnchantProcTable.java:91-107]
- [x] [Review][Patch] Skip rows with `probability > 100` (column is unsigned/unbounded; 500 loads as a valid tier) [src/l1j/server/server/datatables/EnchantProcTable.java:91-107]
- [x] [Review][Patch] Normalize `damage_type` before the known-set check (trim + case-insensitive; `'Physical'` is currently skipped as unknown) [src/l1j/server/server/datatables/EnchantProcTable.java:101]
- [x] [Review][Patch] Fix misleading error message `"error while creating enchant_proc table"` — this code loads, never creates [src/l1j/server/server/datatables/EnchantProcTable.java:73]
- [x] [Review][Patch] Pin `tier_id` 1–7 explicitly in the seed INSERT (AUTO_INCREMENT + omitted ID lets tier identity drift on any non-empty pre-existing table) [db/update_087.sql:16-23]
- [x] [Review][Patch] Enforce `tier_id` ordering in code (sort loaded list) so the documented first-match tie-break doesn't depend solely on the `ORDER BY` clause in the SQL string [src/l1j/server/server/datatables/EnchantProcTable.java:109]
- [x] [Review][Patch] Make `_instance` volatile (unsynchronized publication; a concurrent early `getTier` could observe a partially populated list) [src/l1j/server/server/datatables/EnchantProcTable.java:46]
- [x] [Review][Patch] Log skipped-row count in the summary line and warn when 0 tiers load, so `7 Loaded` vs `0 Loaded` vs failed-load are distinguishable at a glance [src/l1j/server/server/datatables/EnchantProcTable.java:109]
- [x] [Review][Patch] Catch `Exception` (not only `SQLException`) around the load so a non-SQL failure can't abort boot [src/l1j/server/server/datatables/EnchantProcTable.java:72]
- [x] [Review][Patch] Guard `enchantLevel < 0` in `getTier` (corrupted item data could match a future negative-min tier) [src/l1j/server/server/datatables/EnchantProcTable.java:117]
- [x] [Review][Patch] Document the `probability` unit (percent) in javadoc — the 25 = 25% convention exists only in the SQL header comment [src/l1j/server/server/model/L1EnchantProcTier.java]

#### Deferred

- [x] [Review][Defer] No automated verification of the boot-load path — repo has no test infrastructure; verification is a one-off manual docker boot, not re-run in any normal path; a missing/broken `enchant_proc` table leaves the feature silently off [src/l1j/server/server/GameServerThread.java:299] — deferred, pre-existing
- [x] [Review][Defer] `effect_id = 0` doubles as placeholder and potentially-real effect ID with no marker — Story 1.3 selects real IDs via `update_088.sql` by design [db/update_087.sql:16-23] — deferred, pre-existing
- [x] [Review][Defer] Singleton is published even when the load fails (no retry; empty table indistinguishable from no-match at API level) — codebase-wide table pattern shared by `WeaponSkillTable` and all siblings [src/l1j/server/server/datatables/EnchantProcTable.java:51-57] — deferred, pre-existing
