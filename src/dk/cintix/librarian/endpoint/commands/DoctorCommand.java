package dk.cintix.librarian.endpoint.commands;

import dk.cintix.librarian.LibrarianCore;
import dk.cintix.librarian.LibrarianCore.DoctorResult;

import java.nio.file.Path;

public final class DoctorCommand {
    private final LibrarianCore core;

    public DoctorCommand(LibrarianCore core) {
        this.core = core;
    }

    public int execute(Path projectDir) throws Exception {
        System.out.println("doctor: " + projectDir);
        DoctorResult result = core.doctor(projectDir);

        System.out.println("Config valid: " + result.configValid());
        if (!result.configValid()) {
            System.out.println("  Error: " + result.configError());
        }
        System.out.println("Dependencies: " + result.dependencyCount());

        System.out.println("Repositories reachable: " + result.reachableRepos());
        if (!result.unreachableRepos().isEmpty()) {
            System.out.println("Repositories unreachable: " + result.unreachableRepos());
        }

        System.out.println("Lock file present: " + result.lockFileExists());
        if (result.lockFileExists()) {
            System.out.println("  Entries: " + result.lockEntryCount());
        }

        if (!result.missingJars().isEmpty()) {
            System.out.println("Missing JARs: " + result.missingJars());
        }

        System.out.println("Doctor check complete.");
        return 0;
    }
}
