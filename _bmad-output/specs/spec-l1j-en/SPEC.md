---
id: SPEC-l1j-en
companions:
  - ../planning-artifacts/architecture/architecture-l1j-en-2026-08-28/ARCHITECTURE-SPINE.md
sources: []
---

> **Canonical contract.** This SPEC and the files in `companions:` are the complete, preservation-validated contract for what to build, test, and validate. Source documents listed in frontmatter are for traceability — consult them only if you need narrative rationale or prose color this contract intentionally omits.

# l1j-en — Lineage 1 Server Emulator

## Why

An opportunity to capture: l1j-en is a mature, feature-complete brownfield emulator for the final 2009 US Lineage 1 client, and its load-bearing invariants (concurrency model, persistence authority, frozen protocol, content layering, package discipline) live only in the code and in the heads of long-gone contributors. New features, AI-assisted development, and modernization refactors need a machine-readable contract so independent work — human or agent — cannot drift from or silently break those invariants. Affected: every developer and coding agent contributing to the server.

## Capabilities

- **CAP-1** Character & progression
  - **intent:** The server runs the full 2009 US character lifecycle — creation, leveling, skills, equipment, inventory, deletion.
  - **success:** A character created on a fresh DB can level, equip, and use skills, and retains all state across a server restart.
- **CAP-2** World simulation & combat
  - **intent:** The server simulates maps, monster spawns, movement, skills, and drops per the configured tables.
  - **success:** A player kills a spawned mob, receives the configured drops, and respawn honors `spawnlist` timing.
- **CAP-3** Economy & item flow
  - **intent:** Adena, shops, auction, trade, mail/letters, and getback work per 2009 US rules.
  - **success:** An item moved between players via trade, auction, or mail persists correctly on both sides after a restart.
- **CAP-4** Social & political structures
  - **intent:** Parties, clans/pledges, castles including siege/war, and houses function.
  - **success:** A clan captures and holds a castle; war times and ownership persist across a restart.
- **CAP-5** Timed & recurring events
  - **intent:** Boss cycles, UB, timed dungeons, light/dark, and the other scheduled events run on their configured schedules.
  - **success:** Each configured event fires on schedule with player-visible state transitions.
- **CAP-6** GM & operations
  - **intent:** GMs operate the live server through in-game commands plus telnet/ssh consoles, with logging and bans.
  - **success:** A GM executes each registered command against the live world and ban actions take effect.
- **CAP-7** Content pipeline
  - **intent:** Static content (DB, `data/xml`, `maps/`, `config/`) loads at boot, and content changes ship as versioned files applied stop-the-world.
  - **success:** A content change shipped per spine AD-4 is live after a stop-the-world restart with zero code changes.
- **CAP-8** Client protocol compatibility
  - **intent:** An unmodified 2009 US client connects, plays, and receives all server features.
  - **success:** The stock client logs in, moves, fights, trades, and uses GM features with no protocol errors.

## Constraints

- The architecture spine (companion) binds all implementation work: AD-1 (concurrent worker-pool threading, per-object locking), AD-2 (memory is runtime authority; stop-the-world content), AD-3 (frozen protocol baseline, gated extensions), AD-4 (content placement), AD-5 (subpackage-first placement), AD-6 (vendored dependencies), AD-7 (single state owner per feature/event), plus its consistency conventions.
- Existing packet layouts and semantics are immutable and the unmodified 2009 US client must keep working; new packets/opcodes for modified clients are allowed only when gated on client version/feature detection so stock clients never receive packets they cannot parse, with new opcodes allocated in `encryptions/Opcodes.java`.
- Until a test strategy is adopted, review against the spine is the sole verification gate for new work.
- The operational envelope is single-node Docker Compose (server + MariaDB + nginx); no high-availability or multi-node topology.
- Ratify-as-is scope: this contract fixes the invariants the code already obeys. Build-system or Java-version modernization is not part of this spec — it is a deliberate amendment to the spine.

## Non-goals

- Client development is out of scope for this contract; the server supports stock and modified clients per AD-3, but building or shipping clients is not this spec's work.
- No build-system migration (Ant → other) and no Java version upgrade.
- No multi-node scaling, sharding, or HA.
- No introduction of test infrastructure; verification policy is deferred.
- No migration of existing content between the DB and XML layers.

## Success signal

A new feature or content change built against this contract compiles under the existing Ant build, runs under the unmodified 2009 US client, persists correctly across a restart, and passes a review against the spine with zero AD violations.

## Assumptions

- The eight capability domains (CAP-1..CAP-8) partition the server's feature surface; derived from the codebase sweep, granularity adjustable.
