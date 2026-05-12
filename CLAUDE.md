# CLAUDE.md — librarian

## Project Identity

**librarian** — A deterministic Java library synchronizer. One responsibility: read `librarian.json`, resolve Maven artifacts, download JARs to `lib/`, generate `librarian.lock.json`.

Package: `dk.cintix.librarian`
Project root: `/home/cintix/projects/cintix-librarian`

## Philosophy

- No hidden lifecycle, no implicit transitive deps, no plugin system, no build orchestration
- Pure Java 17+, **zero external dependencies** — JDK APIs only
- No Spring, no DI frameworks, no reflection magic, no annotation processing, no Maven/Gradle
- Deterministic, mechanical, transparent, portable, editor-agnostic, build-system-agnostic
- Transitive dependencies: disabled by default (`"transitive": true` per-dependency opt-in)
- Cache: disabled by default — no hidden global state
- Modular Hybrid Architecture: feature-module ownership, clean contracts, composition-root wiring

## Build & Test

```bash
bash build.sh          # javac + jar → build/dist/librarian + librarian.jar
bash test.sh           # Compile + run all 13 test suites
ant                    # Ant build → build/ant/dist/librarian.jar
ant test               # Ant test runner
./build/dist/librarian help
```

Build is pure `javac` + `jar` with `--release 17`. No build tools. The `librarian` script sets classpath and runs `dk.cintix.librarian.endpoint.Main`.

## File Map

### Top-level contract

```
src/dk/cintix/librarian/
  LibrarianCore.java                              # Public API — 4 methods + public DTO records
```

### Module: config (`dk.cintix.librarian.config`)

```
src/dk/cintix/librarian/config/
  ConfigContract.java                             # Public contract + DTOs (DependencySpec, RepositoryDef, ConfigData)
  services/
    ConfigService.java                            # Facade implementing ConfigContract
    domain/models/
      Configuration.java                          # Internal config model
    persistence/
      ConfigReader.java                           # Read librarian.json + merge ~/.librarian/config.json
```

### Module: resolution (`dk.cintix.librarian.resolution`)

```
src/dk/cintix/librarian/resolution/
  ResolutionContract.java                         # Public contract + VersionSpec, ResolvedDependency
  services/
    ResolutionService.java                        # Facade implementing ResolutionContract
    domain/rules/
      VersionResolver.java                        # VersionSpec → exact version via maven-metadata.xml
    persistence/
      MavenMetadataClient.java                    # Fetch maven-metadata.xml, checkReachable
      MavenMetadataParser.java                    # Parse maven-metadata.xml → version list + latest/release
```

### Module: artifact (`dk.cintix.librarian.artifact`)

```
src/dk/cintix/librarian/artifact/
  ArtifactContract.java                           # Public contract + DownloadedArtifact DTO
  services/
    ArtifactService.java                          # Facade implementing ArtifactContract
    domain/rules/
      ArtifactDownloader.java                     # Download JAR + SHA-1 checksum
      LibDirectoryManager.java                    # Manage lib/ — ensureDir, cleanOutdated, existingJars, listJars
```

### Module: lockfile (`dk.cintix.librarian.lockfile`)

```
src/dk/cintix/librarian/lockfile/
  LockFileContract.java                           # Public contract + LockEntry, LockFileData DTOs
  services/
    LockFileService.java                          # Facade implementing LockFileContract
    domain/models/
      LockFile.java                               # Internal lock file model
    persistence/
      LockFileWriter.java                         # Generate/write/read librarian.lock.json
```

### Module: sync (`dk.cintix.librarian.sync`)

```
src/dk/cintix/librarian/sync/
  services/
    SyncManager.java                              # Orchestrator — implements LibrarianCore, depends on all module contracts
```

### Endpoint

```
src/dk/cintix/librarian/endpoint/
  Main.java                                       # Composition root — wires all modules, dispatches commands
  commands/
    SyncCommand.java                              # librarian sync
    ResolveCommand.java                           # librarian resolve
    UpdateCommand.java                            # librarian update
    DoctorCommand.java                            # librarian doctor
```

### Infrastructure (shared technical code, zero business logic)

```
src/dk/cintix/librarian/infrastructure/
  json/
    JsonValue.java                                # Sealed interface — 6 permitted subtypes
    JsonObject.java, JsonArray.java               # Container types
    JsonString.java, JsonNumber.java              # Value types (records)
    JsonBoolean.java, JsonNull.java               # Value types
    JsonParser.java                               # Recursive-descent parser
    JsonWriter.java                               # Pretty-printer with indent control
    JsonParseException.java                       # RuntimeException for parse errors
```

### Test sources (14 files)

```
tests/dk/cintix/librarian/
  Assert.java
  infrastructure/json/    JsonParserTest.java, JsonWriterTest.java
  infrastructure/maven/   MavenMetadataParserTest.java
  config/                 DependencySpecTest.java, ConfigurationTest.java, ConfigReaderTest.java
  resolution/             VersionSpecTest.java, ResolvedDependencyTest.java, VersionResolverTest.java
  artifact/               LibDirectoryManagerTest.java
  lockfile/               LockFileTest.java, LockFileWriterTest.java
  integration/            EndToEndTest.java
```

## Architecture Rules

1. **Top-level contract** (`LibrarianCore.java`) defines public DTOs — no internal types leak through the API
2. **Module contracts** (`*Contract.java`) are the only public entry points into each module
3. **Module facades** (`*Service.java`) implement contracts, delegate to domain rules
4. **Domain models** (`models/`) are pure data objects internal to their module
5. **Domain rules** (`rules/`) contain business logic — depend on models + module-private persistence
6. **Persistence** (`persistence/`) handles file I/O and data access — internal to each module
7. **Infrastructure** (`infrastructure/`) is purely technical — JSON. Zero business logic. Zero module dependencies.
8. **Endpoint** depends only on `LibrarianCore` interface — never on module internals
9. **Composition root** (`Main.java`) wires all modules — manual `new`, no DI container, no reflection
10. **SyncManager** depends only on module contracts — never on module internals
11. **No cross-module imports of internals** — modules communicate only through contracts

## Key Design Decisions

### Custom JSON parser (not Gson/Jackson)
- `JsonValue` is a sealed interface with 6 permitted subtypes
- `JsonParser` is a hand-written recursive-descent parser
- `JsonWriter` supports compact and pretty-printed output
- **Why:** zero dependencies. The librarian.json format is simple enough for a custom parser.

### Module boundaries
Each module owns its domain and exposes only a contract:
- **config** — Configuration parsing, dependency specs, repository definitions
- **resolution** — Version constraint parsing and resolution against Maven repositories
- **artifact** — JAR downloading and lib/ directory management
- **lockfile** — Lock file generation, writing, and reading
- **sync** — Orchestration: resolve → download → clean → lock

### Version resolution algorithm
```
1. EXACT → return spec.raw() immediately (no network call)
2. Fetch maven-metadata.xml → parse all <version> entries
3. Filter out pre-releases (SNAPSHOT, alpha, beta, rc, preview)
4. Filter by spec.matches() — VersionSpec.Type determines logic
5. Sort by semantic version (major, minor, patch), return highest
```

### Sync flow (SyncManager.sync)
```
librarian.json → ConfigContract.read() → ConfigData
    → for each dep: ResolutionContract.resolve() → ResolvedDependency
    → for each dep: ArtifactContract.download() → JAR + SHA-1
    → ArtifactContract.cleanOutdated() → remove stale JARs
    → LockFileContract.generate() + write() → librarian.lock.json
```

## CLI Design

Manual arg dispatch in `Main.main()`. No picocli, no external CLI library. Switch on `args[0]`:
- `sync` → SyncCommand, `resolve` → ResolveCommand, `update` → UpdateCommand, `doctor` → DoctorCommand
- `help`, `--help`, `-h` → print usage

Composition root wires all modules once, injects `LibrarianCore` into each command via constructor.

## Test Conventions

- AAA pattern: `// Arrange`, `// Act`, `// Assert` comments in every test
- Custom `Assert.java` utility
- Each test class has a `main()` method — self-executing
- `test.sh` compiles main + test sources, runs each test class with `java -ea`
- Test data uses temp directories created with `Files.createTempDirectory()`
- EndToEndTest checks network availability first, skips if offline

## Known Behaviors & Gotchas

1. **Pre-release filtering**: Versions containing `SNAPSHOT`, `alpha`, `beta`, `rc`, or `preview` (case-sensitive contains) are skipped.
2. **EXACT version skips network**: VersionResolver.resolve() returns early for EXACT type without fetching metadata.
3. **Checksum is SHA-1**: Computed locally after download, not from repository `.sha1` files.
4. **No resume/retry**: Failed downloads are caught per-dependency and collected as errors, not retried.
5. **Lib clean is by filename match**: JARs in lib/ whose filename is NOT in the resolved set are deleted. Non-JAR files are ignored.
6. **Global config is silent-fail**: If `~/.librarian/config.json` is missing or malformed, silently treated as empty defaults.
7. **Lock file version field**: Always `"1"` — format version, not dependency versions.
