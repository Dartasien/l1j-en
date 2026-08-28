# Epic 1 Context: Enchant-Scaled Weapon Procs

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

A player who has successfully enchanted a weapon (enchant level ≥ 1, all weapon types including kiringku/magic weapons) gains a proc that can trigger on a successful attack: bonus damage that scales with the weapon's enchant tier, signified by a spell effect while the damage stays physical per current tiers. Tier definitions (enchant range, trigger chance, min/max damage, damage type, effect) are data-driven DB content tunable without code, and the whole feature is toggleable via config. This makes enchantment investment meaningfully rewarded in combat on the 2009 US server, without any client change.

## Stories

- Story 1.1: Enchant Proc Tier Content & Loader
- Story 1.2: Enchant-Tier Proc Triggers Physical Damage on Attack
- Story 1.3: Spell Effect Signifies the Proc

## Requirements & Constraints

- Trigger: 25% chance on a successful attack when the weapon's enchant level falls in a configured tier; no proc at enchant ≤ 0 or when the config flag is off (behavior then identical to today).
- Damage: within the tier's configured min–max range, applied as the tier's configured damage type (all current seed rows: physical — physical mitigation applies, no magical/elemental component).
- Stacking: enchant procs stack with existing `weapon_skill` procs on the same hit; both damages apply.
- Signification: the tier's configured effect is shown to attacker and nearby players using existing effect packets only — no new packets or opcodes; the unmodified 2009 US client must render it with no protocol errors.
- No side effects: the proc effect applies no buff, status, or attribute change to attacker or target — purely visual signification.
- No persistence regression: enchant level save/restore is untouched; the proc tier follows the restored enchant level after logout/login and full server restart.
- Content: new `enchant_proc` DB table (tier id, min enchant, max enchant, probability, damage type, min damage, max damage, effect id) shipped as a versioned `db/update_NNN.sql` (next free number; `update_087.sql` at compile time) with seed rows for the 7 draft tiers. The `damage_type` column allows non-physical kinds in future content without schema changes.
- Malformed rows (min > max, negative damage, unknown damage type) are skipped with an slf4j warning; the server boots normally.
- No probability, damage, or toggle values hardcoded in feature code; the config on/off key lives in `config/*.properties` read through `Config`.
- Story 1.1 specifically is inert: content + loader only, no attack-path changes.

## Technical Decisions

- Follow the `weapon_skill` precedent: `WeaponSkillTable` loads the `weapon_skill` DB table (probability, fix/random damage, `skill_id`, `effect_id`). The new `EnchantProcTable` goes in `datatables/` as a singleton with `*Table` naming, loads all rows at boot, and exposes lookup by enchant level (no tier match → no proc).
- Damage integration point: `L1Attack` damage calculation already reads `_weaponEnchant` and calls `L1WeaponSkill.getWeaponSkillDamage(_pc, _target, _weaponId)` — the enchant proc hooks alongside this.
- Enchant level source: `L1ItemInstance._enchantLevel` (persisted in `character_items.enchantlvl` via the item storage path). No new player-state persistence.
- Effect signification follows the `L1WeaponSkill` pattern using existing `S_SkillSound` / `S_UseAttackSkill` packets; actual client effect IDs are selected from existing 2009 US effect IDs at implementation (must render on the stock client).
- Threading: the attack path runs on the 10-thread `PacketConsumer` worker pool with no main-thread guarantee. Any new shared mutable state needs an explicit, documented lock owner; a boot-loaded immutable table needs none.
- Package placement: new classes in the matching subpackage under `l1j.server.server` (`datatables/` for the table) — never the flat root.
- Content placement: DB-driven content ships as versioned SQL in `db/` taking the next free `update_NNN.sql` number.
- Draft tier table (seed content, tunable without code): +1–+5 → 3–7 dmg, lightning-bolt effect; +6–+7 → 5–9, fire; +8 → 7–12, ice shard; +9 → 9–16, holy flame; +10 → 12–21, dark lightning; +11 → 16–27, inferno; +12+ → 22–35, meteor strike. All 25% chance, physical. Effect names are placeholders pending real 2009 US effect ID selection.
- Conventions: slf4j only (one logger per class, no `System.out`), no hardcoded rates/flags, config via `Config`, change must compile under the existing Ant build.
- Verification: no test infrastructure exists; review against the architecture spine (AD-1..AD-7 + conventions) is the verification gate.

## Cross-Story Dependencies

- 1.1 → 1.2: the attack-path trigger needs the `EnchantProcTable` lookup and the config flag.
- 1.2 → 1.3: the spell-effect signification attaches to the damage application 1.2 introduces.
- 1.3 also carries the persistence-regression acceptance (enchant save/restore unaffected).
- No dependencies outside this epic; existing `weapon_skill` proc behavior is preserved, not modified.
