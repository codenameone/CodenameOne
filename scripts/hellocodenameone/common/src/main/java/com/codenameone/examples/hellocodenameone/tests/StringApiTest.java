package com.codenameone.examples.hellocodenameone.tests;

/**
 * End-to-end coverage for the String regex/literal helpers that Codename One
 * routes through the bytecode rewriter (replaceAll/replaceFirst) and the
 * literal CharSequence overload that lives directly on the String API
 * (replace(CharSequence, CharSequence)). Exercises the call sites on the
 * device so we catch any platform-specific divergence on iOS, Android and
 * JavaScript.
 */
public class StringApiTest extends BaseTest {

    @Override
    public boolean runTest() {
        try {
            // String.replace(CharSequence, CharSequence) - literal substitution
            assertEqual("ba", "aaa".replace("aa", "b"), "replace(aa,b) on aaa should be ba (left-to-right)");
            assertEqual("hello world", "hello there".replace("there", "world"), "single token replacement failed");
            assertEqual("X-X-X", "a.a.a".replace("a", "X"), "all-occurrence char-sequence replace failed");
            assertEqual("abc", "abc".replace("z", "Q"), "replace with no match should return original");
            assertEqual("xayaza", "aaa".replace("", "x"), "empty-target replace should interleave replacement");
            CharSequence target = new StringBuilder("a");
            CharSequence repl = new StringBuilder("XY");
            assertEqual("XYbXY", "aba".replace(target, repl), "non-String CharSequence overload failed");

            // String.replaceAll - regex-driven substitution via JdkApiRewriteHelper -> RE
            assertEqual("XbXcX", "aabacaa".replaceAll("a+", "X"), "replaceAll greedy + quantifier failed");
            assertEqual("xByB", "aBaB".replaceAll("a", "x"), "replaceAll literal token failed");
            assertEqual("ABC", "abc".replaceAll("[a-z]", "$0").toUpperCase(),
                    "replaceAll with $0 backreference / toUpperCase pipeline failed");
            assertEqual("--", "ab".replaceAll(".", "-"), "replaceAll '.' should match every character");
            assertEqual("nochange", "nochange".replaceAll("zzz", "X"),
                    "replaceAll with no matches should return original");

            // String.replaceFirst - regex-driven first-only substitution
            assertEqual("XbacaaB", "aabacaaB".replaceFirst("a+", "X"),
                    "replaceFirst should only replace the first regex match");
            assertEqual("xbab", "abab".replaceFirst("a", "x"),
                    "replaceFirst literal token failed");
            assertEqual("nochange", "nochange".replaceFirst("zzz", "X"),
                    "replaceFirst with no match should return original");
        } catch (Throwable t) {
            fail("String API test failed: " + t);
            return false;
        }
        done();
        return true;
    }

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }
}
