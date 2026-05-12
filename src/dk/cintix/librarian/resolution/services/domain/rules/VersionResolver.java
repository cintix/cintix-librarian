package dk.cintix.librarian.resolution.services.domain.rules;

import dk.cintix.librarian.config.ConfigContract.DependencySpec;
import dk.cintix.librarian.config.ConfigContract.RepositoryDef;
import dk.cintix.librarian.resolution.ResolutionContract.ResolvedDependency;
import dk.cintix.librarian.resolution.ResolutionContract.VersionSpec;
import dk.cintix.librarian.resolution.services.persistence.MavenMetadataClient;
import dk.cintix.librarian.resolution.services.persistence.MavenMetadataParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class VersionResolver {
    private final MavenMetadataClient metadataClient;
    private final MavenMetadataParser metadataParser;

    public VersionResolver(MavenMetadataClient metadataClient, MavenMetadataParser metadataParser) {
        this.metadataClient = metadataClient;
        this.metadataParser = metadataParser;
    }

    public ResolvedDependency resolve(VersionSpec spec, DependencySpec dep, RepositoryDef repo) throws IOException {
        String exactVersion;
        if (spec.type() == VersionSpec.Type.EXACT) {
            exactVersion = spec.raw();
        } else {
            exactVersion = resolveVersion(spec, dep, repo);
        }
        return new ResolvedDependency(
                dep.groupId(), dep.artifactId(), dep.coordinate(),
                dep.version(), exactVersion, null, null);
    }

    public String resolveVersion(VersionSpec spec, DependencySpec dep, RepositoryDef repo) throws IOException {
        List<String> availableVersions;
        try (InputStream xml = metadataClient.fetchMetadata(repo, dep)) {
            availableVersions = metadataParser.parseVersions(xml);
        } catch (Exception e) {
            throw new IOException("Failed to resolve version for " + dep.coordinate() + ": " + e.getMessage(), e);
        }

        List<VersionParts> matching = new ArrayList<>();
        for (String ver : availableVersions) {
            if (isPrerelease(ver)) continue;
            try {
                VersionParts parts = VersionParts.parse(ver);
                if (spec.matches(parts.major, parts.minor, parts.patch)) {
                    matching.add(parts);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (matching.isEmpty()) {
            throw new IOException("No matching version found for " + dep.coordinate()
                    + " with spec " + spec.raw());
        }

        matching.sort(Comparator.reverseOrder());
        return matching.get(0).toString();
    }

    private static boolean isPrerelease(String ver) {
        return ver.endsWith("-SNAPSHOT")
                || ver.contains("alpha")
                || ver.contains("beta")
                || ver.contains("rc")
                || ver.contains("preview");
    }

    private static final class VersionParts implements Comparable<VersionParts> {
        final int major, minor, patch;
        final String original;

        VersionParts(int major, int minor, int patch, String original) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
            this.original = original;
        }

        static VersionParts parse(String ver) {
            String[] parts = ver.split("\\.");
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            return new VersionParts(major, minor, patch, ver);
        }

        @Override
        public int compareTo(VersionParts o) {
            int cmp = Integer.compare(major, o.major);
            if (cmp != 0) return cmp;
            cmp = Integer.compare(minor, o.minor);
            if (cmp != 0) return cmp;
            return Integer.compare(patch, o.patch);
        }

        @Override
        public String toString() { return original; }
    }
}
