---
title: 'Story 1.1: Enchant Proc Tier Content & Loader'
type: 'feature'
created: '2026-08-28'
status: 'in-review'
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
