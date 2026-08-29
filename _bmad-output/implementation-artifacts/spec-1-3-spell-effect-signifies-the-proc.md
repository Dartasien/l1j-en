---
title: 'Story 1.3: Spell Effect Signifies the Proc'
type: 'feature'
created: '2026-08-29'
status: 'in-review'
baseline_commit: 'NO_VCS'
review_loop_iteration: 0
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

## Verification

**Commands:**
- `grep -c "UPDATE enchant_proc" db/update_088.sql` -- expected: 7
- `ant build` -- expected: BUILD SUCCESSFUL

**Manual checks (if no CLI):**
- Apply `db/update_088.sql` to the database and verify the rows update.
- Ensure the modified code in `L1Attack` compiles cleanly.
