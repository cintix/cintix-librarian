package dk.cintix.librarian.lockfile.services.persistence;

import dk.cintix.librarian.Assert;
import dk.cintix.librarian.infrastructure.json.JsonParser;
import dk.cintix.librarian.lockfile.LockFileContract.LockEntry;
import dk.cintix.librarian.lockfile.LockFileContract.LockFileData;
import dk.cintix.librarian.resolution.ResolutionContract.ResolvedDependency;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class LockFileWriterTest {
    private static Path tmpDir;

    public static void main(String[] args) throws IOException {
        Assert.reset();
        System.out.println("=== LockFileWriter Tests ===\n");

        tmpDir = Files.createTempDirectory("librarian-test-lockfile");

        try {
            testGenerateEmpty();
            testGenerateSingleDependency();
            testGenerateMultipleDependencies();
            testWriteAndReadRoundtrip();
            testReadNonExistentFile();
            testWrittenFileIsValidJson();
            testGeneratedLockEntryFields();
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

    static void testGenerateEmpty() {
        Assert.setCurrentTest("testGenerateEmpty");
        LockFileWriter writer = new LockFileWriter();
        LockFileData data = writer.generate(List.of());
        Assert.equal(0, data.dependencies().size());
        Assert.equal("1", data.version());
    }

    static void testGenerateSingleDependency() {
        Assert.setCurrentTest("testGenerateSingleDependency");
        LockFileWriter writer = new LockFileWriter();
        ResolvedDependency dep = new ResolvedDependency("g", "a", "g:a", "^1.0", "1.2.3", "central", "abc");
        LockFileData data = writer.generate(List.of(dep));
        Assert.equal(1, data.dependencies().size());
        LockEntry entry = data.dependencies().get("g:a");
        Assert.notNull(entry);
        Assert.equal("^1.0", entry.requested());
        Assert.equal("1.2.3", entry.resolved());
        Assert.equal("central", entry.repository());
        Assert.equal("abc", entry.checksum());
    }

    static void testGenerateMultipleDependencies() {
        Assert.setCurrentTest("testGenerateMultipleDependencies");
        LockFileWriter writer = new LockFileWriter();
        ResolvedDependency d1 = new ResolvedDependency("g1", "a1", "g1:a1", "1.0", "1.0.0", "r1", "c1");
        ResolvedDependency d2 = new ResolvedDependency("g2", "a2", "g2:a2", "2.0", "2.0.0", "r2", "c2");
        LockFileData data = writer.generate(List.of(d1, d2));
        Assert.equal(2, data.dependencies().size());
    }

    static void testWriteAndReadRoundtrip() throws IOException {
        Assert.setCurrentTest("testWriteAndReadRoundtrip");
        LockFileWriter writer = new LockFileWriter();
        ResolvedDependency dep = new ResolvedDependency("g", "a", "g:a", "1.0", "1.0.0", "r", "chk");
        LockFileData written = writer.generate(List.of(dep));
        writer.write(written, tmpDir);
        LockFileData read = writer.read(tmpDir);
        Assert.notNull(read);
        Assert.equal(1, read.dependencies().size());
        Assert.equal("1.0.0", read.dependencies().get("g:a").resolved());
    }

    static void testReadNonExistentFile() throws IOException {
        Assert.setCurrentTest("testReadNonExistentFile");
        LockFileWriter writer = new LockFileWriter();
        LockFileData data = writer.read(tmpDir.resolve("nonexistent"));
        Assert.isNull(data);
    }

    static void testWrittenFileIsValidJson() throws IOException {
        Assert.setCurrentTest("testWrittenFileIsValidJson");
        LockFileWriter writer = new LockFileWriter();
        ResolvedDependency dep = new ResolvedDependency("g", "a", "g:a", "1.0", "1.0.0", "r", null);
        LockFileData data = writer.generate(List.of(dep));
        writer.write(data, tmpDir);
        String content = Files.readString(tmpDir.resolve("librarian.lock.json"));
        Assert.notNull(JsonParser.parse(content));
    }

    static void testGeneratedLockEntryFields() {
        Assert.setCurrentTest("testGeneratedLockEntryFields");
        LockFileWriter writer = new LockFileWriter();
        ResolvedDependency dep = new ResolvedDependency("x", "y", "x:y", "^1", "1.5", null, null);
        LockFileData data = writer.generate(List.of(dep));
        LockEntry entry = data.dependencies().get("x:y");
        Assert.equal("^1", entry.requested());
        Assert.equal("1.5", entry.resolved());
        Assert.isNull(entry.repository());
        Assert.isNull(entry.checksum());
    }
}
