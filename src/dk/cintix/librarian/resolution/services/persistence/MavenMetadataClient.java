package dk.cintix.librarian.resolution.services.persistence;

import dk.cintix.librarian.config.ConfigContract.DependencySpec;
import dk.cintix.librarian.config.ConfigContract.RepositoryDef;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class MavenMetadataClient {
    private final HttpClient httpClient;

    public MavenMetadataClient() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public InputStream fetchMetadata(RepositoryDef repo, DependencySpec dep) throws IOException {
        String url = buildMetadataUrl(repo, dep);
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).build();
            HttpResponse<InputStream> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                throw new IOException("Failed to fetch metadata: HTTP " + response.statusCode() + " from " + url);
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Fetch interrupted: " + url, e);
        }
    }

    public boolean checkReachable(RepositoryDef repo) {
        try {
            String url = repo.url() + "/";
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() < 500;
        } catch (Exception e) {
            return false;
        }
    }

    private String buildMetadataUrl(RepositoryDef repo, DependencySpec dep) {
        String base = repo.url();
        if (!base.endsWith("/")) base += "/";
        String groupPath = dep.groupId().replace('.', '/');
        return base + groupPath + "/" + dep.artifactId() + "/maven-metadata.xml";
    }
}
