package dk.cintix.librarian.artifact.services.domain.rules;

import dk.cintix.librarian.Assert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public final class LibDirectoryManagerTest {
    private static Path tmpDir;

    public static void main(String[] args) throws IOException {
        Assert.reset();
        System.out.println("=== LibDirectoryManager Tests ===\n");

        tmpDir = Files.createTempDirectory("librarian-test-libdir");

        try {
            testEnsureDirectoryCreatesDir();
            testEnsureDirectoryIdempotent();
            testCleanOutdatedRemovesUnwantedJars();
            testCleanOutdatedKeepsExpectedJars();
            testCleanOutdatedEmptyDir();
            testCleanOutdatedNonExistentDir();
            testExistingJarsReturnsCorrectSet();
            testExistingJarsNonExistentDirReturnsEmptySet();
            testListJarsReturnsPaths();
            testListJarsNonExistentDirReturnsEmpty();
        } finally {
            cleanup();
        }

        Assert.summary();
        if (!Assert.allPassed()) System.exit(1);
    }

    static void cleanup() throws IOException {
        if (tmpDir != null && Files.exists(tmpDir)) {
            try (var files = Files.walk(tmpDir)) {
                files.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                });
            }
        }
    }

    static void testEnsureDirectoryCreatesDir() throws IOException {
        Assert.setCurrentTest("testEnsureDirectoryCreatesDir");
        Path libDir = tmpDir.resolve("test-create");
        LibDirectoryManager mgr = new LibDirectoryManager();
        mgr.ensureDirectory(libDir);
        Assert.isTrue(Files.exists(libDir));
        Assert.isTrue(Files.isDirectory(libDir));
    }

    static void testEnsureDirectoryIdempotent() throws IOException {
        Assert.setCurrentTest("testEnsureDirectoryIdempotent");
        Path libDir = tmpDir.resolve("test-idempotent");
        LibDirectoryManager mgr = new LibDirectoryManager();
        mgr.ensureDirectory(libDir);
        mgr.ensureDirectory(libDir); // Should not throw
        Assert.isTrue(Files.exists(libDir));
    }

    static void testCleanOutdatedRemovesUnwantedJars() throws IOException {
        Assert.setCurrentTest("testCleanOutdatedRemovesUnwantedJars");
        Path libDir = tmpDir.resolve("test-clean-remove");
        Files.createDirectories(libDir);
        Files.createFile(libDir.resolve("old.jar"));
        Files.createFile(libDir.resolve("keep.jar"));
        LibDirectoryManager mgr = new LibDirectoryManager();
        List<Path> removed = mgr.cleanOutdated(libDir, Set.of("keep.jar"));
        Assert.equal(1, removed.size());
        Assert.isFalse(Files.exists(libDir.resolve("old.jar")));
        Assert.isTrue(Files.exists(libDir.resolve("keep.jar")));
    }

    static void testCleanOutdatedKeepsExpectedJars() throws IOException {
        Assert.setCurrentTest("testCleanOutdatedKeepsExpectedJars");
        Path libDir = tmpDir.resolve("test-clean-keep");
        Files.createDirectories(libDir);
        Files.createFile(libDir.resolve("a.jar"));
        Files.createFile(libDir.resolve("b.jar"));
        LibDirectoryManager mgr = new LibDirectoryManager();
        mgr.cleanOutdated(libDir, Set.of("a.jar", "b.jar"));
        Assert.isTrue(Files.exists(libDir.resolve("a.jar")));
        Assert.isTrue(Files.exists(libDir.resolve("b.jar")));
    }

    static void testCleanOutdatedEmptyDir() throws IOException {
        Assert.setCurrentTest("testCleanOutdatedEmptyDir");
        Path libDir = tmpDir.resolve("test-clean-empty");
        Files.createDirectories(libDir);
        LibDirectoryManager mgr = new LibDirectoryManager();
        List<Path> removed = mgr.cleanOutdated(libDir, Set.of());
        Assert.equal(0, removed.size());
    }

    static void testCleanOutdatedNonExistentDir() throws IOException {
        Assert.setCurrentTest("testCleanOutdatedNonExistentDir");
        Path libDir = tmpDir.resolve("test-clean-nonexistent");
        LibDirectoryManager mgr = new LibDirectoryManager();
        List<Path> removed = mgr.cleanOutdated(libDir, Set.of("a.jar"));
        Assert.equal(0, removed.size());
    }

    static void testExistingJarsReturnsCorrectSet() throws IOException {
        Assert.setCurrentTest("testExistingJarsReturnsCorrectSet");
        Path libDir = tmpDir.resolve("test-existing-jars");
        Files.createDirectories(libDir);
        Files.createFile(libDir.resolve("a.jar"));
        Files.createFile(libDir.resolve("b.jar"));
        Files.createFile(libDir.resolve("not-a-jar.txt"));
        LibDirectoryManager mgr = new LibDirectoryManager();
        Set<String> jars = mgr.existingJars(libDir);
        Assert.isTrue(jars.contains("a.jar"));
        Assert.isTrue(jars.contains("b.jar"));
        Assert.isFalse(jars.contains("not-a-jar.txt"));
    }

    static void testExistingJarsNonExistentDirReturnsEmptySet() throws IOException {
        Assert.setCurrentTest("testExistingJarsNonExistentDirReturnsEmptySet");
        LibDirectoryManager mgr = new LibDirectoryManager();
        Set<String> jars = mgr.existingJars(tmpDir.resolve("nonexistent"));
        Assert.equal(0, jars.size());
    }

    static void testListJarsReturnsPaths() throws IOException {
        Assert.setCurrentTest("testListJarsReturnsPaths");
        Path libDir = tmpDir.resolve("test-list-jars");
        Files.createDirectories(libDir);
        Files.createFile(libDir.resolve("x.jar"));
        LibDirectoryManager mgr = new LibDirectoryManager();
        List<Path> jars = mgr.listJars(libDir);
        Assert.equal(1, jars.size());
    }

    static void testListJarsNonExistentDirReturnsEmpty() throws IOException {
        Assert.setCurrentTest("testListJarsNonExistentDirReturnsEmpty");
        LibDirectoryManager mgr = new LibDirectoryManager();
        List<Path> jars = mgr.listJars(tmpDir.resolve("no-dir"));
        Assert.equal(0, jars.size());
    }
}
