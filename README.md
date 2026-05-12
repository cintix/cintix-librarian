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
bash build.sh        # Shell build → build/dist/librarian
ant                  # Ant build → build/ant/dist/librarian.jar
```

### Usage

```bash
librarian sync       # Resolve, download, clean lib/, write lock
librarian resolve    # Resolve versions only (no download)
librarian update     # Re-resolve all, re-download, update lock
librarian doctor     # Check configuration health
librarian help       # Show help
```

---

## Tutorial

### Step 1 — Create librarian.json

Create `librarian.json` in your project root:

```json
{
  "libDir": "lib",
  "dependencies": {
    "org.slf4j:slf4j-api": "2.0.16"
  }
}
```

Minimal config. `libDir` defaults to `"lib"`, `defaultRepository` defaults to `"maven-central"`.

### Step 2 — Sync

```bash
$ librarian sync
sync: /home/me/my-project
  resolved org.slf4j:slf4j-api -> 2.0.16
Lock file written: /home/me/my-project/librarian.lock.json
```

Your project now has:

```text
my-project/
  lib/
    slf4j-api-2.0.16.jar    ← downloaded
  librarian.json
  librarian.lock.json        ← generated lock file
```

### Step 3 — Add dependencies

```json
{
  "libDir": "lib",
  "dependencies": {
    "org.slf4j:slf4j-api": "2.0.16",
    "com.google.code.gson:gson": {
      "version": "^2.11",
      "transitive": false
    },
    "com.fasterxml.jackson.core:jackson-databind": {
      "version": "^2.17",
      "transitive": true,
      "repository": "maven-central"
    }
  }
}
```

Detail form lets you control `transitive`, `repository`, and `type` per dependency.

### Step 4 — Update

```bash
$ librarian update
update: /home/me/my-project
Updated 3 dependencies
```

Re-resolves all version constraints, re-downloads, regenerates lock file.

### Step 5 — Check health

```bash
$ librarian doctor
doctor: /home/me/my-project
Config valid: true
Dependencies: 3
Repositories reachable: [maven-central]
Lock file present: true
  Entries: 3
Doctor check complete.
```

### Using version streams

```json
{
  "dependencies": {
    "org.slf4j:slf4j-api": "2.*",
    "com.google.code.gson:gson": "^2.11",
    "commons-io:commons-io": "latest"
  }
}
```

| Syntax | Meaning | Resolves to |
|--------|---------|-------------|
| `2.0.16` | Exact version | `2.0.16` |
| `2.*` | Major stream | Highest `2.x.x` |
| `2.1.*` | Patch stream | Highest `2.1.x` |
| `^2.11` | Compatible range | Highest `>= 2.11.0, < 3.0.0` |
| `latest` | Latest stable | Highest non-prerelease |

### Using Git dependencies

Fetch pre-built JARs from GitHub/GitLab Releases:

```json
{
  "dependencies": {
    "github.com/my-org/my-library": {
      "type": "git",
      "version": "v2.1.0"
    }
  }
}
```

```bash
$ librarian sync
sync: /home/me/my-project
  resolved github.com/my-org/my-library -> 2.1.0
Lock file written: /home/me/my-project/librarian.lock.json
```

Git version specs:

| Spec | Meaning |
|------|---------|
| `"v2.1.0"` | Exact release tag |
| `"latest"` | Latest release |
| `"^v2.0"` | Highest tag `>= 2.0.0, < 3.0.0` |

Only `.jar` assets attached to releases are downloaded. No cloning, no build tools required.

### Custom repositories

```json
{
  "libDir": "lib",
  "defaultRepository": "company-releases",
  "repositories": {
    "default": "company-releases",
    "items": {
      "company-releases": {
        "type": "maven",
        "url": "https://nexus.company.local/repository/releases"
      },
      "maven-central": {
        "type": "maven",
        "url": "https://repo1.maven.org/maven2"
      }
    }
  },
  "dependencies": {
    "com.company:internal-lib": {
      "version": "^3.0",
      "repository": "company-releases"
    },
    "org.slf4j:slf4j-api": "2.0.16"
  }
}
```

### Lock file

`librarian.lock.json` captures the exact resolved state:

```json
{
  "version": "1",
  "dependencies": {
    "org.slf4j:slf4j-api": {
      "requested": "2.*",
      "resolved": "2.0.16",
      "repository": "maven-central",
      "checksum": "0172931663a09a1fa515567af5fbef00897d3c04"
    },
    "com.google.code.gson:gson": {
      "requested": "^2.11",
      "resolved": "2.14.0",
      "repository": "maven-central",
      "checksum": "efc0e34ede4e3204eaefb84a00e55e8c86634382"
    }
  }
}
```

Commit this file. It makes builds reproducible — `librarian sync` respects existing lock entries.

### Global configuration

Optional `~/.librarian/config.json` for personal defaults:

```json
{
  "libDir": "external/lib",
  "defaultRepository": "company-releases"
}
```

### Resolve-only mode

```bash
$ librarian resolve
resolve: /home/me/my-project
Resolved 3 dependencies
```

Resolves versions and writes the lock file **without** downloading JARs. Useful for CI pipelines that verify resolution succeeds before syncing.

---

## Architecture

Single project following modular-hybrid architecture with six modules:

```
src/dk/cintix/librarian/
  LibrarianCore.java         # Top-level public API + DTOs

  config/                    # Configuration parsing
  resolution/                # Version resolution (Maven)
  artifact/                  # JAR download + lib/ management
  lockfile/                  # Lock file persistence
  git/                       # Git release dependencies
  sync/                      # Orchestration (implements LibrarianCore)
  endpoint/                  # CLI (Main.java + commands)
  infrastructure/json/       # Custom JSON parser/writer (zero business logic)
```

- `endpoint` depends only on `LibrarianCore` — never on module internals
- Modules communicate through contracts — no cross-module imports of internals
- Infrastructure depends on nothing except JDK
- Composition root (`Main.java`) wires all modules manually — no DI framework

## Repository Support

- Maven Central, Nexus, Artifactory, GitHub Packages
- Custom/private Maven repositories
- GitHub Releases, GitLab Releases (pre-built JARs)

## Design Constraints

This tool is intentionally NOT:

- a build system
- a Gradle/Maven replacement
- a lifecycle engine
- a plugin platform
- a task runner
- a scripting environment
- a framework
