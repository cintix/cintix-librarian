package dk.cintix.librarian.artifact.services;

import dk.cintix.librarian.artifact.ArtifactContract;
import dk.cintix.librarian.artifact.ArtifactContract.DownloadedArtifact;
import dk.cintix.librarian.artifact.services.domain.rules.ArtifactDownloader;
import dk.cintix.librarian.artifact.services.domain.rules.LibDirectoryManager;
import dk.cintix.librarian.config.ConfigContract.DependencySpec;
import dk.cintix.librarian.config.ConfigContract.RepositoryDef;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public final class ArtifactService implements ArtifactContract {
    private final ArtifactDownloader artifactDownloader;
    private final LibDirectoryManager libDirectoryManager;

    public ArtifactService() {
        this.artifactDownloader = new ArtifactDownloader();
        this.libDirectoryManager = new LibDirectoryManager();
    }

    @Override
    public void ensureDirectory(Path libDir) throws IOException {
        libDirectoryManager.ensureDirectory(libDir);
    }

    @Override
    public DownloadedArtifact download(DependencySpec dep, String version, RepositoryDef repo, Path libDir)
            throws IOException {
        return artifactDownloader.download(dep, version, repo, libDir);
    }

    @Override
    public List<Path> cleanOutdated(Path libDir, Set<String> expectedFiles) throws IOException {
        return libDirectoryManager.cleanOutdated(libDir, expectedFiles);
    }

    @Override
    public Set<String> existingJars(Path libDir) throws IOException {
        return libDirectoryManager.existingJars(libDir);
    }

    @Override
    public List<Path> listJars(Path libDir) throws IOException {
        return libDirectoryManager.listJars(libDir);
    }
}
