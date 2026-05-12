package dk.cintix.librarian.artifact;

import dk.cintix.librarian.config.ConfigContract.DependencySpec;
import dk.cintix.librarian.config.ConfigContract.RepositoryDef;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public interface ArtifactContract {

    record DownloadedArtifact(Path path, String checksum) {}

    void ensureDirectory(Path libDir) throws IOException;
    DownloadedArtifact download(DependencySpec dep, String version, RepositoryDef repo, Path libDir) throws IOException;
    List<Path> cleanOutdated(Path libDir, Set<String> expectedFiles) throws IOException;
    Set<String> existingJars(Path libDir) throws IOException;
    List<Path> listJars(Path libDir) throws IOException;
}
