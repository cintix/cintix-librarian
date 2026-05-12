#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="$PROJECT_DIR/build"
SRC_DIR="$PROJECT_DIR/src"
CLASSES="$BUILD_DIR/classes"
DIST_DIR="$BUILD_DIR/dist"

echo "==> Cleaning build directory"
rm -rf "$BUILD_DIR"
mkdir -p "$CLASSES" "$DIST_DIR"

echo "==> Compiling"
javac --release 17 \
    -d "$CLASSES" \
    $(find "$SRC_DIR" -name "*.java" | sort)

echo "==> Packaging librarian.jar"
jar --create --file "$DIST_DIR/librarian.jar" -e dk.cintix.librarian.endpoint.Main -C "$CLASSES" .

echo "==> Creating executable librarian script"
cat > "$DIST_DIR/librarian" << 'SCRIPT'
#!/usr/bin/env bash
DIR="$(cd "$(dirname "$0")" && pwd)"
exec java -cp "$DIR/librarian.jar" dk.cintix.librarian.endpoint.Main "$@"
SCRIPT
chmod +x "$DIST_DIR/librarian"

echo "==> Build complete"
echo "    JAR: $DIST_DIR/librarian.jar"
echo "    Run: $DIST_DIR/librarian <command>"
