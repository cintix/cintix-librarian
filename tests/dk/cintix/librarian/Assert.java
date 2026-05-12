package dk.cintix.librarian;

import java.util.List;
import java.util.Objects;

public final class Assert {
    private static int passed = 0;
    private static int failed = 0;
    private static String currentTest;

    private Assert() {}

    public static void setCurrentTest(String name) { currentTest = name; }

    public static void equal(Object expected, Object actual) {
        if (Objects.equals(expected, actual)) {
            passed++;
        } else {
            fail("Expected: " + expected + ", but was: " + actual);
        }
    }

    public static void equal(int expected, int actual) { equal((Object) expected, actual); }
    public static void equal(long expected, long actual) { equal((Object) expected, actual); }

    public static void equal(double expected, double actual, double delta) {
        if (Math.abs(expected - actual) < delta) {
            passed++;
        } else {
            fail("Expected: " + expected + ", but was: " + actual + " (delta: " + delta + ")");
        }
    }

    public static void isTrue(boolean condition) {
        if (condition) passed++;
        else fail("Expected true, but was false");
    }

    public static void isFalse(boolean condition) {
        if (!condition) passed++;
        else fail("Expected false, but was true");
    }

    public static void notNull(Object obj) {
        if (obj != null) passed++;
        else fail("Expected non-null, but was null");
    }

    public static void isNull(Object obj) {
        if (obj == null) passed++;
        else fail("Expected null, but was: " + obj);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Throwable> void throwsException(Class<T> expectedType, Runnable runnable) {
        try {
            runnable.run();
            fail("Expected exception " + expectedType.getSimpleName() + " but none thrown");
        } catch (Throwable e) {
            if (expectedType.isInstance(e)) {
                passed++;
            } else {
                fail("Expected exception " + expectedType.getSimpleName() + " but got " + e.getClass().getSimpleName());
            }
        }
    }

    public static void contains(String haystack, String needle) {
        if (haystack != null && haystack.contains(needle)) passed++;
        else fail("Expected \"" + haystack + "\" to contain \"" + needle + "\"");
    }

    public static <T> void listContains(List<T> list, T element) {
        if (list.contains(element)) passed++;
        else fail("Expected list to contain " + element);
    }

    public static <T> void listSize(int expected, List<T> list) {
        equal(expected, list.size());
    }

    private static void fail(String message) {
        failed++;
        System.out.println("  FAIL [" + currentTest + "] " + message);
    }

    public static int passed() { return passed; }
    public static int failed() { return failed; }

    public static void reset() {
        passed = 0;
        failed = 0;
        currentTest = null;
    }

    public static void summary() {
        int total = passed + failed;
        System.out.println("\n  " + total + "/" + total + " passed");
        if (failed == 0) {
            System.out.println("  All tests passed.");
        } else {
            System.out.println("  " + failed + " test(s) FAILED");
        }
    }

    public static boolean allPassed() { return failed == 0; }
}
