package dk.cintix.librarian.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface ConfigContract {

    record DependencySpec(String groupId, String artifactId, String coordinate,
                           String version, boolean transitive, String repository,
                           String type) {
        public DependencySpec {
            if (type == null) type = "maven";
        }

        public DependencySpec(String groupId, String artifactId, String coordinate,
                               String version, boolean transitive, String repository) {
            this(groupId, artifactId, coordinate, version, transitive, repository, "maven");
        }

        public boolean isGit() { return "git".equals(type); }
    }

    record RepositoryDef(String type, String url) {}

    record ConfigData(String libDir, String defaultRepository,
                       Map<String, RepositoryDef> repositories,
                       List<DependencySpec> dependencies) {}

    ConfigData read(Path projectDir) throws IOException;
}
