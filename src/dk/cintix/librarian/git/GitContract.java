package dk.cintix.librarian.git;

import java.io.IOException;
import java.nio.file.Path;

public interface GitContract {

    record GitResolved(String repoUrl, String tag, String assetUrl,
                        String artifactId, String version) {
        public String fileName() {
            return artifactId + "-" + version + ".jar";
        }
    }

    record GitAsset(String fileName, String checksum) {}

    GitResolved resolve(String repoUrl, String versionSpec) throws IOException;

    GitAsset download(GitResolved resolved, Path libDir) throws IOException;
}
