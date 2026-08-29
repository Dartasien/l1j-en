---
title: 'Story 1.3: Spell Effect Signifies the Proc'
type: 'feature'
created: '2026-08-29'
status: 'done'
baseline_commit: 'NO_VCS'
review_loop_iteration: 1
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Enchant procs trigger correctly (Stories 1.1 and 1.2) and apply physical damage, but lack visual feedback. The placeholder `effect_id` (0) in the `enchant_proc` table means the trigger is invisible to the player and nearby observers.

**Approach:** Ship `db/update_088.sql` to update the 7 seed tiers with valid 2009 US client effect IDs. Wire the attack path (`L1Attack`) to broadcast these effects to the attacker and nearby players using the `L1WeaponSkill` signification pattern (`S_SkillSound` or `S_UseAttackSkill`), ensuring no side effects or enchant persistence regressions.

## Boundaries & Constraints

**Always:** 
- Use existing 2009 US client effect IDs only; no new client data.
- The effect is purely visual; no buffs, status, or attribute changes applied.
- Enchant level save/restore logic must remain completely untouched.
- `update_088.sql` must handle existing rows gracefully (e.g. `UPDATE enchant_proc SET effect_id = ... WHERE tier_id = ...`).

**Ask First:** 
- If the chosen effect IDs appear broken or misaligned in-game during testing (we chose: 10, 1811, 1810, 2165, 3924, 1819, 762).

**Never:** 
- Do not modify existing `weapon_skill` behavior.
- Do not change how damage is calculated or applied.
- Do not apply any magical/elemental component to the damage.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Valid Proc on Melee | Melee weapon attack triggers tier proc (e.g. tier 1) | `S_SkillSound` packet with `effect_id = 10` broadcasted to target | N/A |
| Valid Proc on Ranged | Bow/Sting attack triggers tier proc | `S_UseAttackSkill` with effect broadcasted | N/A |
| Missing Effect | `effect_id` is 0 or unconfigured | No effect packet sent, damage still applies | N/A |

</frozen-after-approval>

## Code Map

- `src/l1j/server/server/model/L1Attack.java` -- Damage integration point where the `EnchantProcTable` is queried. Here we need to check `enchantProcTier.getEffectId()` and dispatch the appropriate `S_SkillSound` or `S_UseAttackSkill` packet.
- `src/l1j/server/server/model/L1WeaponSkill.java` -- The existing pattern for `S_SkillSound` / `S_UseAttackSkill` dispatch. Used as reference, no modification needed.
- `db/update_088.sql` -- New SQL file to update the 7 seed tiers with real effect IDs.

## Tasks & Acceptance

**Execution:**
- [x] `db/update_088.sql` -- create -- Update `enchant_proc` table to set real `effect_id` values for `tier_id` 1 through 7 (10, 1811, 1810, 2165, 3924, 1819, 762).
- [x] `src/l1j/server/server/model/L1Attack.java` -- modify -- When an enchant proc triggers and damage is rolled, check if `effect_id > 0`. If so, dispatch the effect packet (using `S_UseAttackSkill` for Bow/Sting, or `S_SkillSound` for melee). Broadcast it to nearby players.

**Acceptance Criteria:**
- Given `db/update_088.sql` is applied, when querying `enchant_proc`, then the 7 seed rows have non-zero `effect_id`s.
- Given an attacker triggers an enchant proc, when the damage is applied, then the configured visual effect is broadcast to the attacker and nearby players.
- Given a bow/sting weapon triggers an enchant proc, when the damage is applied, then the effect travels from attacker to target via `S_UseAttackSkill`.
- Given a melee weapon triggers an enchant proc, when the damage is applied, then the effect bursts on the target via `S_SkillSound`.

## Spec Change Log

- 2026-08-29 (review loop 1) -- Triggering finding: the Verification grep `grep -c "UPDATE enchant_proc" db/update_088.sql` (expected 7) matches nothing in the actual file shape (`ON DUPLICATE KEY UPDATE`), so the check was inert (returns 0); the same review surfaced that the committed `L1Attack` change did not compile (`_isArrowType` is a private field of `L1WeaponSkill`, undefined in `L1Attack`) and that `update_088.sql` seeded tier 7 `max_enchant = 127` vs the canonical `255` in `update_087.sql`. Amended: (a) Verification grep now matches the real upsert shape; (b) added an explicit seed-consistency constraint so re-derivation cannot diverge from `update_087.sql`; (c) `ant build` is a hard gate on the final code. Known-bad state avoided: an inert verification gate letting a non-compiling story reach review with every check green or unrun. KEEP instructions for re-derivation: (1) proc block stays AFTER the kiringku block with its explanatory comment (Story 1.2 contract); (2) keep the `effect_id > 0` guard (Missing Effect matrix row); (3) packet construction must mirror the `L1WeaponSkill` reference pattern exactly -- `S_UseAttackSkill(attacker, targetId, effectId, targetX, targetY, ActionCodes.ACTION_Attack, false)` for ranged, `S_SkillSound(targetId, effectId)` for melee (hub = target); (4) ranged means the local `L1Attack` field `isRanged` (`isBow | isGauntlet` = spec's "Bow/Sting") -- do NOT use `_isArrowType`, which does not exist in `L1Attack` and will not compile; (5) `update_088.sql` upserts must touch ONLY `effect_id` (preserve operator-tuned content) and every other seed column must match `update_087.sql` exactly (tier 7 `max_enchant = 255`); (6) broadcast to attacker + nearby via `_pc.sendAndBroadcast`.

## Verification

**Commands:**
- `grep -c "ON DUPLICATE KEY UPDATE" db/update_088.sql` -- expected: 7
- `ant build` -- expected: BUILD SUCCESSFUL (hard gate: must pass on the final committed code, not an earlier revision)

**Seed consistency:** every non-`effect_id` column in `update_088.sql` must equal the corresponding seed in `db/update_087.sql` (in particular tier 7 `max_enchant = 255`).

**Manual checks (if no CLI):**
- Apply `db/update_088.sql` to the database and verify the rows update.
- Ensure the modified code in `L1Attack` compiles cleanly.
- In-game (requires 2009 US client, user-run): melee proc bursts the configured effect on the target; bow/sting proc shows the effect flying attacker→target; observers nearby see it; `effect_id = 0` tier shows no effect but still deals proc damage.

**Verification results (08-29-2026, review loop 1 re-derivation):**
- `grep -c "ON DUPLICATE KEY UPDATE" db/update_088.sql` → 7 ✓
- `ant compile` in `l1j-build-check` container: BUILD SUCCESSFUL (1 changed source recompiled) ✓
- Throwaway MariaDB 12.0.2: applied `update_087.sql` then `update_088.sql`; all 7 rows present with effect_ids 10, 1811, 1810, 2165, 3924, 1819, 762; tier 7 `max_enchant = 255` (matches 087); `COUNT(effect_id > 0)` = 7 ✓
- Scratch harness (in git-ignored `build/harness13`, compiled against `build/` in the build container, **not committed**, removed after run) — ALL PASS (87 checks):
  - Melee matrix row: `S_SkillSound(targetId, fx)` carries target objid + effect gfx for all 7 seed IDs (LE layout `C D H H D`, pad-to-4 → 16 bytes)
  - Ranged matrix row: `S_UseAttackSkill(attacker, targetId, fx, tx, ty, ACTION_Attack, motion=false)` carries actionId, target, gfx, target XY for all 7 seed IDs (32 bytes)
  - Missing-Effect matrix row: guard expression `getEffectId() > 0` as written in `L1Attack` — 0 suppresses, 10 dispatches (re-implementation check, same rigor as Story 1.2 gate test)
- In-game client check remains user-run (no 2009 US client in CI); packet-level coverage above substitutes for branch/packet-shape verification.

## Suggested Review Order

**Effect dispatch (entry point)**

- Missing-Effect matrix row: `effect_id` 0 sends no packet, damage still applies
  [`L1Attack.java:848`](../../src/l1j/server/server/model/L1Attack.java#L848)

- Branch on `isRanged` — Bow + Sting per spec; the loop-1 compile breaker lived here
  [`L1Attack.java:849`](../../src/l1j/server/server/model/L1Attack.java#L849)

- Ranged: mirrors the `L1WeaponSkill` reference pattern exactly (motion off)
  [`L1Attack.java:850`](../../src/l1j/server/server/model/L1Attack.java#L850)

- Melee: burst hubs on the target, broadcast to attacker + nearby
  [`L1Attack.java:852`](../../src/l1j/server/server/model/L1Attack.java#L852)

**Content data**

- Header pins the 087 dependency and the seed-consistency contract
  [`update_088.sql:1`](../../db/update_088.sql#L1)

- Tier 1 upsert: touches only `effect_id`, preserves operator-tuned values
  [`update_088.sql:7`](../../db/update_088.sql#L7)

- Tier 7: `max_enchant = 255` matches the canonical 087 seed (loop-1 fix)
  [`update_088.sql:13`](../../db/update_088.sql#L13)
