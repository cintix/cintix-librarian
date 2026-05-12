package dk.cintix.librarian.config.services.persistence;

import dk.cintix.librarian.Assert;
import dk.cintix.librarian.config.ConfigContract.DependencySpec;
import dk.cintix.librarian.config.services.domain.models.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ConfigReaderTest {
    private static Path tmpDir;

    public static void main(String[] args) throws IOException {
        Assert.reset();
        System.out.println("=== ConfigReader Tests ===\n");

        tmpDir = Files.createTempDirectory("librarian-test-config");

        try {
            testMissingConfigFile();
            testParseMinimalConfig();
            testParseFullConfig();
            testParseDependenciesSimpleForm();
            testParseDependenciesDetailForm();
            testParseDependenciesMixed();
            testParseDependenciesWithTransitive();
            testParseDependenciesWithRepository();
            testParseDependenciesMissingVersion();
            testDefaultValues();
            testCustomLibDir();
            testRepositoriesParsing();
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

    static void writeConfig(String json) throws IOException {
        Files.writeString(tmpDir.resolve("librarian.json"), json);
    }

    static void testMissingConfigFile() {
        Assert.setCurrentTest("testMissingConfigFile");
        ConfigReader reader = new ConfigReader();
        try {
            reader.read(tmpDir.resolve("no-config"));
            Assert.isTrue(false); // Should not reach here
        } catch (IOException e) {
            Assert.isTrue(true);
        }
    }

    static void testParseMinimalConfig() throws IOException {
        Assert.setCurrentTest("testParseMinimalConfig");
        writeConfig("{\"dependencies\":{}}");
        ConfigReader reader = new ConfigReader();
        Configuration config = reader.read(tmpDir);
        Assert.notNull(config);
    }

    static void testParseFullConfig() throws IOException {
        Assert.setCurrentTest("testParseFullConfig");
        writeConfig("{\"libDir\":\"lib\",\"defaultRepository\":\"central\",\"dependencies\":{\"a:b\":\"1.0\"}}");
        ConfigReader reader = new ConfigReader();
        Configuration config = reader.read(tmpDir);
        Assert.equal("lib", config.libDir());
        Assert.equal("central", config.defaultRepository());
    }

    static void testParseDependenciesSimpleForm() throws IOException {
        Assert.setCurrentTest("testParseDependenciesSimpleForm");
        writeConfig("{\"dependencies\":{\"org.slf4j:slf4j-api\":\"2.0.16\"}}");
        ConfigReader reader = new ConfigReader();
        Configuration config = reader.read(tmpDir);
        List<DependencySpec> deps = reader.parseDependencies(config);
        Assert.equal(1, deps.size());
        Assert.equal("org.slf4j:slf4j-api", deps.get(0).coordinate());
        Assert.equal("2.0.16", deps.get(0).version());
    }

    static void testParseDependenciesDetailForm() throws IOException {
        Assert.setCurrentTest("testParseDependenciesDetailForm");
        writeConfig("{\"dependencies\":{\"g:a\":{\"version\":\"^2.0\",\"transitive\":true}}}");
        ConfigReader reader = new ConfigReader();
        Configuration config = reader.read(tmpDir);
        List<DependencySpec> deps = reader.parseDependencies(config);
        Assert.equal(1, deps.size());
        Assert.equal("^2.0", deps.get(0).version());
        Assert.isTrue(deps.get(0).transitive());
    }

    static void testParseDependenciesMixed() throws IOException {
        Assert.setCurrentTest("testParseDependenciesMixed");
        writeConfig("{\"dependencies\":{\"a:b\":\"1.0\",\"c:d\":{\"version\":\"^2.0\",\"transitive\":false}}}");
        ConfigReader reader = new ConfigReader();
        Configuration config = reader.read(tmpDir);
        List<DependencySpec> deps = reader.parseDependencies(config);
        Assert.equal(2, deps.size());
    }

    static void testParseDependenciesWithTransitive() throws IOException {
        Assert.setCurrentTest("testParseDependenciesWithTransitive");
        writeConfig("{\"dependencies\":{\"g:a\":{\"version\":\"1.0\",\"transitive\":true}}}");
        ConfigReader reader = new ConfigReader();
        Configuration config = reader.read(tmpDir);
        List<DependencySpec> deps = reader.parseDependencies(config);
        Assert.isTrue(deps.get(0).transitive());
    }

    static void testParseDependenciesWithRepository() throws IOException {
        Assert.setCurrentTest("testParseDependenciesWithRepository");
        writeConfig("{\"dependencies\":{\"g:a\":{\"version\":\"1.0\",\"repository\":\"my-repo\"}}}");
        ConfigReader reader = new ConfigReader();
        Configuration config = reader.read(tmpDir);
        List<DependencySpec> deps = reader.parseDependencies(config);
        Assert.equal("my-repo", deps.get(0).repository());
    }

    static void testParseDependenciesMissingVersion() throws IOException {
        Assert.setCurrentTest("testParseDependenciesMissingVersion");
        writeConfig("{\"dependencies\":{\"g:a\":{\"transitive\":true}}}");
        ConfigReader reader = new ConfigReader();
        Configuration config = reader.read(tmpDir);
        Assert.throwsException(IllegalArgumentException.class, () -> reader.parseDependencies(config));
    }

    static void testDefaultValues() throws IOException {
        Assert.setCurrentTest("testDefaultValues");
        writeConfig("{\"dependencies\":{}}");
        ConfigReader reader = new ConfigReader();
        Configuration config = reader.read(tmpDir);
        Assert.isNull(config.libDir());
        Assert.isNull(config.defaultRepository());
    }

    static void testCustomLibDir() throws IOException {
        Assert.setCurrentTest("testCustomLibDir");
        writeConfig("{\"libDir\":\"external/lib\",\"dependencies\":{}}");
        ConfigReader reader = new ConfigReader();
        Configuration config = reader.read(tmpDir);
        Assert.equal("external/lib", config.libDir());
    }

    static void testRepositoriesParsing() throws IOException {
        Assert.setCurrentTest("testRepositoriesParsing");
        writeConfig("{\"repositories\":{\"default\":\"central\",\"items\":{\"central\":{\"type\":\"maven\",\"url\":\"https://repo1.maven.org/maven2\"}}},\"dependencies\":{}}");
        ConfigReader reader = new ConfigReader();
        Configuration config = reader.read(tmpDir);
        Assert.equal(1, config.repositories().size());
    }
}
