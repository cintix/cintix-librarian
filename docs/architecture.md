# Architecture

## Module Boundaries

```
endpoint (Main.java + commands/)
        ↓ depends on
LibrarianCore.java (top-level public API + DTOs)
        ↓ implemented by
SyncManager (sync/services/) — orchestrator
        ↓ depends on contracts (dispatches by dependency type)
ConfigContract  ResolutionContract  ArtifactContract  LockFileContract  GitContract
        ↓ implemented by
ConfigService   ResolutionService   ArtifactService   LockFileService   GitService
        ↓ internal to each module
domain/rules/ + persistence/
        ↓ use
infrastructure/json/ (shared technical code)
```

## Module Dependency Rules

1. **endpoint** depends only on `LibrarianCore` interface + public DTOs
2. **sync** depends on all module contracts — never on module internals
3. **config, resolution, artifact, lockfile, git** — each module:
   - Exposes a public contract with DTOs at module root
   - Implements contract in `services/` package
   - Keeps domain models and persistence internal
4. **infrastructure** depends on nothing except JDK — zero module dependencies
5. **No cross-module imports of internals** — all cross-module communication through contracts

## Package-Level Design

### `dk.cintix.librarian`

Top-level public API:

```java
public interface LibrarianCore {
    record ResolvedDepInfo(...) {}
    record LockEntryInfo(...) {}
    record LockFileInfo(...) {}
    record SyncResult(...) {}
    record DoctorResult(...) {}

    SyncResult sync(Path projectDir);
    LockFileInfo resolve(Path projectDir);
    LockFileInfo update(Path projectDir);
    DoctorResult doctor(Path projectDir);
}
```

No internal types leak through this interface.

### `dk.cintix.librarian.config`

Configuration parsing module:

- `ConfigContract` — public contract + `DependencySpec`, `RepositoryDef`, `ConfigData` records
- `ConfigService` — facade implementing contract
- `Configuration` — internal model
- `ConfigReader` — JSON file I/O, parses `librarian.json` + `~/.librarian/config.json`

`DependencySpec` includes `type` field (default `"maven"`, or `"git"`) to route dependencies to the correct resolver.

### `dk.cintix.librarian.resolution`

Maven version resolution module:

- `ResolutionContract` — public contract + `VersionSpec` class, `ResolvedDependency` record
- `ResolutionService` — facade implementing contract
- `VersionResolver` — business logic: spec → exact version via maven-metadata.xml
- `MavenMetadataClient` — HTTP client for maven-metadata.xml
- `MavenMetadataParser` — XML parser for version listings

### `dk.cintix.librarian.artifact`

JAR download and lib/ management module:

- `ArtifactContract` — public contract + `DownloadedArtifact` record
- `ArtifactService` — facade implementing contract
- `ArtifactDownloader` — JAR download + SHA-1 checksum
- `LibDirectoryManager` — lib/ directory operations (ensureDir, cleanOutdated, existingJars, listJars)

### `dk.cintix.librarian.lockfile`

Lock file module:

- `LockFileContract` — public contract + `LockEntry`, `LockFileData` records
- `LockFileService` — facade implementing contract
- `LockFile` — internal model
- `LockFileWriter` — read/write `librarian.lock.json`

### `dk.cintix.librarian.git`

Git release dependency module:

- `GitContract` — public contract + `GitResolved`, `GitAsset` records
- `GitService` — facade implementing contract
- `GitVersionResolver` — talks to GitHub/GitLab Releases API, resolves version specs to tags, finds `.jar` assets
- `GitAssetDownloader` — HTTP download of release asset + SHA-1 checksum

Only pre-built `.jar` assets from releases are supported. No cloning or building.

### `dk.cintix.librarian.sync`

Orchestration module:

- `SyncManager` — implements `LibrarianCore`, orchestrates all five modules
  - For Maven deps (`type: "maven"`): `ConfigContract` → `ResolutionContract` → `ArtifactContract`
  - For Git deps (`type: "git"`): `ConfigContract` → `GitContract` (handles both resolve + download)

### `dk.cintix.librarian.infrastructure`

Shared technical code, zero business logic:

- `json/` — Custom JSON parser and pretty-printer (sealed interface + 6 subtypes)

## Data Flow

```
librarian.json
    ↓ ConfigContract.read()
ConfigData (libDir, repos, List<DependencySpec>)
    ↓ SyncManager dispatches by dep.type()
    ├── type="maven"
    │   ↓ ResolutionContract.resolve()
    │   ↓   MavenMetadataClient.fetchMetadata()
    │   ↓   MavenMetadataParser.parseVersions()
    │   ↓ ArtifactContract.download()
    │   ↓   HTTP GET → lib/*.jar
    └── type="git"
        ↓ GitContract.resolve()
        ↓   GitHub Releases API → find tag + .jar asset
        ↓ GitContract.download()
        ↓   HTTP GET → lib/*.jar
    ↓ ArtifactContract.cleanOutdated()
    ↓ LockFileContract.generate() + write()
librarian.lock.json
```

## Composition Root

`Main.java` wires all modules:

```
Main
  ├── ConfigService(configContract)
  ├── ResolutionService(resolutionContract)
  ├── ArtifactService(artifactContract)
  ├── LockFileService(lockFileContract)
  ├── GitService(gitContract)
  └── SyncManager(coreContract) ← receives all five contracts
        ↓ injected into
  SyncCommand, ResolveCommand, UpdateCommand, DoctorCommand
```

No dependency injection framework. No reflection. No annotations.

## New Integration Points

Future integrations (Ant task, IDE plugin, etc.) should:

1. Depend only on `LibrarianCore` interface and its public DTOs
2. Instantiate `SyncManager` with the five module implementations
3. Never import from `services/`, `domain/`, or `persistence/` packages
