package dk.cintix.librarian.resolution.services.domain.rules;

import dk.cintix.librarian.Assert;
import dk.cintix.librarian.resolution.ResolutionContract.ResolvedDependency;

public final class ResolvedDependencyTest {

    public static void main(String[] args) {
        Assert.reset();
        System.out.println("=== ResolvedDependency Tests ===\n");

        testBasicConstruction();
        testFileNameFormat();
        testAllAccessors();
        testToString();
        testChecksumCanBeNull();

        Assert.summary();
        if (!Assert.allPassed()) System.exit(1);
    }

    static void testBasicConstruction() {
        Assert.setCurrentTest("testBasicConstruction");
        ResolvedDependency dep = new ResolvedDependency(
                "org.slf4j", "slf4j-api", "org.slf4j:slf4j-api", "2.0.*", "2.0.16", "maven-central", "abc123");
        Assert.equal("org.slf4j", dep.groupId());
        Assert.equal("slf4j-api", dep.artifactId());
        Assert.equal("2.0.16", dep.resolvedVersion());
    }

    static void testFileNameFormat() {
        Assert.setCurrentTest("testFileNameFormat");
        ResolvedDependency dep = new ResolvedDependency("g", "my-lib", "g:my-lib", "1.*", "1.5.0", "r", "sha1");
        Assert.equal("my-lib-1.5.0.jar", dep.fileName());
    }

    static void testAllAccessors() {
        Assert.setCurrentTest("testAllAccessors");
        ResolvedDependency dep = new ResolvedDependency("a.b", "c", "a.b:c", "^1.0", "1.2.3", "repo1", "chk");
        Assert.equal("a.b", dep.groupId());
        Assert.equal("c", dep.artifactId());
        Assert.equal("a.b:c", dep.coordinate());
        Assert.equal("^1.0", dep.requestedVersion());
        Assert.equal("1.2.3", dep.resolvedVersion());
        Assert.equal("repo1", dep.repository());
        Assert.equal("chk", dep.checksum());
    }

    static void testToString() {
        Assert.setCurrentTest("testToString");
        ResolvedDependency dep = new ResolvedDependency("g", "a", "g:a", "1.0", "1.0", "r", null);
        Assert.notNull(dep.toString());
    }

    static void testChecksumCanBeNull() {
        Assert.setCurrentTest("testChecksumCanBeNull");
        ResolvedDependency dep = new ResolvedDependency("g", "a", "g:a", "1.0", "1.0", "r", null);
        Assert.isNull(dep.checksum());
    }
}
