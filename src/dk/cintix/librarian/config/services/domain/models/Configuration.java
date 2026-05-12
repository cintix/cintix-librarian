package dk.cintix.librarian.config.services.domain.models;

import dk.cintix.librarian.config.ConfigContract.RepositoryDef;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Configuration {
    private String libDir;
    private String defaultRepository;
    private RepositoriesWrapper repositories;
    private Map<String, Object> dependencies;

    public static final class RepositoriesWrapper {
        public String defaultRepo;
        public Map<String, RepositoryDef> items;

        public RepositoriesWrapper() {}
    }

    public Configuration() {}

    public String libDir() { return libDir; }
    public String defaultRepository() { return defaultRepository; }
    public Map<String, RepositoryDef> repositories() {
        return repositories != null ? repositories.items : new LinkedHashMap<>();
    }
    public Map<String, Object> dependencies() { return dependencies; }

    public void setLibDir(String libDir) { this.libDir = libDir; }
    public void setDefaultRepository(String defaultRepository) { this.defaultRepository = defaultRepository; }
    public void setRepositories(RepositoriesWrapper repositories) { this.repositories = repositories; }
    public void setDependencies(Map<String, Object> dependencies) { this.dependencies = dependencies; }
}
