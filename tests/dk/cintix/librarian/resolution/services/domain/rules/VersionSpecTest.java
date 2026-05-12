package dk.cintix.librarian.resolution.services.domain.rules;

import dk.cintix.librarian.Assert;
import dk.cintix.librarian.resolution.ResolutionContract.VersionSpec;
import dk.cintix.librarian.resolution.ResolutionContract.VersionSpec.Type;

public final class VersionSpecTest {

    public static void main(String[] args) {
        Assert.reset();
        System.out.println("=== VersionSpec Tests ===\n");

        testParseExact();
        testParsePatchStream();
        testParseMajorStream();
        testParseCompatible();
        testParseCompatibleWithPatch();
        testParseLatest();
        testParseLatestCaseInsensitive();
        testParseTrimsWhitespace();
        testParseErrorNull();
        testParseErrorInvalidFormat();
        testParseErrorCompatibleTooShort();
        testExactMatch();
        testExactMismatch();
        testPatchStreamMatch();
        testPatchStreamMismatchMinor();
        testPatchStreamMismatchMajor();
        testMajorStreamMatch();
        testMajorStreamMismatch();
        testCompatibleMatch();
        testCompatibleMatchHigherMinor();
        testCompatibleMismatchLowerMinor();
        testCompatibleMismatchHigherMajor();
        testLatestMatchesEverything();
        testTypeAccessors();
        testEquality();

        Assert.summary();
        if (!Assert.allPassed()) System.exit(1);
    }

    static void testParseExact() {
        Assert.setCurrentTest("testParseExact");
        VersionSpec spec = VersionSpec.parse("2.1.5");
        Assert.equal(Type.EXACT, spec.type());
        Assert.equal(2, spec.major());
        Assert.equal(1, spec.minor());
        Assert.equal(5, spec.patch());
    }

    static void testParsePatchStream() {
        Assert.setCurrentTest("testParsePatchStream");
        VersionSpec spec = VersionSpec.parse("2.1.*");
        Assert.equal(Type.PATCH_STREAM, spec.type());
        Assert.equal(2, spec.major());
        Assert.equal(1, spec.minor());
    }

    static void testParseMajorStream() {
        Assert.setCurrentTest("testParseMajorStream");
        VersionSpec spec = VersionSpec.parse("2.*");
        Assert.equal(Type.MAJOR_STREAM, spec.type());
        Assert.equal(2, spec.major());
    }

    static void testParseCompatible() {
        Assert.setCurrentTest("testParseCompatible");
        VersionSpec spec = VersionSpec.parse("^2.1");
        Assert.equal(Type.COMPATIBLE, spec.type());
        Assert.equal(2, spec.major());
        Assert.equal(1, spec.minor());
    }

    static void testParseCompatibleWithPatch() {
        Assert.setCurrentTest("testParseCompatibleWithPatch");
        VersionSpec spec = VersionSpec.parse("^2.1.3");
        Assert.equal(Type.COMPATIBLE, spec.type());
        Assert.equal(2, spec.major());
        Assert.equal(1, spec.minor());
        Assert.equal(3, spec.patch());
    }

    static void testParseLatest() {
        Assert.setCurrentTest("testParseLatest");
        VersionSpec spec = VersionSpec.parse("latest");
        Assert.equal(Type.LATEST, spec.type());
    }

    static void testParseLatestCaseInsensitive() {
        Assert.setCurrentTest("testParseLatestCaseInsensitive");
        VersionSpec spec = VersionSpec.parse("LATEST");
        Assert.equal(Type.LATEST, spec.type());
    }

    static void testParseTrimsWhitespace() {
        Assert.setCurrentTest("testParseTrimsWhitespace");
        VersionSpec spec = VersionSpec.parse("  2.0.0  ");
        Assert.equal(Type.EXACT, spec.type());
    }

    static void testParseErrorNull() {
        Assert.setCurrentTest("testParseErrorNull");
        Assert.throwsException(IllegalArgumentException.class, () -> VersionSpec.parse(null));
    }

    static void testParseErrorInvalidFormat() {
        Assert.setCurrentTest("testParseErrorInvalidFormat");
        Assert.throwsException(IllegalArgumentException.class, () -> VersionSpec.parse("not-a-version"));
    }

    static void testParseErrorCompatibleTooShort() {
        Assert.setCurrentTest("testParseErrorCompatibleTooShort");
        Assert.throwsException(IllegalArgumentException.class, () -> VersionSpec.parse("^"));
    }

    static void testExactMatch() {
        Assert.setCurrentTest("testExactMatch");
        VersionSpec spec = VersionSpec.parse("2.1.5");
        Assert.isTrue(spec.matches(2, 1, 5));
    }

    static void testExactMismatch() {
        Assert.setCurrentTest("testExactMismatch");
        VersionSpec spec = VersionSpec.parse("2.1.5");
        Assert.isFalse(spec.matches(2, 1, 6));
    }

    static void testPatchStreamMatch() {
        Assert.setCurrentTest("testPatchStreamMatch");
        VersionSpec spec = VersionSpec.parse("2.1.*");
        Assert.isTrue(spec.matches(2, 1, 0));
        Assert.isTrue(spec.matches(2, 1, 99));
    }

    static void testPatchStreamMismatchMinor() {
        Assert.setCurrentTest("testPatchStreamMismatchMinor");
        VersionSpec spec = VersionSpec.parse("2.1.*");
        Assert.isFalse(spec.matches(2, 2, 0));
    }

    static void testPatchStreamMismatchMajor() {
        Assert.setCurrentTest("testPatchStreamMismatchMajor");
        VersionSpec spec = VersionSpec.parse("2.1.*");
        Assert.isFalse(spec.matches(3, 1, 0));
    }

    static void testMajorStreamMatch() {
        Assert.setCurrentTest("testMajorStreamMatch");
        VersionSpec spec = VersionSpec.parse("2.*");
        Assert.isTrue(spec.matches(2, 0, 0));
        Assert.isTrue(spec.matches(2, 99, 99));
    }

    static void testMajorStreamMismatch() {
        Assert.setCurrentTest("testMajorStreamMismatch");
        VersionSpec spec = VersionSpec.parse("2.*");
        Assert.isFalse(spec.matches(3, 0, 0));
    }

    static void testCompatibleMatch() {
        Assert.setCurrentTest("testCompatibleMatch");
        VersionSpec spec = VersionSpec.parse("^2.1.0");
        Assert.isTrue(spec.matches(2, 1, 0));
        Assert.isTrue(spec.matches(2, 1, 5));
        Assert.isTrue(spec.matches(2, 2, 0));
    }

    static void testCompatibleMatchHigherMinor() {
        Assert.setCurrentTest("testCompatibleMatchHigherMinor");
        VersionSpec spec = VersionSpec.parse("^2.1");
        Assert.isTrue(spec.matches(2, 5, 0));
    }

    static void testCompatibleMismatchLowerMinor() {
        Assert.setCurrentTest("testCompatibleMismatchLowerMinor");
        VersionSpec spec = VersionSpec.parse("^2.5");
        Assert.isFalse(spec.matches(2, 4, 9));
    }

    static void testCompatibleMismatchHigherMajor() {
        Assert.setCurrentTest("testCompatibleMismatchHigherMajor");
        VersionSpec spec = VersionSpec.parse("^2.1");
        Assert.isFalse(spec.matches(3, 0, 0));
    }

    static void testLatestMatchesEverything() {
        Assert.setCurrentTest("testLatestMatchesEverything");
        VersionSpec spec = VersionSpec.parse("latest");
        Assert.isTrue(spec.matches(0, 0, 0));
        Assert.isTrue(spec.matches(99, 99, 99));
    }

    static void testTypeAccessors() {
        Assert.setCurrentTest("testTypeAccessors");
        VersionSpec spec = VersionSpec.parse("2.1.5");
        Assert.equal("2.1.5", spec.raw());
        Assert.equal(2, spec.major());
        Assert.equal(1, spec.minor());
        Assert.equal(5, spec.patch());
    }

    static void testEquality() {
        Assert.setCurrentTest("testEquality");
        VersionSpec a = VersionSpec.parse("2.1.5");
        Assert.equal("2.1.5", a.toString());
    }
}
