package dk.cintix.librarian.artifact.services.domain.rules;

import dk.cintix.librarian.artifact.ArtifactContract.DownloadedArtifact;
import dk.cintix.librarian.config.ConfigContract.DependencySpec;
import dk.cintix.librarian.config.ConfigContract.RepositoryDef;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class ArtifactDownloader {
    private final HttpClient httpClient;

    public ArtifactDownloader() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public DownloadedArtifact download(DependencySpec dep, String version, RepositoryDef repo, Path libDir)
            throws IOException {
        String url = buildArtifactUrl(repo, dep, version);
        String fileName = dep.artifactId() + "-" + version + ".jar";
        Path target = libDir.resolve(fileName);

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).build();
            HttpResponse<InputStream> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                throw new IOException("Failed to download artifact: HTTP " + response.statusCode() + " from " + url);
            }
            Files.copy(response.body(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Download interrupted: " + url, e);
        }

        String checksum = sha1(target);
        return new DownloadedArtifact(target, checksum);
    }

    private String buildArtifactUrl(RepositoryDef repo, DependencySpec dep, String version) {
        String base = repo.url();
        if (!base.endsWith("/")) base += "/";
        String groupPath = dep.groupId().replace('.', '/');
        return base + groupPath + "/" + dep.artifactId() + "/" + version + "/"
                + dep.artifactId() + "-" + version + ".jar";
    }

    private String sha1(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = Files.readAllBytes(file);
            byte[] hash = digest.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-1 not available", e);
        }
    }
}
