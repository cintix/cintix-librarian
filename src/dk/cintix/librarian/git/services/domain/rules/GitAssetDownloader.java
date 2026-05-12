package dk.cintix.librarian.git.services.domain.rules;

import dk.cintix.librarian.git.GitContract.GitAsset;
import dk.cintix.librarian.git.GitContract.GitResolved;

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

public final class GitAssetDownloader {
    private final HttpClient httpClient;

    public GitAssetDownloader() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public GitAsset download(GitResolved resolved, Path libDir) throws IOException {
        Path target = libDir.resolve(resolved.fileName());

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(resolved.assetUrl())).build();
            HttpResponse<InputStream> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                throw new IOException("Failed to download asset: HTTP " + response.statusCode()
                        + " from " + resolved.assetUrl());
            }
            Files.copy(response.body(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Download interrupted: " + resolved.assetUrl(), e);
        }

        String checksum = sha1(target);
        return new GitAsset(resolved.fileName(), checksum);
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
