package dk.cintix.librarian.git.services.domain.rules;

import dk.cintix.librarian.git.GitContract.GitResolved;
import dk.cintix.librarian.infrastructure.json.JsonArray;
import dk.cintix.librarian.infrastructure.json.JsonObject;
import dk.cintix.librarian.infrastructure.json.JsonParser;
import dk.cintix.librarian.infrastructure.json.JsonValue;
import dk.cintix.librarian.resolution.ResolutionContract.VersionSpec;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class GitVersionResolver {
    private final HttpClient httpClient;

    public GitVersionResolver() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public GitResolved resolve(String repoUrl, String versionSpec) throws IOException {
        VersionSpec spec = VersionSpec.parse(versionSpec);

        if (spec.type() == VersionSpec.Type.EXACT) {
            return resolveExactTag(repoUrl, spec.raw());
        }

        List<TagInfo> tags = listReleases(repoUrl);
        if (tags.isEmpty()) {
            throw new IOException("No releases found for: " + repoUrl);
        }

        List<TagInfo> matching = new ArrayList<>();
        for (TagInfo tag : tags) {
            if (tag.prerelease) continue;
            if (spec.matches(tag.major, tag.minor, tag.patch)) {
                matching.add(tag);
            }
        }

        if (matching.isEmpty()) {
            throw new IOException("No matching release found for " + repoUrl + " with spec " + versionSpec);
        }

        matching.sort(Comparator.comparingInt((TagInfo t) -> t.major)
                .thenComparingInt(t -> t.minor)
                .thenComparingInt(t -> t.patch));
        TagInfo best = matching.get(matching.size() - 1);

        return buildResolved(repoUrl, best);
    }

    private GitResolved resolveExactTag(String repoUrl, String tag) throws IOException {
        String apiUrl = buildReleaseByTagUrl(repoUrl, tag);
        JsonObject release = fetchJson(apiUrl);
        String assetUrl = findJarAsset(release);
        String repoName = extractRepoName(repoUrl);
        int[] parts = parseVersion(tag);
        String version = parts[0] + "." + parts[1] + "." + parts[2];
        return new GitResolved(repoUrl, tag, assetUrl, repoName, version);
    }

    private List<TagInfo> listReleases(String repoUrl) throws IOException {
        List<TagInfo> tags = new ArrayList<>();
        String apiUrl = buildReleasesUrl(repoUrl);
        JsonValue result = fetchJsonRaw(apiUrl);
        if (result instanceof JsonArray arr) {
            for (JsonValue v : arr.values()) {
                if (v instanceof JsonObject obj) {
                    String tag = obj.getString("tag_name");
                    if (tag != null) {
                        int[] parts = parseVersion(tag);
                        boolean prerelease = obj.getBoolean("prerelease") != null
                                && obj.getBoolean("prerelease");
                        tags.add(new TagInfo(tag, parts[0], parts[1], parts[2], prerelease));
                    }
                }
            }
        }
        return tags;
    }

    static int[] parseVersion(String tag) {
        String v = tag.startsWith("v") ? tag.substring(1) : tag;
        String[] parts = v.split("\\.");
        try {
            int major = parts.length > 0 ? Integer.parseInt(parts[0]) : 0;
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            return new int[]{major, minor, patch};
        } catch (NumberFormatException e) {
            return new int[]{0, 0, 0};
        }
    }

    private JsonObject fetchJson(String url) throws IOException {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("Accept", "application/json").build();
            HttpResponse<InputStream> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                throw new IOException("Git API returned HTTP " + response.statusCode() + " for: " + url);
            }
            return JsonParser.parse(new String(response.body().readAllBytes())).asObject();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Git API request interrupted: " + url, e);
        }
    }

    private JsonValue fetchJsonRaw(String url) throws IOException {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("Accept", "application/json").build();
            HttpResponse<InputStream> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                throw new IOException("Git API returned HTTP " + response.statusCode() + " for: " + url);
            }
            return JsonParser.parse(new String(response.body().readAllBytes()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Git API request interrupted: " + url, e);
        }
    }

    private String findJarAsset(JsonObject release) throws IOException {
        JsonArray assets = null;
        JsonValue assetsVal = release.values().get("assets");
        if (assetsVal instanceof JsonArray arr) assets = arr;
        if (assets == null || assets.values().isEmpty()) {
            throw new IOException("No assets found in release");
        }
        for (JsonValue v : assets.values()) {
            if (v instanceof JsonObject asset) {
                String name = asset.getString("name");
                if (name != null && name.endsWith(".jar")) {
                    String downloadUrl = asset.getString("browser_download_url");
                    if (downloadUrl != null) return downloadUrl;
                }
            }
        }
        throw new IOException("No .jar asset found in release");
    }

    private GitResolved buildResolved(String repoUrl, TagInfo tag) throws IOException {
        String apiUrl = buildReleaseByTagUrl(repoUrl, tag.tag);
        JsonObject release = fetchJson(apiUrl);
        String assetUrl = findJarAsset(release);
        String repoName = extractRepoName(repoUrl);
        String version = tag.major + "." + tag.minor + "." + tag.patch;
        return new GitResolved(repoUrl, tag.tag, assetUrl, repoName, version);
    }

    private String buildReleasesUrl(String repoUrl) {
        String path = extractRepoPath(repoUrl);
        return "https://api.github.com/repos/" + path + "/releases?per_page=100";
    }

    private String buildReleaseByTagUrl(String repoUrl, String tag) {
        String path = extractRepoPath(repoUrl);
        return "https://api.github.com/repos/" + path + "/releases/tags/" + tag;
    }

    private String extractRepoPath(String repoUrl) {
        String path = repoUrl;
        if (path.startsWith("https://")) path = path.substring(8);
        if (path.startsWith("http://")) path = path.substring(7);
        if (path.startsWith("github.com/")) path = path.substring(11);
        if (path.startsWith("gitlab.com/")) path = path.substring(11);
        if (path.endsWith(".git")) path = path.substring(0, path.length() - 4);
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
        return path;
    }

    private String extractRepoName(String repoUrl) {
        String path = extractRepoPath(repoUrl);
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    record TagInfo(String tag, int major, int minor, int patch, boolean prerelease) {}
}
