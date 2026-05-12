package dk.cintix.librarian.lockfile.services;

import dk.cintix.librarian.lockfile.LockFileContract;
import dk.cintix.librarian.lockfile.LockFileContract.LockFileData;
import dk.cintix.librarian.lockfile.services.persistence.LockFileWriter;
import dk.cintix.librarian.resolution.ResolutionContract.ResolvedDependency;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class LockFileService implements LockFileContract {
    private final LockFileWriter lockFileWriter;

    public LockFileService() {
        this.lockFileWriter = new LockFileWriter();
    }

    @Override
    public LockFileData generate(List<ResolvedDependency> resolved) {
        return lockFileWriter.generate(resolved);
    }

    @Override
    public void write(LockFileData lockFile, Path projectDir) throws IOException {
        lockFileWriter.write(lockFile, projectDir);
    }

    @Override
    public LockFileData read(Path projectDir) throws IOException {
        return lockFileWriter.read(projectDir);
    }
}
