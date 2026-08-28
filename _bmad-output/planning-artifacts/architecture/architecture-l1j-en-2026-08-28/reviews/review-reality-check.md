# Review — reality check (named tech vs repo/web)

Target: `ARCHITECTURE-SPINE.md` (l1j-en)

Posture: brownfield ratify-as-is — the stack is pinned by the repo, so the check is "does the spine's stack table match what is actually vendored and wired", not "is this the current version on the web" (chasing current versions would violate the ratify-as-is scope; modernization is Deferred).

## Verdict
Stack table matches the repo except two pre-existing repo inconsistencies (build.xml vs `lib/`), now annotated in the spine. No invented or outdated bindings.

## Checks
- Java 9+ (README claim; `build.xml` sets no source/target — compiles at whatever JDK runs Ant): matches spine.
- Netty 4.1.29.Final: `lib/netty-all-4.1.29.Final.jar` + build.xml classpath. Matches.
- mysql-connector-java 5.1.31: jar + classpath. Matches.
- BoneCP 0.8.0.RELEASE: jar + classpath; used by `L1DatabaseFactory`. Matches.
- **c3p0 0.9.1.2: referenced in build.xml classpath but NO jar in `lib/`.** Ant fileset silently skips it. Pre-existing repo inconsistency — annotated in spine stack table; not a spine error.
- javolution 5.2.6, Guava 17.0, sshd-core 1.2.0, JAXB 2.3.1, javax.activation 1.2.0: jars + classpath. Match.
- **slf4j: `lib/` has `slf4j-api-1.7.5.jar` + `slf4j-jdk14-1.7.25.jar`, but build.xml classpath lists `slf4j-jdk14-1.7.5.jar` (not present in `lib/`).** Version mismatch between build file and vendored jar — annotated in spine; worth a separate fix in the repo (logging binding may not be on the compiled classpath).
- MariaDB 12.0.2: `docker-compose.yaml`. Matches.
- nginx / Docker Compose envelope: files present. Matches.

## Findings
1. [MEDIUM, annotated] c3p0 in build.xml without a jar in `lib/`.
2. [MEDIUM, annotated] slf4j-jdk14 version mismatch (build.xml 1.7.5 vs lib 1.7.25) — possible missing logging binding at runtime; flag to user as a repo bug candidate.
3. [LOW] No web research performed — correct for ratify-as-is brownfield; no starter relied upon.
