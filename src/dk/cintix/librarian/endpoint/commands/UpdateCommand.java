package dk.cintix.librarian.endpoint.commands;

import dk.cintix.librarian.LibrarianCore;
import dk.cintix.librarian.LibrarianCore.LockFileInfo;

import java.nio.file.Path;

public final class UpdateCommand {
    private final LibrarianCore core;

    public UpdateCommand(LibrarianCore core) {
        this.core = core;
    }

    public int execute(Path projectDir) throws Exception {
        System.out.println("update: " + projectDir);
        LockFileInfo lockFile = core.update(projectDir);
        System.out.println("Updated " + lockFile.dependencies().size() + " dependencies");
        return 0;
    }
}
