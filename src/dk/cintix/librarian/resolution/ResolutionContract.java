package dk.cintix.librarian.resolution;

import dk.cintix.librarian.config.ConfigContract.DependencySpec;
import dk.cintix.librarian.config.ConfigContract.RepositoryDef;

import java.io.IOException;

public interface ResolutionContract {

    final class VersionSpec {
        public enum Type { EXACT, PATCH_STREAM, MAJOR_STREAM, COMPATIBLE, LATEST }

        private final Type type;
        private final String raw;
        private final int major, minor, patch;

        private VersionSpec(Type type, String raw, int major, int minor, int patch) {
            this.type = type;
            this.raw = raw;
            this.major = major;
            this.minor = minor;
            this.patch = patch;
        }

        public static VersionSpec parse(String raw) {
            if (raw == null || raw.isBlank()) {
                throw new IllegalArgumentException("Version spec must not be empty");
            }
            String trimmed = raw.trim();

            if ("latest".equalsIgnoreCase(trimmed)) {
                return new VersionSpec(Type.LATEST, trimmed, 0, 0, 0);
            }
            if (trimmed.startsWith("^")) {
                int[] parts = parseVersionParts(trimmed.substring(1));
                return new VersionSpec(Type.COMPATIBLE, trimmed, parts[0], parts[1], parts[2]);
            }
            if (trimmed.endsWith(".*")) {
                String prefix = trimmed.substring(0, trimmed.length() - 2);
                if (prefix.contains("*")) throw new IllegalArgumentException("Invalid version spec: " + trimmed);
                int[] parts = parseVersionParts(prefix);
                int dotCount = prefix.length() - prefix.replace(".", "").length();
                if (dotCount == 0) {
                    return new VersionSpec(Type.MAJOR_STREAM, trimmed, parts[0], 0, 0);
                } else {
                    return new VersionSpec(Type.PATCH_STREAM, trimmed, parts[0], parts[1], 0);
                }
            }
            int[] parts = parseVersionParts(trimmed);
            return new VersionSpec(Type.EXACT, trimmed, parts[0], parts[1], parts[2]);
        }

        private static int[] parseVersionParts(String version) {
            String[] parts = version.split("\\.");
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            return new int[]{major, minor, patch};
        }

        public boolean matches(int maj, int min, int pat) {
            return switch (type) {
                case EXACT -> maj == major && min == minor && pat == patch;
                case PATCH_STREAM -> maj == major && min == minor;
                case MAJOR_STREAM -> maj == major;
                case COMPATIBLE -> maj == major && (min > minor || (min == minor && pat >= patch));
                case LATEST -> true;
            };
        }

        public Type type() { return type; }
        public String raw() { return raw; }
        public int major() { return major; }
        public int minor() { return minor; }
        public int patch() { return patch; }

        @Override
        public String toString() { return raw; }
    }

    record ResolvedDependency(String groupId, String artifactId, String coordinate,
                               String requestedVersion, String resolvedVersion,
                               String repository, String checksum) {
        public String fileName() {
            return artifactId + "-" + resolvedVersion + ".jar";
        }
    }

    ResolvedDependency resolve(VersionSpec spec, DependencySpec dep, RepositoryDef repo) throws IOException;
}
