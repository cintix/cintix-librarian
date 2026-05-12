package dk.cintix.librarian.lockfile.services.domain.models;

import dk.cintix.librarian.Assert;
import dk.cintix.librarian.lockfile.LockFileContract.LockEntry;

import java.util.Map;

public final class LockFileTest {

    public static void main(String[] args) {
        Assert.reset();
        System.out.println("=== LockFile Tests ===\n");

        testDefaultVersion();
        testEmptyDependencies();
        testSetVersion();
        testSetDependencies();
        testLockEntryFields();
        testLockEntryRecord();

        Assert.summary();
        if (!Assert.allPassed()) System.exit(1);
    }

    static void testDefaultVersion() {
        Assert.setCurrentTest("testDefaultVersion");
        LockFile lockFile = new LockFile();
        Assert.isNull(lockFile.version());
    }

    static void testEmptyDependencies() {
        Assert.setCurrentTest("testEmptyDependencies");
        LockFile lockFile = new LockFile();
        Assert.isNull(lockFile.dependencies());
    }

    static void testSetVersion() {
        Assert.setCurrentTest("testSetVersion");
        LockFile lockFile = new LockFile();
        lockFile.setVersion("1");
        Assert.equal("1", lockFile.version());
    }

    static void testSetDependencies() {
        Assert.setCurrentTest("testSetDependencies");
        LockFile lockFile = new LockFile();
        LockEntry entry = new LockEntry("^2.0", "2.1.0", "central", "abc123");
        lockFile.setDependencies(Map.of("g:a", entry));
        Assert.equal(1, lockFile.dependencies().size());
        Assert.equal("^2.0", lockFile.dependencies().get("g:a").requested());
    }

    static void testLockEntryFields() {
        Assert.setCurrentTest("testLockEntryFields");
        LockEntry entry = new LockEntry("^2.0", "2.1.0", "central", "abc123");
        Assert.equal("^2.0", entry.requested());
        Assert.equal("2.1.0", entry.resolved());
        Assert.equal("central", entry.repository());
        Assert.equal("abc123", entry.checksum());
    }

    static void testLockEntryRecord() {
        Assert.setCurrentTest("testLockEntryRecord");
        LockEntry e1 = new LockEntry("1.0", "1.0.0", "r", null);
        Assert.equal("1.0", e1.requested());
        Assert.isNull(e1.checksum());
    }
}
