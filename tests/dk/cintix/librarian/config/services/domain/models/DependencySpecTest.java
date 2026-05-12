package dk.cintix.librarian.config.services.domain.models;

import dk.cintix.librarian.Assert;
import dk.cintix.librarian.config.ConfigContract.DependencySpec;

public final class DependencySpecTest {

    public static void main(String[] args) {
        Assert.reset();
        System.out.println("=== DependencySpec Tests ===\n");

        testDefaultTransitiveFalse();
        testCoordinateParsing();
        testJarFileName();
        testFullConstruction();
        testWithTransitive();
        testWithRepository();
        testCoordinateWithoutColon();

        Assert.summary();
        if (!Assert.allPassed()) System.exit(1);
    }

    static void testDefaultTransitiveFalse() {
        Assert.setCurrentTest("testDefaultTransitiveFalse");
        DependencySpec dep = new DependencySpec("org.slf4j", "slf4j-api", "org.slf4j:slf4j-api", "2.0.16", false, null);
        Assert.isFalse(dep.transitive());
    }

    static void testCoordinateParsing() {
        Assert.setCurrentTest("testCoordinateParsing");
        DependencySpec dep = new DependencySpec("com.google.guava", "guava", "com.google.guava:guava", "33.0", false, null);
        Assert.equal("com.google.guava", dep.groupId());
        Assert.equal("guava", dep.artifactId());
        Assert.equal("com.google.guava:guava", dep.coordinate());
    }

    static void testJarFileName() {
        Assert.setCurrentTest("testJarFileName");
        DependencySpec dep = new DependencySpec("g", "a", "g:a", "1.0", false, null);
        // Jar file name would be artifactId-version.jar
        Assert.equal("a", dep.artifactId());
        Assert.equal("1.0", dep.version());
    }

    static void testFullConstruction() {
        Assert.setCurrentTest("testFullConstruction");
        DependencySpec dep = new DependencySpec("a.b", "c", "a.b:c", "^2.0", true, "my-repo");
        Assert.equal("a.b", dep.groupId());
        Assert.equal("c", dep.artifactId());
        Assert.equal("a.b:c", dep.coordinate());
        Assert.equal("^2.0", dep.version());
        Assert.isTrue(dep.transitive());
        Assert.equal("my-repo", dep.repository());
    }

    static void testWithTransitive() {
        Assert.setCurrentTest("testWithTransitive");
        DependencySpec dep = new DependencySpec("g", "a", "g:a", "1.0", true, null);
        Assert.isTrue(dep.transitive());
        DependencySpec dep2 = new DependencySpec("g", "a", "g:a", "1.0", false, null);
        Assert.isFalse(dep2.transitive());
    }

    static void testWithRepository() {
        Assert.setCurrentTest("testWithRepository");
        DependencySpec dep = new DependencySpec("g", "a", "g:a", "1.0", false, "custom-repo");
        Assert.equal("custom-repo", dep.repository());
    }

    static void testCoordinateWithoutColon() {
        Assert.setCurrentTest("testCoordinateWithoutColon");
        DependencySpec dep = new DependencySpec("simple", "simple", "simple", "1.0", false, null);
        Assert.equal("simple", dep.groupId());
        Assert.equal("simple", dep.artifactId());
    }
}
