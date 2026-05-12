package dk.cintix.librarian.config.services.domain.models;

import dk.cintix.librarian.Assert;
import dk.cintix.librarian.config.ConfigContract.RepositoryDef;

import java.util.Map;

public final class ConfigurationTest {

    public static void main(String[] args) {
        Assert.reset();
        System.out.println("=== Configuration Tests ===\n");

        testDefaultLibDir();
        testDefaultRepo();
        testCustomLibDir();
        testCustomDefaultRepo();
        testReposDefaultsToEmpty();
        testCustomRepos();
        testDependenciesEmptyByDefault();
        testSetDependencies();

        Assert.summary();
        if (!Assert.allPassed()) System.exit(1);
    }

    static void testDefaultLibDir() {
        Assert.setCurrentTest("testDefaultLibDir");
        Configuration config = new Configuration();
        Assert.isNull(config.libDir());
    }

    static void testDefaultRepo() {
        Assert.setCurrentTest("testDefaultRepo");
        Configuration config = new Configuration();
        Assert.isNull(config.defaultRepository());
    }

    static void testCustomLibDir() {
        Assert.setCurrentTest("testCustomLibDir");
        Configuration config = new Configuration();
        config.setLibDir("custom-lib");
        Assert.equal("custom-lib", config.libDir());
    }

    static void testCustomDefaultRepo() {
        Assert.setCurrentTest("testCustomDefaultRepo");
        Configuration config = new Configuration();
        config.setDefaultRepository("my-repo");
        Assert.equal("my-repo", config.defaultRepository());
    }

    static void testReposDefaultsToEmpty() {
        Assert.setCurrentTest("testReposDefaultsToEmpty");
        Configuration config = new Configuration();
        Assert.equal(0, config.repositories().size());
    }

    static void testCustomRepos() {
        Assert.setCurrentTest("testCustomRepos");
        Configuration config = new Configuration();
        Configuration.RepositoriesWrapper wrapper = new Configuration.RepositoriesWrapper();
        wrapper.defaultRepo = "central";
        wrapper.items = Map.of("central", new RepositoryDef("maven", "https://repo1.maven.org/maven2"));
        config.setRepositories(wrapper);
        Assert.equal(1, config.repositories().size());
    }

    static void testDependenciesEmptyByDefault() {
        Assert.setCurrentTest("testDependenciesEmptyByDefault");
        Configuration config = new Configuration();
        Assert.isNull(config.dependencies());
    }

    static void testSetDependencies() {
        Assert.setCurrentTest("testSetDependencies");
        Configuration config = new Configuration();
        config.setDependencies(Map.of("a:b", "1.0"));
        Assert.equal(1, config.dependencies().size());
    }
}
