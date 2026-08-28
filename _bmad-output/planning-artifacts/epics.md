---
stepsCompleted: [step-01-validate-prerequisites, step-02-design-epics, step-03-create-stories]
inputDocuments:
  - _bmad-output/specs/spec-l1j-en/SPEC.md
  - _bmad-output/planning-artifacts/architecture/architecture-l1j-en-2026-08-28/ARCHITECTURE-SPINE.md
---

# l1j-en - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for l1j-en, decomposing the requirements from the SPEC (PRD-equivalent canonical contract), the Architecture Spine, and related constraints into implementable stories.

**Scope note (ratify-as-is):** l1j-en is a mature, feature-complete brownfield emulator for the final 2009 US Lineage 1 client. The contract fixes the invariants the code already obeys so that future work (human or agent) cannot drift from or silently break them. Capability requirements describe the feature surface that must remain true; architecture requirements (AD-1..AD-7 + conventions) bind all new work.

## Requirements Inventory

### Functional Requirements

FR1: The server runs the full 2009 US character lifecycle — creation, leveling, skills, equipment, inventory, deletion. A character created on a fresh DB can level, equip, and use skills, and retains all state across a server restart. (CAP-1)

FR2: The server simulates maps, monster spawns, movement, skills, and drops per the configured tables. A player kills a spawned mob, receives the configured drops, and respawn honors `spawnlist` timing. (CAP-2)

FR3: Adena, shops, auction, trade, mail/letters, and getback work per 2009 US rules. An item moved between players via trade, auction, or mail persists correctly on both sides after a restart. (CAP-3)

FR4: Parties, clans/pledges, castles including siege/war, and houses function. A clan captures and holds a castle; war times and ownership persist across a restart. (CAP-4)

FR5: Boss cycles, UB, timed dungeons, light/dark, and the other scheduled events run on their configured schedules. Each configured event fires on schedule with player-visible state transitions. (CAP-5)

FR6: GMs operate the live server through in-game commands plus telnet/ssh consoles, with logging and bans. A GM executes each registered command against the live world and ban actions take effect. (CAP-6)

FR7: Static content (DB, `data/xml`, `maps/`, `config/`) loads at boot, and content changes ship as versioned files applied stop-the-world. A content change shipped per spine AD-4 is live after a stop-the-world restart with zero code changes. (CAP-7)

FR8: An unmodified 2009 US client connects, plays, and receives all server features. The stock client logs in, moves, fights, trades, and uses GM features with no protocol errors. (CAP-8)

FR9: A weapon whose successful enchant level falls in a configured tier range gains a proc that can trigger on a successful attack. (New feature)

FR10: The enchant-tier proc deals additional physical damage within the tier's configured min–max range. (New feature)

FR11: The proc trigger is signified to the client with the tier's configured spell/effect, with no magical status or attribute side effects. (New feature)

FR12: Tier definitions (enchant range, probability, min/max damage, effect ID) are data-driven content shipped per AD-4 (DB table + versioned `db/update_NNN.sql`, following the `weapon_skill` precedent). (New feature)

FR13: The enchant-tier proc feature has an on/off toggle in `config/*.properties` read through `Config`. (New feature)

FR14: Enchant level persistence and restore are unaffected by the new feature (no regression to existing enchant save/load). (New feature)

### NonFunctional Requirements

NFR1: Existing packet byte layouts and semantics are immutable; the unmodified 2009 US client must keep working. New packets/opcodes for modified clients are allowed only when gated on client version/feature detection (`C_ClientVersion`) so stock clients never receive packets they cannot parse; new opcodes are allocated in `encryptions/Opcodes.java` from unused opcode space, one distinct value per feature, recorded in the change. (AD-3)

NFR2: Until a test strategy is adopted, review against the architecture spine (AD-1..AD-7 + consistency conventions) is the sole verification gate for new work.

NFR3: The operational envelope is single-node Docker Compose (server + MariaDB + nginx); no high-availability, multi-node, sharding, or scaling topology.

NFR4: Ratify-as-is scope: build-system migration (Ant → other) and Java version upgrade are explicitly out of scope; they are deliberate future amendments to the spine, not current work. Client development is out of scope.

NFR5: Success signal — a new feature or content change built against the contract compiles under the existing Ant build, runs under the unmodified 2009 US client, persists correctly across a restart, and passes a review against the spine with zero AD violations.

### Additional Requirements

- AD-1 (threading): Game logic runs concurrently on the 10-thread `PacketConsumer` pool, `GeneralThreadPool`, and telnet/ssh GM threads; no main-thread guarantee, no global lock. New code mutating shared state must `synchronized` on the object owning the state (existing per-object pattern). Any new shared mutable state requires an explicit, documented lock owner; the legacy no-lock pattern may be inherited but not extended.
- AD-2 (persistence): In-memory `L1World` is the runtime authority. The DB is a snapshot written only by save paths (logout / scheduled / shutdown) through `datatables/` + `storage/`; no feature code issues player-state SQL and no runtime code reads the DB expecting live values. Content changes (`db/`, `data/`, `maps/`, `config/`) are applied with the server stopped and take effect on boot.
- AD-3 (protocol): Frozen baseline with gated extensions — see NFR1.
- AD-4 (content placement): New static content goes in the same layer as the closest existing content of that kind (DB-driven: NPCs, spawns, drops, dungeons → SQL; XML-driven: item making, quests, teleporters, boss cycles → `data/xml/`). New content kind with no match defaults to DB. Content is never moved between layers as a side effect of a feature. DB schema/content changes ship as versioned SQL in `db/` following `update_*.sql` (optional/variant content in `db/optional/`), taking the next free `update_NNN.sql` number.
- AD-5 (package placement): New classes go into the matching concern subpackage under `l1j.server.server` (`model/`, `clientpackets/`, `serverpackets/`, `controllers/`, `datatables/`, `storage/`, `command/`, `network/`, `taskmanager/`, `utils/`, `types/`, `encryptions/`, `log/`). No new classes in the flat root `l1j.server.server`.
- AD-6 (dependencies): New third-party dependencies must be vendored into `lib/` and added to the `build.xml` classpath in the same change, and noted in the change description.
- AD-7 (state ownership): Every feature/event has exactly one home for its live state: world/player state in `model/` (objects owned by `L1World`); event-scoped state in that event's controller singleton reachable only through its accessors (`WarTimeController` pattern). No parallel copies; other surfaces (GM commands, packets) go through the owner's accessors.
- Consistency conventions: `C_*`/`S_*` packet naming, `*Table` datatables, `*Controller` timed events, `L1*` model classes. New GM commands go through `command/` + `GMCommands`/`pcommands.properties` registration (no parallel command surface). New timed/recurring events follow the `controllers/` pattern with registration in `GameServerThread`; scheduled work via `GeneralThreadPool`; no raw `Timer`/`Thread.sleep` loops in feature code. Runtime-tunable values live in `config/*.properties` read through `Config` (no hardcoded rates, ports, or flags). Logging via slf4j only, one logger per class, no `System.out`.
- Build substrate: existing Ant build (`build.xml` targets clean/compile/jar), Java 9+, Netty 4.1.29.Final, MariaDB 12.0.2 (Docker), nginx + Docker Compose. No starter template — brownfield repo, work compiles under the existing Ant build.
- Known repo inconsistencies documented in the spine stack (candidates for hygiene stories, not invariant violations): `build.xml` classpath lists `slf4j-jdk14-1.7.5.jar` which is not in `lib/`; `c3p0` is listed in the classpath but no jar is in `lib/` (BoneCP is the pool actually used).
- Deferred (explicitly NOT in scope for story creation): build-system migration, Java version pinning, test strategy/test infrastructure, lock retrofit for telnet/ssh GM paths, legacy no-lock audit in `model/`, DB migration tooling, multi-environment topology, scaling/HA, shrinking the frozen flat root.

### UX Design Requirements

N/A — l1j-en is a headless game server with no user interface. In-game UX is owned by the frozen 2009 US client and is out of scope per the SPEC non-goals.

### FR Coverage Map

FR1: Out of scope for this epic list — existing capability; contract continues to bind all work
FR2: Out of scope for this epic list — existing capability; contract continues to bind all work
FR3: Out of scope for this epic list — existing capability; contract continues to bind all work
FR4: Out of scope for this epic list — existing capability; contract continues to bind all work
FR5: Out of scope for this epic list — existing capability; contract continues to bind all work
FR6: Out of scope for this epic list — existing capability; contract continues to bind all work
FR7: Out of scope for this epic list — existing capability; contract continues to bind all work
FR8: Epic 1 — enforced via AD-3 (stock client must render the proc effect with no protocol changes)
FR9: Epic 1 — enchant-tier proc trigger on successful attack
FR10: Epic 1 — physical damage within tier min–max range
FR11: Epic 1 — spell/effect signifies the proc, no magical side effects
FR12: Epic 1 — data-driven tier content per AD-4
FR13: Epic 1 — config on/off toggle
FR14: Epic 1 — no enchant persistence regression

## Epic List

### Epic 1: Enchant-Scaled Weapon Procs
A player who has successfully enchanted a weapon gains a proc that can trigger on attack, dealing bonus physical damage that scales with the weapon's enchant tier — e.g. +1–5 triggers a lightning-bolt effect for 3–7 damage, +6–7 a fire effect for 5–9, and so on — with the spell effect signifying the proc while the damage stays physical. Tier definitions (range, chance, damage, effect) are data-driven content, and the feature is toggleable via config.
**FRs covered:** FR9, FR10, FR11, FR12, FR13, FR14 (FR8 enforced via AD-3)

**Binding constraints on all stories:** AD-1 (attack path runs on the 10-thread worker pool — any new shared state needs a documented lock owner), AD-2 (no new player-state persistence; enchant level already flows through storage paths), AD-3 (no protocol changes — the proc must render using existing effect packets the stock 2009 US client understands), AD-4 (content placement: DB table + versioned `db/update_NNN.sql`), AD-5 (new classes in matching subpackages), AD-7 (single state owner), plus consistency conventions (slf4j only, `*Table` naming, no hardcoded rates, config via `Config`).

**Codebase anchors (verified 2026-07-11):** `L1ItemInstance._enchantLevel` (persisted in `character_items.enchantlvl` via `CharactersItemStorage`); `L1Attack` damage calc already reads `_weaponEnchant` and calls `L1WeaponSkill.getWeaponSkillDamage(_pc, _target, _weaponId)`; `WeaponSkillTable` loads the `weapon_skill` DB table (probability, fix/random damage, `skill_id`, `effect_id`) — the pattern to follow.

**Resolved design decisions (2026-07-11):** all weapons with enchant level ≥ 1 are eligible (including kiringku/magic weapons); enchant procs stack with existing `weapon_skill` procs on the same hit; 25% trigger chance across all tiers; tier table below with ~30% damage growth per + above +7 (enchanting past +7 is very difficult, so each single + is a significant jump); the `enchant_proc` table includes a `damage_type` column so damage kind can change from physical to other types in future content without schema changes (all current seed rows are physical).

**Draft tier table (seed content for `db/update_NNN.sql`, tunable without code):**

| Tier | Enchant | Chance | Damage Type | Physical Damage | (avg) | Signifying Effect |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | +1 – +5 | 25% | physical | 3–7 | 5 | Lightning bolt |
| 2 | +6 – +7 | 25% | physical | 5–9 | 7 | Fire attack |
| 3 | +8 | 25% | physical | 7–12 | 9.5 | Ice shard |
| 4 | +9 | 25% | physical | 9–16 | 12.5 | Holy flame |
| 5 | +10 | 25% | physical | 12–21 | 16.5 | Dark lightning |
| 6 | +11 | 25% | physical | 16–27 | 21.5 | Inferno |
| 7 | +12 and above | 25% | physical | 22–35 | 28.5 | Meteor strike |

Effect names are placeholders — actual client effect IDs will be selected from existing 2009 US effect IDs during implementation (must render on the stock client, AD-3).

## Epic 1: Enchant-Scaled Weapon Procs

A player who has successfully enchanted a weapon gains a proc that can trigger on attack, dealing bonus damage that scales with the weapon's enchant tier, signified by a spell effect. Tier definitions are data-driven DB content; the feature is config-toggleable.

### Story 1.1: Enchant Proc Tier Content & Loader

As a content operator,
I want enchant proc tiers defined in a versioned DB table and loaded at boot,
So that I can tune proc ranges, chances, damage, and effects without any code change.

**Acceptance Criteria:**

**Given** the server is stopped
**When** I apply a new `db/update_NNN.sql` (next free number) that creates an `enchant_proc` table — columns: tier id, min enchant, max enchant, probability, damage type, min damage, max damage, effect id — with seed rows for the tier table above
**Then** the schema and content exist in MariaDB
**And** the content lives in the DB layer, matching the `weapon_skill` precedent (AD-4)
**And** the `damage_type` column allows non-physical damage kinds to be configured in future content without schema changes (all current seed rows are physical)

**Given** the updated database
**When** the server boots
**Then** a new `EnchantProcTable` in `datatables/` (singleton, `*Table` naming convention) loads all rows and exposes a lookup by enchant level

**Given** an enchant level within a tier's min–max range
**When** the lookup is called
**Then** the matching tier is returned; and when the level matches no tier, no tier is returned (no proc)

**Given** a malformed row (e.g. min > max, negative damage, unknown damage type)
**When** the table loads
**Then** the row is skipped with an slf4j warning and the server boots normally

**And** no new classes are placed in the flat root `l1j.server.server` (AD-5), logging is slf4j only, and this story changes **no** runtime behavior (inert content, no attack-path changes)

### Story 1.2: Enchant-Tier Proc Triggers Physical Damage on Attack

As a player with a weapon enchanted +1 or higher,
I want my attacks to have a 25% chance of dealing bonus physical damage based on my enchant tier,
So that investing in enchantment is meaningfully rewarded in combat.

**Acceptance Criteria:**

**Given** the feature config flag (new key in `config/*.properties`, read through `Config`) is enabled, and I attack with any weapon (all weapon types, including kiringku/magic weapons) at enchant level ≥ 1 within a configured tier
**When** the 25% probability roll succeeds
**Then** bonus damage within the tier's min–max range is added to the hit, applied as the tier's configured damage type (all current tiers: physical)

**Given** the config flag is disabled
**When** I attack with an enchanted weapon
**Then** no enchant proc occurs and existing behavior is unchanged

**Given** a weapon at enchant level 0 or lower
**When** I attack
**Then** no enchant proc occurs

**Given** the enchant proc and an existing `weapon_skill` proc both trigger on the same hit
**When** the hit resolves
**Then** both damages apply (they stack)

**Given** a tier configured with physical damage is applied to the target
**When** the hit resolves
**Then** physical mitigation applies and no magical or elemental component is involved

**And** no probability, damage, or toggle values are hardcoded in feature code (config/content convention); the attack path runs on the worker pool with no new shared mutable state lacking a documented lock owner (AD-1); no player-state SQL from feature code (AD-2); and the change compiles under the existing Ant build (NFR5)

### Story 1.3: Spell Effect Signifies the Proc

As a player and nearby observer,
I want the proc signified by the tier's configured spell effect,
So that the trigger is visible and understandable in-game with no new client required.

**Acceptance Criteria:**

**Given** the enchant proc triggers
**When** the damage is applied
**Then** the tier's configured effect is sent to the attacker and nearby players using the existing `S_SkillSound` / `S_UseAttackSkill` packets, following the `L1WeaponSkill` pattern — no new packets or opcodes (AD-3)

**Given** the unmodified 2009 US client
**When** the proc triggers
**Then** the effect renders with no protocol errors, and the stock client never receives a packet it cannot parse (FR8)

**Given** the proc triggers
**When** the effect is applied
**Then** no buff, status, or attribute effect is applied to attacker or target — the effect is purely visual signification of the damage (FR11)

**Given** a character with an enchanted weapon
**When** they log out and back in, and after a full server restart
**Then** the enchant level is unchanged and the proc tier follows the restored enchant level, with no changes to existing save/load paths (FR14)
