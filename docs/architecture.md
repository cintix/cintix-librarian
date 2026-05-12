# Architecture

## Module Boundaries

```
endpoint (Main.java + commands/)
        ↓ depends on
LibrarianCore.java (top-level public API + DTOs)
        ↓ implemented by
SyncManager (sync/services/) — orchestrator
        ↓ depends on contracts
ConfigContract  ResolutionContract  ArtifactContract  LockFileContract
        ↓ implemented by
ConfigService   ResolutionService   ArtifactService   LockFileService
        ↓ internal to each module
domain/rules/ + persistence/
        ↓ use
infrastructure/json/ (shared technical code)
```

## Module Dependency Rules

1. **endpoint** depends only on `LibrarianCore` interface + public DTOs
2. **sync** depends on all module contracts — never on module internals
3. **config, resolution, artifact, lockfile** — each module:
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
- `ConfigReader` — JSON file I/O

### `dk.cintix.librarian.resolution`

Version resolution module:

- `ResolutionContract` — public contract + `VersionSpec` class, `ResolvedDependency` record
- `ResolutionService` — facade implementing contract
- `VersionResolver` — business logic: spec → exact version
- `MavenMetadataClient` — HTTP client for maven-metadata.xml
- `MavenMetadataParser` — XML parser for version listings

### `dk.cintix.librarian.artifact`

Artifact management module:

- `ArtifactContract` — public contract + `DownloadedArtifact` record
- `ArtifactService` — facade implementing contract
- `ArtifactDownloader` — JAR download + SHA-1 checksum
- `LibDirectoryManager` — lib/ directory operations

### `dk.cintix.librarian.lockfile`

Lock file module:

- `LockFileContract` — public contract + `LockEntry`, `LockFileData` records
- `LockFileService` — facade implementing contract
- `LockFile` — internal model
- `LockFileWriter` — read/write librarian.lock.json

### `dk.cintix.librarian.sync`

Orchestration module:

- `SyncManager` — implements `LibrarianCore`, orchestrates all four modules

### `dk.cintix.librarian.infrastructure`

Shared technical code, zero business logic:

- `json/` — Custom JSON parser and pretty-printer (sealed interface + 6 subtypes)

## Data Flow

```
librarian.json
    ↓ ConfigContract.read()
ConfigData (libDir, repos, List<DependencySpec>)
    ↓ ResolutionContract.resolve()
    ↓   MavenMetadataClient.fetchMetadata()
    ↓   MavenMetadataParser.parseVersions()
ResolvedDependency[]
    ↓ ArtifactContract.download()
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
  └── SyncManager(coreContract) ← receives all four contracts
        ↓ injected into
  SyncCommand, ResolveCommand, UpdateCommand, DoctorCommand
```

No dependency injection framework. No reflection. No annotations.

## New Integration Points

Future integrations (Ant task, IDE plugin, etc.) should:

1. Depend only on `LibrarianCore` interface and its public DTOs
2. Instantiate `SyncManager` with the four module implementations
3. Never import from `services/`, `domain/`, or `persistence/` packages
