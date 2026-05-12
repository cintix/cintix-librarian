package dk.cintix.librarian.lockfile;

import dk.cintix.librarian.resolution.ResolutionContract.ResolvedDependency;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface LockFileContract {

    record LockEntry(String requested, String resolved, String repository, String checksum) {}

    record LockFileData(String version, Map<String, LockEntry> dependencies) {}

    LockFileData generate(List<ResolvedDependency> resolved);
    void write(LockFileData lockFile, Path projectDir) throws IOException;
    LockFileData read(Path projectDir) throws IOException;
}
