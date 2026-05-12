package dk.cintix.librarian.config.services.persistence;

import dk.cintix.librarian.config.ConfigContract.DependencySpec;
import dk.cintix.librarian.config.ConfigContract.RepositoryDef;
import dk.cintix.librarian.config.services.domain.models.Configuration;
import dk.cintix.librarian.infrastructure.json.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ConfigReader {
    private static final String CONFIG_FILE = "librarian.json";
    private static final String GLOBAL_CONFIG_DIR = ".librarian";
    private static final String GLOBAL_CONFIG_FILE = "config.json";

    public Configuration read(Path projectDir) throws IOException {
        Path configPath = projectDir.resolve(CONFIG_FILE);
        if (!Files.exists(configPath)) {
            throw new IOException("Configuration file not found: " + configPath);
        }
        String json = Files.readString(configPath);
        JsonObject root = JsonParser.parse(json).asObject();
        Configuration config = parseConfiguration(root);
        Configuration globalConfig = readGlobalConfig();
        return merge(config, globalConfig);
    }

    private Configuration readGlobalConfig() {
        Path globalConfigPath = Path.of(System.getProperty("user.home"), GLOBAL_CONFIG_DIR, GLOBAL_CONFIG_FILE);
        if (!Files.exists(globalConfigPath)) {
            return new Configuration();
        }
        try {
            String json = Files.readString(globalConfigPath);
            JsonObject root = JsonParser.parse(json).asObject();
            return parseConfiguration(root);
        } catch (Exception e) {
            return new Configuration();
        }
    }

    private Configuration parseConfiguration(JsonObject root) {
        Configuration config = new Configuration();
        String libDir = root.getString("libDir");
        if (libDir != null) config.setLibDir(libDir);
        String defaultRepo = root.getString("defaultRepository");
        if (defaultRepo != null) config.setDefaultRepository(defaultRepo);
        JsonObject reposWrapper = root.getObject("repositories");
        if (reposWrapper != null) {
            String defaultRepo2 = reposWrapper.getString("default");
            Configuration.RepositoriesWrapper wrapper = new Configuration.RepositoriesWrapper();
            wrapper.defaultRepo = defaultRepo2;
            JsonObject items = reposWrapper.getObject("items");
            if (items != null) {
                Map<String, RepositoryDef> repos = new LinkedHashMap<>();
                for (var entry : items.values().entrySet()) {
                    String name = entry.getKey();
                    JsonValue v = entry.getValue();
                    if (v instanceof JsonObject obj) {
                        RepositoryDef def = new RepositoryDef(
                                obj.getString("type"),
                                obj.getString("url"));
                        repos.put(name, def);
                    }
                }
                wrapper.items = repos;
            }
            config.setRepositories(wrapper);
        }
        JsonObject deps = root.getObject("dependencies");
        if (deps != null) {
            Map<String, Object> depMap = new LinkedHashMap<>();
            for (var entry : deps.values().entrySet()) {
                String key = entry.getKey();
                JsonValue value = entry.getValue();
                if (value instanceof JsonString str) {
                    depMap.put(key, str.value());
                } else if (value instanceof JsonObject obj) {
                    Map<String, Object> detail = new LinkedHashMap<>();
                    for (var detailEntry : obj.values().entrySet()) {
                        JsonValue dv = detailEntry.getValue();
                        if (dv instanceof JsonString s) detail.put(detailEntry.getKey(), s.value());
                        else if (dv instanceof JsonBoolean b) detail.put(detailEntry.getKey(), b.value());
                        else if (dv instanceof JsonNumber n) detail.put(detailEntry.getKey(), n.value());
                    }
                    depMap.put(key, detail);
                }
            }
            config.setDependencies(depMap);
        }
        return config;
    }

    private Configuration merge(Configuration project, Configuration global) {
        if (project.libDir() == null && global.libDir() != null) project.setLibDir(global.libDir());
        if (project.defaultRepository() == null && global.defaultRepository() != null)
            project.setDefaultRepository(global.defaultRepository());
        return project;
    }

    public List<DependencySpec> parseDependencies(Configuration config) {
        List<DependencySpec> result = new ArrayList<>();
        Map<String, Object> deps = config.dependencies();
        if (deps == null) return result;
        for (Map.Entry<String, Object> entry : deps.entrySet()) {
            String coordinate = entry.getKey();
            Object value = entry.getValue();
            String versionStr = null;
            boolean transitive = false;
            String repository = null;
            if (value instanceof String v) {
                versionStr = v;
            } else if (value instanceof Map<?, ?> detail) {
                @SuppressWarnings("unchecked")
                Map<String, Object> detailMap = (Map<String, Object>) detail;
                versionStr = (String) detailMap.get("version");
                if (detailMap.containsKey("transitive")) transitive = (Boolean) detailMap.get("transitive");
                if (detailMap.containsKey("repository")) repository = (String) detailMap.get("repository");
            }
            if (versionStr == null)
                throw new IllegalArgumentException("No version specified for dependency: " + coordinate);
            String[] parts = coordinate.split(":");
            String groupId = parts.length > 0 ? parts[0] : "";
            String artifactId = parts.length > 1 ? parts[1] : "";
            result.add(new DependencySpec(groupId, artifactId, coordinate, versionStr, transitive, repository));
        }
        return result;
    }
}
