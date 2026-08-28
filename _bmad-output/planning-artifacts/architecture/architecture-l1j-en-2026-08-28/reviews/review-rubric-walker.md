# Review — rubric walker (good-spine checklist)

Target: `ARCHITECTURE-SPINE.md` (l1j-en, feature altitude, ratify-as-is)

## Verdict
Pass with fixes. The spine fixes the real divergence points for this brownfield monolith and ratifies (not contradicts) the code — the threading AD was corrected mid-run after the sweep disproved the upstream single-main-thread assumption, which is exactly the behavior the checklist rewards.

## Checklist walk
- **Fixes real divergence points, misses none:** threading (AD-1), persistence authority (AD-2), protocol freeze (AD-3), content layering (AD-4), package placement (AD-5), dependencies (AD-6) — the six places two independent builders would actually fork. One real miss: **no rule for who owns a feature's live state** (model vs controller singleton vs ad-hoc statics) — see adversarial review, fixed as AD-7.
- **Every AD's Rule enforceable and prevents its stated divergence:** yes for AD-1..AD-6. AD-4 has a gap for an *entirely new* content kind (no "closest existing kind" to follow) — tightened with a DB-default tiebreaker.
- **Nothing under Deferred lets two units diverge:** build system / Java version are deferred coherently (single build.xml, one JVM); test policy deferred with a revisit condition; lock retrofits are refactors, not feature gates. OK.
- **Named tech verified-current:** brownfield ratification — verified against the repo, not the web (correct posture). Two repo inconsistencies found by the reality-check reviewer (slf4j-jdk14, c3p0) — recorded in the stack table, not spine errors.
- **Ratifies rather than contradicts brownfield:** yes — per-object locking, concurrent PacketConsumer pool, stop-the-world content, subpackage-first all match observed code.
- **Every dimension decided/deferred/open:** operational envelope covered (Docker Compose seed, single-node, HA deferred). Gap: **environments** (dev/test/prod topology) was silent — added to Deferred.

## Findings
1. [HIGH, fixed] No single-state-owner rule → AD-7 added.
2. [MEDIUM, fixed] AD-4 silent on brand-new content kinds → DB-default tiebreaker added.
3. [MEDIUM, fixed] `update_*.sql` numbering has no ownership rule → convention added.
4. [LOW, fixed] Environments dimension silent → Deferred row added.
