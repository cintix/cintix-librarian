#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="$PROJECT_DIR/build"
SRC_DIR="$PROJECT_DIR/src"
TEST_DIR="$PROJECT_DIR/tests"
CLASSES="$BUILD_DIR/test-classes"

echo "==> Compiling sources and tests"
rm -rf "$CLASSES"
mkdir -p "$CLASSES"

javac --release 17 -d "$CLASSES" $(find "$SRC_DIR" "$TEST_DIR" -name "*.java" | sort)

PASSED=0
FAILED=0
TOTAL=0

run_test() {
    local class="$1"
    local label="$2"
    echo ""
    echo "--- $label ---"
    if java -ea -cp "$CLASSES" "$class"; then
        PASSED=$((PASSED + 1))
    else
        FAILED=$((FAILED + 1))
    fi
    TOTAL=$((TOTAL + 1))
}

echo ""
echo "========================================"
echo "  librarian Test Suite"
echo "========================================"

# Infrastructure tests
run_test dk.cintix.librarian.infrastructure.json.JsonParserTest "JsonParser"
run_test dk.cintix.librarian.infrastructure.json.JsonWriterTest "JsonWriter"
run_test dk.cintix.librarian.infrastructure.maven.MavenMetadataParserTest "MavenMetadataParser"

# Resolution module tests
run_test dk.cintix.librarian.resolution.services.domain.rules.VersionSpecTest "VersionSpec"
run_test dk.cintix.librarian.resolution.services.domain.rules.ResolvedDependencyTest "ResolvedDependency"
run_test dk.cintix.librarian.resolution.services.domain.rules.VersionResolverTest "VersionResolver"

# Config module tests
run_test dk.cintix.librarian.config.services.domain.models.DependencySpecTest "DependencySpec"
run_test dk.cintix.librarian.config.services.domain.models.ConfigurationTest "Configuration"
run_test dk.cintix.librarian.config.services.persistence.ConfigReaderTest "ConfigReader"

# Artifact module tests
run_test dk.cintix.librarian.artifact.services.domain.rules.LibDirectoryManagerTest "LibDirectoryManager"

# Lockfile module tests
run_test dk.cintix.librarian.lockfile.services.domain.models.LockFileTest "LockFile"
run_test dk.cintix.librarian.lockfile.services.persistence.LockFileWriterTest "LockFileWriter"

# Git module tests
run_test dk.cintix.librarian.git.services.domain.rules.GitVersionResolverTest "GitVersionResolver"

# Integration tests
run_test dk.cintix.librarian.integration.EndToEndTest "EndToEnd"

echo ""
echo "========================================"
echo "  Results: $PASSED/$TOTAL suites passed"
echo "========================================"

if [ "$FAILED" -gt 0 ]; then
    echo "  $FAILED suite(s) FAILED"
    exit 1
else
    echo "  All test suites passed."
fi
