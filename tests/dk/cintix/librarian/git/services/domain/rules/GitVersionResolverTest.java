package dk.cintix.librarian.git.services.domain.rules;

import dk.cintix.librarian.Assert;

public final class GitVersionResolverTest {

    public static void main(String[] args) {
        Assert.reset();
        System.out.println("=== GitVersionResolver Tests ===\n");

        testParseVersionWithVPrefix();
        testParseVersionWithoutVPrefix();
        testParseVersionThreeParts();
        testParseVersionTwoParts();
        testParseVersionOnePart();
        testParseVersionNonNumeric();
        testParseVersionComplex();

        Assert.summary();
        if (!Assert.allPassed()) System.exit(1);
    }

    static void testParseVersionWithVPrefix() {
        Assert.setCurrentTest("testParseVersionWithVPrefix");
        int[] parts = GitVersionResolver.parseVersion("v2.1.5");
        Assert.equal(2, parts[0]);
        Assert.equal(1, parts[1]);
        Assert.equal(5, parts[2]);
    }

    static void testParseVersionWithoutVPrefix() {
        Assert.setCurrentTest("testParseVersionWithoutVPrefix");
        int[] parts = GitVersionResolver.parseVersion("3.0.0");
        Assert.equal(3, parts[0]);
        Assert.equal(0, parts[1]);
        Assert.equal(0, parts[2]);
    }

    static void testParseVersionThreeParts() {
        Assert.setCurrentTest("testParseVersionThreeParts");
        int[] parts = GitVersionResolver.parseVersion("v1.2.3");
        Assert.equal(1, parts[0]);
        Assert.equal(2, parts[1]);
        Assert.equal(3, parts[2]);
    }

    static void testParseVersionTwoParts() {
        Assert.setCurrentTest("testParseVersionTwoParts");
        int[] parts = GitVersionResolver.parseVersion("v2.1");
        Assert.equal(2, parts[0]);
        Assert.equal(1, parts[1]);
        Assert.equal(0, parts[2]);
    }

    static void testParseVersionOnePart() {
        Assert.setCurrentTest("testParseVersionOnePart");
        int[] parts = GitVersionResolver.parseVersion("v2");
        Assert.equal(2, parts[0]);
        Assert.equal(0, parts[1]);
        Assert.equal(0, parts[2]);
    }

    static void testParseVersionNonNumeric() {
        Assert.setCurrentTest("testParseVersionNonNumeric");
        int[] parts = GitVersionResolver.parseVersion("not-a-version");
        Assert.equal(0, parts[0]);
        Assert.equal(0, parts[1]);
        Assert.equal(0, parts[2]);
    }

    static void testParseVersionComplex() {
        Assert.setCurrentTest("testParseVersionComplex");
        int[] parts = GitVersionResolver.parseVersion("v10.20.30");
        Assert.equal(10, parts[0]);
        Assert.equal(20, parts[1]);
        Assert.equal(30, parts[2]);
    }
}
