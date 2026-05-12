package dk.cintix.librarian.lockfile.services.persistence;

import dk.cintix.librarian.infrastructure.json.JsonObject;
import dk.cintix.librarian.infrastructure.json.JsonParser;
import dk.cintix.librarian.infrastructure.json.JsonString;
import dk.cintix.librarian.infrastructure.json.JsonValue;
import dk.cintix.librarian.infrastructure.json.JsonWriter;
import dk.cintix.librarian.lockfile.LockFileContract.LockEntry;
import dk.cintix.librarian.lockfile.LockFileContract.LockFileData;
import dk.cintix.librarian.lockfile.services.domain.models.LockFile;
import dk.cintix.librarian.resolution.ResolutionContract.ResolvedDependency;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LockFileWriter {
    private static final String LOCK_FILE = "librarian.lock.json";

    public LockFileData generate(List<ResolvedDependency> resolved) {
        Map<String, LockEntry> entries = new LinkedHashMap<>();
        for (ResolvedDependency dep : resolved) {
            entries.put(dep.coordinate(), new LockEntry(
                    dep.requestedVersion(), dep.resolvedVersion(),
                    dep.repository(), dep.checksum()));
        }
        return new LockFileData("1", entries);
    }

    public void write(LockFileData lockFileData, Path projectDir) throws IOException {
        Map<String, JsonValue> depMap = new LinkedHashMap<>();
        for (var entry : lockFileData.dependencies().entrySet()) {
            Map<String, JsonValue> entryMap = new LinkedHashMap<>();
            entryMap.put("requested", new JsonString(entry.getValue().requested()));
            entryMap.put("resolved", new JsonString(entry.getValue().resolved()));
            if (entry.getValue().repository() != null)
                entryMap.put("repository", new JsonString(entry.getValue().repository()));
            if (entry.getValue().checksum() != null)
                entryMap.put("checksum", new JsonString(entry.getValue().checksum()));
            depMap.put(entry.getKey(), new JsonObject(entryMap));
        }
        Map<String, JsonValue> root = new LinkedHashMap<>();
        root.put("version", new JsonString(lockFileData.version()));
        root.put("dependencies", new JsonObject(depMap));
        Path lockPath = projectDir.resolve(LOCK_FILE);
        Files.writeString(lockPath, JsonWriter.write(new JsonObject(root), true));
    }

    public LockFileData read(Path projectDir) throws IOException {
        Path lockPath = projectDir.resolve(LOCK_FILE);
        if (!Files.exists(lockPath)) return null;
        String json = Files.readString(lockPath);
        JsonObject root = JsonParser.parse(json).asObject();
        String version = root.getString("version");
        Map<String, LockEntry> entries = new LinkedHashMap<>();
        JsonObject deps = root.getObject("dependencies");
        if (deps != null) {
            for (var entry : deps.values().entrySet()) {
                if (entry.getValue() instanceof JsonObject obj) {
                    entries.put(entry.getKey(), new LockEntry(
                            obj.getString("requested"),
                            obj.getString("resolved"),
                            obj.getString("repository"),
                            obj.getString("checksum")));
                }
            }
        }
        return new LockFileData(version != null ? version : "1", entries);
    }

    // Internal helper for converting from internal LockFile model
    public LockFile generateLegacy(List<ResolvedDependency> resolved) {
        LockFile lockFile = new LockFile();
        lockFile.setVersion("1");
        Map<String, LockEntry> entries = new LinkedHashMap<>();
        for (ResolvedDependency dep : resolved) {
            entries.put(dep.coordinate(), new LockEntry(
                    dep.requestedVersion(), dep.resolvedVersion(),
                    dep.repository(), dep.checksum()));
        }
        lockFile.setDependencies(entries);
        return lockFile;
    }
}
