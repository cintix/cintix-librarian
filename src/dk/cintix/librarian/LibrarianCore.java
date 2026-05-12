package dk.cintix.librarian;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface LibrarianCore {

    record ResolvedDepInfo(String coordinate, String requestedVersion, String resolvedVersion,
                            String repository, String checksum) {}

    record LockEntryInfo(String requested, String resolved, String repository, String checksum) {}

    record LockFileInfo(Map<String, LockEntryInfo> dependencies) {}

    record SyncResult(List<ResolvedDepInfo> resolved, List<String> removed,
                       List<String> errors, LockFileInfo lockFile) {}

    record DoctorResult(boolean configValid, String configError, int dependencyCount,
                          boolean lockFileExists, int lockEntryCount,
                          List<String> reachableRepos, List<String> unreachableRepos,
                          List<String> missingJars) {}

    SyncResult sync(Path projectDir) throws IOException;

    LockFileInfo resolve(Path projectDir) throws IOException;

    LockFileInfo update(Path projectDir) throws IOException;

    DoctorResult doctor(Path projectDir) throws IOException;
}
