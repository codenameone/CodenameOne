package com.codenameone.examples.hellocodenameone;

final class DefaultMethodDemo {
    private interface BaseFormatter {
        String base(String input);

        default String format(String input) {
            return "base:" + base(input);
        }
    }

    private interface ChildFormatter extends BaseFormatter {
        @Override
        default String format(String input) {
            return "child:" + BaseFormatter.super.format(input);
        }

        default String exclaim(String input) {
            return format(input).toUpperCase();
        }
    }

    private interface AlternateFormatter extends BaseFormatter {
        @Override
        default String format(String input) {
            return "alt:" + base(input);
        }
    }

    private DefaultMethodDemo() {
    }

    static void validate() {
        String result = run();
        String expected = "base:one|child:base:two|CHILD:BASE:TWO|alt:alt-three";
        if (!expected.equals(result)) {
            throw new IllegalStateException("Default method regression: " + result);
        }
    }

    private static String run() {
        BaseFormatter base = value -> value;
        ChildFormatter child = value -> value;
        AlternateFormatter alt = value -> "alt-" + value;
        String baseResult = base.format("one");
        String childResult = child.format("two");
        String childExclaim = child.exclaim("two");
        String altResult = alt.format("three");
        return baseResult + "|" + childResult + "|" + childExclaim + "|" + altResult;
    }
}
