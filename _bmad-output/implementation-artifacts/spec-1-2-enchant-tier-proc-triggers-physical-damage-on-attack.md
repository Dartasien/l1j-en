 ---
title: 'Story 1.2: Enchant-Tier Proc Triggers Physical Damage on Attack'
type: 'feature'
created: '2026-08-29'
status: done
review_loop_iteration: 0
baseline_commit: 41df7f2308af4d22c5959545e3318cf068caacdd
context:
  - {project-root}/_bmad-output/implementation-artifacts/epic-1-context.md
  - {project-root}/_bmad-output/planning-artifacts/architecture/architecture-l1j-en-2026-08-28/ARCHITECTURE-SPINE.md
  - {project-root}/_bmad-output/implementation-artifacts/spec-1-1-enchant-proc-tier-content-loader.md
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Story 1.1 shipped the `enchant_proc` content and `EnchantProcTable` loader, but nothing consumes them — `getTier` has zero callers. Players with enchanted weapons get no combat reward from enchantment beyond the flat `_weaponEnchant` damage bonus. Epic 1's player-facing value (25%-chance bonus physical damage scaling with enchant tier) is not yet live.

**Approach:** Hook the proc into the single player-attack damage path, `L1Attack.calcPcBaseDamage`: after the kiringku replacement block (so procs apply to kiringku/magic weapons too), look up the attacker's tier via `EnchantProcTable.getInstance().getTier(_weaponEnchant)`, roll the tier's probability (weapon-skill pattern), and on success add a uniform roll within the tier's min–max damage to `damage` — which then flows through the existing physical mitigation. Gated by a new `Config.USE_ENCHANT_PROCS` flag (`UseEnchantProcs` in `config/altsettings.properties`). No packets, no opcodes, no SQL — signification is Story 1.3.

## Boundaries & Constraints

**Always:**
- All probability, damage, and toggle values come from content (`enchant_proc` rows) or `Config` — nothing hardcoded in feature code.
- The proc adds to `damage` inside `calcPcBaseDamage` so it goes through the same physical mitigation as weapon-skill procs (AC: physical mitigation applies, no magical/elemental component).
- Placement is after the kiringku block so the proc applies to all weapon types including kiringku/magic weapons (AC).
- Stacks with `weapon_skill` procs on the same hit (both are additive on `damage`; AC).
- `ThreadLocalRandom` only (worker-pool safe); the table is boot-loaded immutable — no new shared mutable state, no locking (AD-1).
- No player-state SQL, no save/load changes (AD-2). slf4j only if any logging is added.
- Compiles under the existing Ant build (NFR5).

**Ask First:**
- [x] Shipped value of `UseEnchantProcs` in `config/altsettings.properties`: RESOLVED 2026-08-29 — `True` out of the box for this server (user-approved); code default stays `False` per house pattern.
- Any deviation from the weapon-skill probability-roll idiom (`probability >= nextInt(100) + 1`).

**Never:**
- No packets, opcodes, or `S_SkillSound`/`S_UseAttackSkill` changes (Story 1.3).
- No schema changes, no new `update_*.sql` (content already shipped in `update_087.sql`).
- No changes to `EnchantProcTable` load/validation logic beyond what this story strictly needs (it is done and reviewed).
- No new third-party dependencies.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| FLAG_ON_TIER_HIT | flag on, enchant 3 (tier 1: 25%, 3–7), roll ≤ 25 | `damage` increased by a value in [3, 7]; physical mitigation applies | N/A |
| FLAG_ON_ROLL_MISS | flag on, tier matched, roll > probability | No bonus damage; hit identical to pre-feature | N/A |
| FLAG_OFF | flag off, enchanted weapon, any roll | No enchant proc; behavior byte-identical to pre-feature | N/A |
| ENCHANT_ZERO | flag on, enchant 0 (or negative/corrupt) | `getTier` returns null → no proc | N/A |
| KIRINGKU | flag on, kiringku weapon, tier matched, roll succeeds | Kiringku damage computed first, proc damage added on top | N/A |
| STACK_WEAPON_SKILL | flag on, both enchant proc and `weapon_skill` proc trigger | Both damages apply (additive) | N/A |
| RANGED | flag on, bow/gauntlet with enchanted weapon, roll succeeds | Proc applies (all weapon types) | N/A |
| FIST | flag on, fist weapon | Fist damage replacement runs first; an unenchanted fist (enchant 0) matches no tier → no proc. An *enchanted* fist (or dice-dagger) weapon DOES proc, adding tier damage on top of the replaced 1–2 fist damage — per AC1 "all weapon types" and the Design Notes (confirmed 2026-08-29, second-pass review) | N/A |
| DISABLED_TIER | flag on, tier with `probability = 0` (Story 1.1 decision: valid disable knob) | Tier matches but gate never passes → no proc | N/A |
| MIN_EQ_MAX | tier with `min_damage == max_damage` | `rollDamage` returns that exact value (no `nextInt(0)` exception) | N/A |
| FUTURE_NON_PHYSICAL | tier with a non-physical damage type (impossible today — loader accepts only `physical`) | Guard skips the proc rather than misapplying it as physical | N/A |

</frozen-after-approval>

## Code Map

- `src/l1j/server/server/model/L1Attack.java:751` -- `calcPcBaseDamage(boolean, boolean, boolean, boolean)`: the single base-damage path for player attacks (feeds `calcPcPcDamage`/`calcPcNmDamage`). Flow: weapon roll → `_weaponEnchant` added into `weaponTotalDamage` (line ~768) → `damage` assembled → weapon-skill proc `L1WeaponSkill.getWeaponSkillDamage` (line ~822) → fist replacement → **kiringku replacement block** → chainsword block → armor/cooking/doll modifiers → return. The enchant proc block goes immediately after the kiringku block.
- `src/l1j/server/server/model/L1Attack.java:294` -- `_weaponEnchant = weapon.getEnchantLevel()`; the lookup key. `Config` already imported (line 87); `ThreadLocalRandom` already imported.
- `src/l1j/server/server/datatables/EnchantProcTable.java:184` -- `getTier(int enchantLevel)`: range lookup, null when no tier (incl. ≤ 0). Boot self-checks already verify it (Story 1.1).
- `src/l1j/server/server/model/L1EnchantProcTier.java` -- row holder: `getProbability()` (percent, 0 = disabled), `getDamageType()` (normalized, currently always `physical`), `getMinDamage()`, `getMaxDamage()`. Gains one method: `rollDamage()`.
- `src/l1j/server/server/model/L1WeaponSkill.java:197` -- `getWeaponSkillDamage`: the probability-roll idiom to mirror (`getProbability() < ThreadLocalRandom.current().nextInt(100) + 1` → no proc) and the additive-on-`damage` precedent.
- `src/l1j/server/Config.java:320` / `:954` -- `USE_INT_PROCS` declaration / load: the exact pattern for the new `USE_ENCHANT_PROCS` flag (`altSettings.getProperty("UseEnchantProcs", "False")`).
- `config/altsettings.properties:192` -- `UseIntProcs = False`: the new `UseEnchantProcs` key lands next to it.
- `db/update_087.sql` -- seed content: 7 tiers, all 25% / physical / `effect_id` 0; tier 1 starts at `min_enchant = 1` (enchant 0 → no tier → no proc, AC satisfied by content).

## Tasks & Acceptance

**Execution:**
- [x] `src/l1j/server/Config.java` -- add `public static boolean USE_ENCHANT_PROCS;` next to `USE_INT_PROCS` (line ~320) and load it in `loadAltSettings` next to the `USE_INT_PROCS` load (line ~954) via `altSettings.getProperty("UseEnchantProcs", "False")`
- [x] `config/altsettings.properties` -- add `UseEnchantProcs = True` next to `UseIntProcs` (shipped value per Ask First; code default stays `False`)
- [x] `src/l1j/server/server/model/L1EnchantProcTier.java` -- add `public int rollDamage()`: uniform roll in `[minDamage, maxDamage]` via `_minDamage + ThreadLocalRandom.current().nextInt(_maxDamage - _minDamage + 1)`; javadoc notes min==max returns the constant
- [x] `src/l1j/server/server/model/L1Attack.java` -- in `calcPcBaseDamage`, immediately after the kiringku block: if `Config.USE_ENCHANT_PROCS`, look up `EnchantProcTable.getInstance().getTier(_weaponEnchant)`; when non-null, damage type is `physical`, and `tier.getProbability() >= ThreadLocalRandom.current().nextInt(100) + 1`, add `tier.rollDamage()` to `damage` (import `EnchantProcTable`; tier class is same-package)

**Acceptance Criteria:**
- Given the `UseEnchantProcs` flag is enabled and I attack with any weapon (all weapon types, including kiringku/magic weapons) at enchant level ≥ 1 within a configured tier, when the probability roll succeeds, then bonus damage within the tier's min–max range is added to the hit, applied as the tier's configured damage type (all current tiers: physical)
- Given the config flag is disabled, when I attack with an enchanted weapon, then no enchant proc occurs and existing behavior is unchanged
- Given a weapon at enchant level 0 or lower, when I attack, then no enchant proc occurs
- Given the enchant proc and an existing `weapon_skill` proc both trigger on the same hit, when the hit resolves, then both damages apply (they stack)
- Given a tier configured with physical damage is applied to the target, when the hit resolves, then physical mitigation applies and no magical or elemental component is involved
- No probability, damage, or toggle values are hardcoded in feature code; the attack path runs on the worker pool with no new shared mutable state lacking a documented lock owner (AD-1); no player-state SQL from feature code (AD-2); the change compiles under the existing Ant build (NFR5)

## Design Notes

**Placement after kiringku, not before:** the kiringku block *replaces* `damage` (`damage = L1WeaponSkill.getKiringkuDamage(...)`). A proc added before it would be silently discarded for kiringku weapons — the AC explicitly requires kiringku coverage. After the block, the proc is additive on whatever the final base damage is (normal, fist-replaced, dice-dagger-replaced, or kiringku-replaced).

**Probability idiom:** mirrors `L1WeaponSkill.getWeaponSkillDamage` exactly — roll `nextInt(100) + 1` (1..100), proc when `roll <= probability`. Consequences: `probability = 25` → 25% (matches seed content and the AC's "25% chance"); `probability = 0` → never procs (consistent with the Story 1.1 decision that 0 is a valid disable knob); `probability = 100` → always procs.

**Physical-type guard:** the loader currently accepts only `physical`, so the guard can never fire today — it is defense-in-depth so that if a future content change ever introduces a non-physical tier, it is skipped (no proc) rather than silently misapplied as physical damage. Non-physical proc application is out of scope for this story.

**No `rollProcDamage` helper on the table:** the gate + lookup + roll is three lines at the call site, identical in shape to the existing weapon-skill call; a helper would add indirection without reuse (single call site). `rollDamage()` lives on the tier because it is the tier's own data being rolled.

**Flag default:** code default `False` follows the house pattern (`UseIntProcs`, `UseAutoStone` all default `False`); the shipped properties file enables it so the feature is live for this server out of the box.

## Verification

**Commands:**
- `ant compile` (in `l1j-build-check` container) -- expected: BUILD SUCCESSFUL, no new warnings from the changed files
- Scratch harness (compiled against `build/classes` inside the build container, **not committed**): construct `L1EnchantProcTier` instances directly and assert (a) `rollDamage()` stays within [min, max] over 100k rolls for an asymmetric range and hits both endpoints, (b) `rollDamage()` with min==max returns the constant, (c) the gate expression `prob >= nextInt(100)+1` passes ≈25% over 100k rolls at prob 25 and 0% at prob 0, (d) `getTier` boundaries via the boot self-check (already covered by Story 1.1 boot)

**Manual checks (if no CLI):**
- Fresh docker boot with `UseEnchantProcs = True`: server boots clean, `enchant_proc` loads 7 tiers, no new errors/warnings
- Fresh docker boot with `UseEnchantProcs = False`: server boots clean (flag-off path)
- In-game (requires client, user-run): +1 weapon attacks occasionally exceed baseline damage by 3–7; kiringku weapon procs too; flag off → no procs

**Verification results (08-29-2026):**
- `ant compile` in `l1j-build-check` container: BUILD SUCCESSFUL (3 changed sources recompiled)
- Scratch harness (system temp, compiled against `build/classes` in the build container, not committed) — ALL PASS:
  - `rollDamage` within [3,7] over 100k rolls; reaches both endpoints 3 and 7
  - `rollDamage` with min==max returns the constant 7 (no `nextInt(0)` exception)
  - gate expression (exact `L1Attack` form) at probability 25 passes 24.846% of 100k rolls (~25%)
  - gate at probability 0 never passes; at probability 100 always passes
- Live boot, flag ON (shipped config): `docker compose build l1jserver` + fresh stack (project `l1jverify3`, fresh MariaDB 12.0.2 initialized from `db/`) — log shows `List of enchant proc tiers: 7 Loaded (0 skipped)`, `Database tables loaded successfully!`, `Starting networking`; no ERROR/Exception lines
- Live boot, flag OFF (`UseEnchantProcs = False`, server restart): boots clean, table still loads (content load is flag-independent by design), no errors; config reverted to `True` afterwards
- Environment torn down (containers, `l1jverify3_my-db` volume, `.env`, harness dir removed)

**Matrix coverage audit:**
- FLAG_ON_TIER_HIT — harness (damage range [3,7] + ~25% trigger) ran & passed; the additive-on-`damage` integration is the reviewed 5-line block (compile-verified); end-to-end in-game hit is user-run (needs client)
- FLAG_ON_ROLL_MISS — harness gate frequency (75.154% of rolls miss at prob 25) ran & passed
- FLAG_OFF — flag-off live boot ran & passed; block is gated on `Config.USE_ENCHANT_PROCS` as first condition (no lookup, no roll, no addition when off)
- ENCHANT_ZERO — `getTier` range check (`enchantLevel >= getMinEnchant()`) + `< 0` guard (Story 1.1) + seed content (tier 1 `min_enchant = 1` in `db/update_087.sql`); in-game check user-run
- KIRINGKU — block placed after the kiringku replacement (code map/inspection); in-game check user-run
- STACK_WEAPON_SKILL — both procs additive on `damage`, neither replaces the other (inspection); in-game check user-run
- RANGED — block sits after bow/gauntlet damage assembly (inspection); in-game check user-run
- FIST — fist replacement precedes the block; fist weapons carry enchant 0 → no tier (inspection + content); in-game check user-run
- DISABLED_TIER — harness prob-0 gate ran & passed (never passes)
- MIN_EQ_MAX — harness constant roll ran & passed
- FUTURE_NON_PHYSICAL — physical-type guard is a first-class condition in the block (inspection; untriggerable with current content since the loader accepts only `physical`)

### Review Findings

_Code review 2026-08-29 (layers: blind-hunter, edge-case-hunter, verification-gap; inline passes — same model/session, not independent LLMs; prompts with embedded diff saved as `review-prompt-*-1-2.md`). 12 blind-hunter + 4 edge-case + 3 verification-gap findings; 6 merged/deduped; 5 rejected as noise or already-tracked._

#### Patch (all applied)

- [x] [Review][Patch] No comment on the `L1Attack` proc block explaining the placement constraint — a future move of the block (or the kiringku block) above/below the other would silently lose kiringku coverage [src/l1j/server/server/model/L1Attack.java:834] — added explanatory comment
- [x] [Review][Patch] Block runs `getTier(0)` + full tier scan for the common unenchanted-attack case with no call-site guard for `_weaponEnchant <= 0` (no-proc-at-0 relied solely on DB content) [src/l1j/server/server/model/L1Attack.java:834] — added `_weaponEnchant > 0` precondition (behavior-identical: `getTier` returns null for ≤ 0 anyway)
- [x] [Review][Patch] `"physical".equalsIgnoreCase` per-attack case-insensitive compare although the loader normalizes `damage_type` to exactly lowercase `physical` [src/l1j/server/server/model/L1Attack.java:839] — switched to `equals`
- [x] [Review][Patch] `rollDamage()` could throw `IllegalArgumentException` on the attack path: `_maxDamage - _minDamage + 1` overflows `int` for extreme loaded values (e.g. `max_damage = 2147483647`, `min_damage = 0` passes loader validation), and a directly-constructed tier with `min > max` yields `bound <= 0` [src/l1j/server/server/model/L1EnchantProcTier.java:90] — span now computed in `long`; inverted range degrades to `minDamage`
- [x] [Review][Patch] No boot-log line for `USE_ENCHANT_PROCS` — the flag's deployed state was unobservable, and the flag-off boot test had no observable signal (a misspelled property key or deleted load line would pass both boot tests with the feature silently off) [src/l1j/server/server/GameServerThread.java] — added `EnchantProcs = On/Off` boot line (house pattern per `PvP`/`IngameNews`); post-patch boot tests confirm the wiring both directions (`On` with shipped config, `Off` after flipping the property)

#### Deferred

- [x] [Review][Defer] Attack-path integration has no re-runnable automated verification: the `L1Attack` proc line (gate operator, additivity, placement) is exercised by no test in any normal path — the scratch harness verifies a re-implementation of the gate expression, not the production line, and the core AC (bonus damage on a hit) is user-run with a client. Root cause: repo has no test infrastructure (pre-existing; complements the Story 1.1 boot-load deferral) [src/l1j/server/server/model/L1Attack.java:834-846] — deferred

#### Rejected (noise / already-tracked)

- No server-side log when a proc fires — Story 1.3's `S_SkillSound` is the epic's visibility mechanism; per-hit hot-path logging is not a house pattern (`L1WeaponSkill` does not log its procs)
- Feature silently off if `enchant_proc` table missing/failed — pre-existing, already deferred in Story 1.1 (singleton published on load failure)
- No boot check of flag+table combination — after the flag-log patch, both states are auditable in the boot log (`EnchantProcs = On` + `7 Loaded` / `0 Loaded` warning)
- `probability` outside 0–100 via direct construction — loader is the sole constructor; public unvalidated constructor is the house pattern shared with `L1WeaponSkill`
- Config key naming vs `EnchantWeapon Change` rates — key follows the `Use*Procs` precedent (`UseIntProcs`); comment disambiguates
- Harness not committed / one-shot — same root cause as the deferred no-test-infrastructure finding

**Verification results after review patches (08-29-2026):**
- `ant compile` in `l1j-build-check` container: BUILD SUCCESSFUL (3 changed sources recompiled)
- Extended scratch harness — ALL PASS (8 checks): prior 6 checks plus `rollDamage` with `min > max` degrades to `minDamage` (no throw) and `max = Integer.MAX_VALUE` stays in range (no overflow throw)
- Live boot, flag ON (patched image, fresh stack `l1jverify4`, fresh MariaDB 12.0.2): `EnchantProcs = On`, `List of enchant proc tiers: 7 Loaded (0 skipped)`, `Database tables loaded successfully!`, `Starting networking`; no ERROR/Exception lines
- Live boot, flag OFF (property flipped to `False`, server restart): `EnchantProcs = Off` — proves the `UseEnchantProcs` key is actually read from `altsettings.properties` (the pre-patch flag-off boot had no observable signal); table still loads, boot clean; config reverted to `True`
- Environment torn down (containers, `l1jverify4_my-db` volume, `.env`, harness dir removed)

### Review Findings — second pass

_Code review 2026-08-29, second pass over the post-patch diff `41df7f23..45f16456` (layers: blind-hunter, edge-case-hunter, verification-gap, acceptance-auditor; inline passes — same model/session, not independent LLMs). 18 normalized findings; 11 dismissed as noise/already-adjudicated; 2 decision-needed, 1 patch, 4 deferred._

#### Decision-needed

- [x] [Review][Decision] No magnitude sanity bound on `enchant_proc` damage content — `max_damage = 2147483647` passes all four loader checks (shape-only validation: min>max, negative, probability>100, unknown type) and would add up to ~2.1B to `damage` pre-mitigation on a proc. Loader validation scope was fixed in the Story 1.1 review, and this spec's "Never" forbids `EnchantProcTable` load/validation changes in this story — a magnitude cap requires spec renegotiation. — RESOLVED 2026-08-29: accept GM content authority (defer) — stop-the-world, GM-reviewed SQL per AD-2; a 2.1B-damage row requires deliberate bad content. No loader change.
- [x] [Review][Decision] FIST matrix row contradicts AC1 for enchanted fist weapons — the frozen I/O matrix FIST row expects "no proc" on the premise "fist weapons carry enchant 0", but `L1CreateItem` (GM) can create fist or dice-dagger weapons with enchant ≥ 1, and the code + Design Notes intend the proc to apply on top of fist-replaced / dice-dagger-replaced damage (AC1: "all weapon types"). The code follows AC1; the matrix row's rationale is the inaccurate part. — RESOLVED 2026-08-29: intent confirmed — enchanted fist/dice-dagger weapons DO proc (code stands); correct the frozen matrix row's rationale (human-approved frozen-section edit).

#### Patch

- [x] [Review][Patch] `UseEnchantProcs` properties comment doesn't point at where tier values are tuned — extend the comment to note that per-tier probability/damage values live in the `enchant_proc` DB table (content tunable without code) [config/altsettings.properties:194] — applied 2026-08-29
- [x] [Review][Patch] Correct the FIST matrix row rationale — replace "fist weapons carry enchant 0 → `getTier(0)` null → no proc" with the actual contract: an unenchanted fist weapon procs no tier; an *enchanted* fist (or dice-dagger) weapon DOES proc on top of the replaced damage per AC1 "all weapon types" and the Design Notes [spec I/O matrix FIST row] — applied 2026-08-29 (human-approved frozen-section edit)

#### Deferred

- [x] [Review][Defer] No re-runnable verification of the production proc gate line — the gate/additivity at `L1Attack.java:837-846` is exercised by no test in any normal path; the scratch harness verified a re-implementation of the gate expression (since deleted). Flipping `>=` to `<` (proc fires 75% instead of 25%) passes compile and boot. Root cause: no test infrastructure (pre-existing; complements the Story 1.1 boot-load deferral and the first-pass attack-path deferral) [src/l1j/server/server/model/L1Attack.java:837-846] — deferred, pre-existing
- [x] [Review][Defer] Kiringku placement contract pinned only by a comment — moving the proc block above the kiringku replacement would silently remove kiringku coverage (AC + KIRINGKU matrix row violation); no re-runnable check observes ordering. The explanatory comment (first-pass patch) is the only guard [src/l1j/server/server/model/L1Attack.java:834-846] — deferred, pre-existing
- [x] [Review][Defer] Recorded verification evidence is one-shot and non-reproducible — the scratch harness was not committed and its dir removed; docker environments torn down. The spec's ALL PASS claims (incl. `rollDamage` endpoint coverage) cannot be re-established in any normal path; a `nextLong(span + 1)` → `nextLong(span)` regression would fail no re-runnable check [spec Verification results sections] — deferred, pre-existing
- [x] [Review][Defer] Table load failure indistinguishable from no-tier at the call site — `getInstance()` returns an empty singleton on failed load; the feature is silently off while the boot banner reports `EnchantProcs = On` (banner at `GameServerThread.java:174` also prints before the table loads at `:305`, though both signals land in the same boot log). Pre-existing codebase-wide table pattern [src/l1j/server/server/model/L1Attack.java:838] — deferred, pre-existing (tracked in Story 1.1)

#### Dismissed (11)

Non-physical silent skip at call site (loader already rejects non-physical with a warning; guard is spec-sanctioned defense-in-depth) · no observability when a proc fires (first-pass rejection: Story 1.3 `S_SkillSound` is the visibility mechanism) · boot banner ordering vs table load (both signals auditable in one boot log; first-pass rejection stands) · code-default-False vs shipped-True / missing key silent (house pattern; deployed state observable via boot banner) · no `tryProc` single entry point (first-pass Design Note: single call site, helper adds indirection) · probability-0 tier still pays a random roll (negligible hot-path cost; mirrors weapon-skill idiom) · stale `review-prompt-*-1-2.md` artifacts embed pre-patch diff (historical record of the first loop, accurate at time of run) · 1.2-only deployment ships un-signified damage (epic sequencing by design; Story 1.3 is next) · frozen task text prescribes pre-patch `rollDamage` formula (divergence documented in first-pass Review Findings; frozen section must not be edited) · unsigned DB wrap to negative `rs.getInt` values (loader's negative + min>max checks catch every wrap combination — verified in code) · dice-dagger stacking (Design Notes explicitly cover dice-dagger-replaced damage).

## Suggested Review Order

**Attack-path integration (entry point)**

- The whole feature in five lines: flag + enchant>0 + tier + physical + gate → additive damage
  [`L1Attack.java:834`](../../src/l1j/server/server/model/L1Attack.java#L834)

- Placement comment: why the block must stay after the kiringku replacement
  [`L1Attack.java:834`](../../src/l1j/server/server/model/L1Attack.java#L836)

**Damage roll**

- Uniform [min,max] roll; long-span hardening against overflow and inverted ranges
  [`L1EnchantProcTier.java:94`](../../src/l1j/server/server/model/L1EnchantProcTier.java#L94)

**Feature flag**

- Declaration next to `USE_INT_PROCS`
  [`Config.java:321`](../../src/l1j/server/Config.java#L321)

- Load with `False` code default (house pattern)
  [`Config.java:957`](../../src/l1j/server/Config.java#L957)

- Boot log makes the deployed state auditable and the key wiring verifiable
  [`GameServerThread.java:173`](../../src/l1j/server/server/GameServerThread.java#L173)

**Shipped config**

- `True` out of the box for this server (user decision 2026-08-29)
  [`altsettings.properties:195`](../../config/altsettings.properties#L195)
