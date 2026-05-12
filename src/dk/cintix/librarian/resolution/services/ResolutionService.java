package dk.cintix.librarian.resolution.services;

import dk.cintix.librarian.config.ConfigContract.DependencySpec;
import dk.cintix.librarian.config.ConfigContract.RepositoryDef;
import dk.cintix.librarian.resolution.ResolutionContract;
import dk.cintix.librarian.resolution.ResolutionContract.ResolvedDependency;
import dk.cintix.librarian.resolution.ResolutionContract.VersionSpec;
import dk.cintix.librarian.resolution.services.domain.rules.VersionResolver;
import dk.cintix.librarian.resolution.services.persistence.MavenMetadataClient;
import dk.cintix.librarian.resolution.services.persistence.MavenMetadataParser;

import java.io.IOException;

public final class ResolutionService implements ResolutionContract {
    private final VersionResolver versionResolver;

    public ResolutionService() {
        MavenMetadataClient metadataClient = new MavenMetadataClient();
        MavenMetadataParser metadataParser = new MavenMetadataParser();
        this.versionResolver = new VersionResolver(metadataClient, metadataParser);
    }

    @Override
    public ResolvedDependency resolve(VersionSpec spec, DependencySpec dep, RepositoryDef repo) throws IOException {
        return versionResolver.resolve(spec, dep, repo);
    }
}
