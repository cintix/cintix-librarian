package dk.cintix.librarian.endpoint;

import dk.cintix.librarian.LibrarianCore;
import dk.cintix.librarian.artifact.services.ArtifactService;
import dk.cintix.librarian.config.services.ConfigService;
import dk.cintix.librarian.endpoint.commands.DoctorCommand;
import dk.cintix.librarian.endpoint.commands.ResolveCommand;
import dk.cintix.librarian.endpoint.commands.SyncCommand;
import dk.cintix.librarian.endpoint.commands.UpdateCommand;
import dk.cintix.librarian.git.services.GitService;
import dk.cintix.librarian.lockfile.services.LockFileService;
import dk.cintix.librarian.resolution.services.ResolutionService;
import dk.cintix.librarian.sync.services.SyncManager;

import java.nio.file.Path;

public final class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        String command = args[0];
        Path projectDir = Path.of(".").toAbsolutePath().normalize();

        // Composition root: wire all modules
        ConfigService configService = new ConfigService();
        ResolutionService resolutionService = new ResolutionService();
        ArtifactService artifactService = new ArtifactService();
        LockFileService lockFileService = new LockFileService();
        GitService gitService = new GitService();
        LibrarianCore core = new SyncManager(configService, resolutionService,
                artifactService, lockFileService, gitService);

        int exitCode;
        try {
            exitCode = switch (command) {
                case "sync" -> new SyncCommand(core).execute(projectDir);
                case "resolve" -> new ResolveCommand(core).execute(projectDir);
                case "update" -> new UpdateCommand(core).execute(projectDir);
                case "doctor" -> new DoctorCommand(core).execute(projectDir);
                case "help", "--help", "-h" -> {
                    printUsage();
                    yield 0;
                }
                default -> {
                    System.err.println("Unknown command: " + command);
                    printUsage();
                    yield 1;
                }
            };
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
            exitCode = 1;
        }

        System.exit(exitCode);
    }

    private static void printUsage() {
        System.out.println("""
                librarian — A deterministic Java library synchronizer

                Usage: librarian <command>

                Commands:
                  sync      Synchronize lib/ directory — resolve, download, clean, and lock
                  resolve   Resolve version constraints and write lock file without downloading
                  update    Re-resolve all version constraints, re-download, and update lock file
                  doctor    Check project configuration and library directory health
                  help      Show this help message
                """);
    }
}
