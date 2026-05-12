package dk.cintix.librarian.git.services;

import dk.cintix.librarian.git.GitContract;
import dk.cintix.librarian.git.GitContract.GitAsset;
import dk.cintix.librarian.git.GitContract.GitResolved;
import dk.cintix.librarian.git.services.domain.rules.GitAssetDownloader;
import dk.cintix.librarian.git.services.domain.rules.GitVersionResolver;

import java.io.IOException;
import java.nio.file.Path;

public final class GitService implements GitContract {
    private final GitVersionResolver versionResolver;
    private final GitAssetDownloader assetDownloader;

    public GitService() {
        this.versionResolver = new GitVersionResolver();
        this.assetDownloader = new GitAssetDownloader();
    }

    @Override
    public GitResolved resolve(String repoUrl, String versionSpec) throws IOException {
        return versionResolver.resolve(repoUrl, versionSpec);
    }

    @Override
    public GitAsset download(GitResolved resolved, Path libDir) throws IOException {
        return assetDownloader.download(resolved, libDir);
    }
}
