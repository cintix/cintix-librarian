package dk.cintix.librarian.lockfile.services.domain.models;

import dk.cintix.librarian.lockfile.LockFileContract.LockEntry;

import java.util.LinkedHashMap;
import java.util.Map;

public final class LockFile {
    private String version;
    private Map<String, LockEntry> dependencies;

    public LockFile() {}

    public String version() { return version; }
    public Map<String, LockEntry> dependencies() { return dependencies; }

    public void setVersion(String version) { this.version = version; }
    public void setDependencies(Map<String, LockEntry> dependencies) { this.dependencies = dependencies; }
}
