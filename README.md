# librarian

A deterministic Java library synchronizer.

**One responsibility:** Synchronize a project's explicit library state.

```text
librarian.json → resolve → download → lib/ → librarian.lock.json
```

## Philosophy

- No hidden lifecycle
- No implicit transitive dependencies
- No silent version conflict resolution
- No plugin system
- No embedded scripting
- No build orchestration
- Zero external dependencies — pure JDK 17+

## Quick Start

### Build

```bash
bash build.sh        # Shell build
ant                  # Ant build
```

### Usage

```bash
# In your project directory:
librarian sync       # Resolve, download, clean lib/, write lock
librarian resolve    # Resolve versions only (no download)
librarian update     # Re-resolve all, re-download, update lock
librarian doctor     # Check configuration health
librarian help       # Show help
```

### Project Configuration

Create `librarian.json` in your project root:

```json
{
  "libDir": "lib",
  "defaultRepository": "maven-central",
  "repositories": {
    "default": "maven-central",
    "items": {
      "maven-central": {
        "type": "maven",
        "url": "https://repo1.maven.org/maven2"
      }
    }
  },
  "dependencies": {
    "org.slf4j:slf4j-api": "2.0.16",
    "com.google.code.gson:gson": {
      "version": "^2.11",
      "transitive": false
    }
  }
}
```

After running `librarian sync`, your project looks like:

```text
project/
  src/
  lib/
    slf4j-api-2.0.16.jar
    gson-2.14.0.jar
  librarian.json
  librarian.lock.json
```

## Version Rules

| Syntax | Meaning | Example |
|--------|---------|---------|
| `2.1.5` | Exact version | `"org.slf4j:slf4j-api": "2.0.16"` |
| `2.1.*` | Patch stream | Highest patch in 2.1.x |
| `2.*` | Major stream | Highest minor.patch in 2.x |
| `^2.1` | Compatible range | >= 2.1.0 and < 3.0.0 |
| `latest` | Latest stable | Highest non-prerelease |

## Key Defaults

- **transitive**: `false` — only direct dependencies are fetched
- **cache**: disabled — no hidden global state
- **libDir**: `"lib"` — relative to project root

## Architecture

Single project following modular-hybrid architecture with five modules:

```
src/dk/cintix/librarian/
  LibrarianCore.java         # Top-level public API + DTOs

  config/                    # Configuration parsing
    ConfigContract.java      # Public: DependencySpec, RepositoryDef, ConfigData
    services/ConfigService.java, persistence/ConfigReader.java

  resolution/                # Version resolution
    ResolutionContract.java  # Public: VersionSpec, ResolvedDependency
    services/ResolutionService.java, MavenMetadataClient, MavenMetadataParser

  artifact/                  # JAR download + lib/ management
    ArtifactContract.java    # Public: DownloadedArtifact
    services/ArtifactService.java, ArtifactDownloader, LibDirectoryManager

  lockfile/                  # Lock file persistence
    LockFileContract.java    # Public: LockEntry, LockFileData
    services/LockFileService.java, persistence/LockFileWriter.java

  sync/                      # Orchestration
    services/SyncManager.java  # Implements LibrarianCore, depends on all contracts

  endpoint/                  # CLI
    Main.java                # Composition root — wires all modules
    commands/                # SyncCommand, ResolveCommand, UpdateCommand, DoctorCommand

  infrastructure/            # Shared technical code (zero business logic)
    json/                    # Custom JSON parser/writer
```

- `endpoint` depends only on `LibrarianCore` — never on module internals
- Modules communicate only through contracts — no cross-module imports of internals
- Infrastructure is purely technical, depends on nothing except JDK
- Composition root (`Main.java`) wires all modules manually — no DI framework

## Global Configuration

Optional `~/.librarian/config.json` for user-wide defaults:

```json
{
  "libDir": "external/lib",
  "defaultRepository": "company-repo"
}
```

## Repository Support

Maven-compatible repositories:

- Maven Central
- Nexus
- Artifactory
- GitHub Packages
- Custom/private Maven repositories

## Design Constraints

This tool is intentionally NOT:

- a build system
- a Gradle/Maven replacement
- a lifecycle engine
- a plugin platform
- a task runner
- a scripting environment
- a framework
