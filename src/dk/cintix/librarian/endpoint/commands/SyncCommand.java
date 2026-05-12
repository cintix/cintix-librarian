package dk.cintix.librarian.endpoint.commands;

import dk.cintix.librarian.LibrarianCore;
import dk.cintix.librarian.LibrarianCore.ResolvedDepInfo;
import dk.cintix.librarian.LibrarianCore.SyncResult;

import java.nio.file.Path;

public final class SyncCommand {
    private final LibrarianCore core;

    public SyncCommand(LibrarianCore core) {
        this.core = core;
    }

    public int execute(Path projectDir) throws Exception {
        System.out.println("sync: " + projectDir);
        SyncResult result = core.sync(projectDir);

        for (ResolvedDepInfo dep : result.resolved()) {
            System.out.println("  resolved " + dep.coordinate() + " -> " + dep.resolvedVersion());
        }
        for (String removed : result.removed()) {
            System.out.println("  removed " + removed);
        }
        for (String error : result.errors()) {
            System.err.println("  error: " + error);
        }
        System.out.println("Lock file written: " + projectDir.resolve("librarian.lock.json"));
        return result.errors().isEmpty() ? 0 : 1;
    }
}
