package dk.cintix.librarian.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface ConfigContract {

    record DependencySpec(String groupId, String artifactId, String coordinate,
                           String version, boolean transitive, String repository) {}

    record RepositoryDef(String type, String url) {}

    record ConfigData(String libDir, String defaultRepository,
                       Map<String, RepositoryDef> repositories,
                       List<DependencySpec> dependencies) {}

    ConfigData read(Path projectDir) throws IOException;
}
