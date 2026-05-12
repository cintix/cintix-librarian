package dk.cintix.librarian.endpoint.commands;

import dk.cintix.librarian.LibrarianCore;
import dk.cintix.librarian.LibrarianCore.LockFileInfo;

import java.nio.file.Path;

public final class ResolveCommand {
    private final LibrarianCore core;

    public ResolveCommand(LibrarianCore core) {
        this.core = core;
    }

    public int execute(Path projectDir) throws Exception {
        System.out.println("resolve: " + projectDir);
        LockFileInfo lockFile = core.resolve(projectDir);
        System.out.println("Resolved " + lockFile.dependencies().size() + " dependencies");
        return 0;
    }
}
