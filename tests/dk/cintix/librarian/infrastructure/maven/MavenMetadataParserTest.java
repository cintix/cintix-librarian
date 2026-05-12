package dk.cintix.librarian.infrastructure.maven;

import dk.cintix.librarian.Assert;
import dk.cintix.librarian.resolution.services.persistence.MavenMetadataParser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class MavenMetadataParserTest {
    private static final MavenMetadataParser parser = new MavenMetadataParser();

    public static void main(String[] args) throws Exception {
        Assert.reset();
        System.out.println("=== MavenMetadataParser Tests ===\n");

        testParseVersionsSingleVersion();
        testParseVersionsMultipleVersions();
        testParseVersionsEmptyMetadata();
        testParseVersionsNoVersioning();
        testParseLatestPresent();
        testParseLatestFallbackToRelease();
        testParseLatestMissingBoth();
        testParseVersionsRealWorldFormat();

        Assert.summary();
        if (!Assert.allPassed()) System.exit(1);
    }

    static InputStream xmlStream(String xml) {
        return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    }

    static void testParseVersionsSingleVersion() throws Exception {
        Assert.setCurrentTest("testParseVersionsSingleVersion");
        List<String> versions = parser.parseVersions(xmlStream(
                "<metadata><groupId>g</groupId><artifactId>a</artifactId><versioning><versions><version>1.0</version></versions></versioning></metadata>"));
        Assert.equal(1, versions.size());
        Assert.equal("1.0", versions.get(0));
    }

    static void testParseVersionsMultipleVersions() throws Exception {
        Assert.setCurrentTest("testParseVersionsMultipleVersions");
        List<String> versions = parser.parseVersions(xmlStream(
                "<metadata><versioning><versions><version>1.0</version><version>2.0</version><version>3.0</version></versions></versioning></metadata>"));
        Assert.equal(3, versions.size());
    }

    static void testParseVersionsEmptyMetadata() throws Exception {
        Assert.setCurrentTest("testParseVersionsEmptyMetadata");
        List<String> versions = parser.parseVersions(xmlStream("<metadata></metadata>"));
        Assert.equal(0, versions.size());
    }

    static void testParseVersionsNoVersioning() throws Exception {
        Assert.setCurrentTest("testParseVersionsNoVersioning");
        List<String> versions = parser.parseVersions(xmlStream(
                "<metadata><groupId>g</groupId><artifactId>a</artifactId></metadata>"));
        Assert.equal(0, versions.size());
    }

    static void testParseLatestPresent() throws Exception {
        Assert.setCurrentTest("testParseLatestPresent");
        String latest = parser.parseLatest(xmlStream(
                "<metadata><versioning><latest>2.5.0</latest><release>2.4.0</release></versioning></metadata>"));
        Assert.equal("2.5.0", latest);
    }

    static void testParseLatestFallbackToRelease() throws Exception {
        Assert.setCurrentTest("testParseLatestFallbackToRelease");
        String latest = parser.parseLatest(xmlStream(
                "<metadata><versioning><release>2.4.0</release></versioning></metadata>"));
        Assert.isNull(latest);
    }

    static void testParseLatestMissingBoth() throws Exception {
        Assert.setCurrentTest("testParseLatestMissingBoth");
        String latest = parser.parseLatest(xmlStream("<metadata></metadata>"));
        Assert.isNull(latest);
    }

    static void testParseVersionsRealWorldFormat() throws Exception {
        Assert.setCurrentTest("testParseVersionsRealWorldFormat");
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><metadata><groupId>com.google.code.gson</groupId><artifactId>gson</artifactId><versioning><latest>2.11.0</latest><release>2.11.0</release><versions><version>2.8.9</version><version>2.9.0</version><version>2.10.0</version><version>2.11.0</version></versions></versioning></metadata>";
        List<String> versions = parser.parseVersions(xmlStream(xml));
        Assert.equal(4, versions.size());
        Assert.listContains(versions, "2.11.0");
    }
}
