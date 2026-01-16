package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.CN;

public class BytecodeTranslatorRegressionTest extends BaseTest {
    @FunctionalInterface
    private interface Mapper {
        String apply(String input);

        default String decorate(String input) {
            return "<" + input + ">";
        }

        static String suffix(String input, String suffix) {
            return input + suffix;
        }
    }

    private static final class Prefixer {
        private final String prefix;

        private Prefixer(String prefix) {
            this.prefix = prefix;
        }

        private String applyPrefix(String input) {
            return prefix + input;
        }
    }

    @Override
    public boolean runTest() {
        Thread worker = new Thread(() -> {
            try {
                String[] inputs = {"alpha", "beta", "gamma"};
                Prefixer prefixer = new Prefixer("pre-");
                Mapper upper = String::toUpperCase;
                Mapper prefixed = prefixer::applyPrefix;
                String suffix = "-suffix";
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < 250; i++) {
                    for (String input : inputs) {
                        builder.append(combine(input, upper, prefixed, suffix));
                        builder.append('|');
                    }
                }
                String sample = combine("alpha", upper, prefixed, suffix);
                String expected = "<ALPHA>-suffix|<pre-alpha>-suffix";
                if (!expected.equals(sample)) {
                    fail("Unexpected output: " + sample);
                    return;
                }
                if (builder.length() == 0) {
                    fail("No output generated.");
                    return;
                }
                CN.callSerially(this::done);
            } catch (Throwable t) {
                fail("Regression test failed: " + t);
            }
        }, "cn1-bytecode-regression");
        worker.start();
        return true;
    }

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    private static String combine(String input, Mapper first, Mapper second, String suffix) {
        String left = Mapper.suffix(first.decorate(first.apply(input)), suffix);
        String right = Mapper.suffix(second.decorate(second.apply(input)), suffix);
        return left + "|" + right;
    }
}
