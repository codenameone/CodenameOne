package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.CN;

public class BytecodeTranslatorRegressionTest extends BaseTest {
    private interface Mapper {
        String apply(String input);

        default String decorate(String input) {
            return "<" + input + ">";
        }

        default Mapper andThen(Mapper next) {
            return value -> next.apply(this.apply(value));
        }

        static String suffix(String input, String suffix) {
            return input + suffix;
        }
    }

    private interface Factory {
        Prefixer create(String prefix);
    }

    private interface Combiner {
        String combine(String left, String right);
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
                Factory factory = Prefixer::new;
                Prefixer prefixer = factory.create("pre-");
                Prefixer otherPrefixer = factory.create("other-");
                Mapper upper = String::toUpperCase;
                Mapper prefixed = prefixer::applyPrefix;
                Mapper otherPrefixed = otherPrefixer::applyPrefix;
                Mapper reversed = new Mapper() {
                    @Override
                    public String apply(String input) {
                        return new StringBuilder(input).reverse().toString();
                    }
                };
                Mapper pipeline = upper.andThen(reversed).andThen(otherPrefixed);
                Combiner combiner = BytecodeTranslatorRegressionTest::join;
                String suffix = "-suffix";
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < 250; i++) {
                    for (String input : inputs) {
                        builder.append(combine(input, prefixed, pipeline, suffix, combiner));
                        builder.append('|');
                    }
                }
                String sample = combine("alpha", prefixed, pipeline, suffix, combiner);
                String expected = "<pre-alpha>-suffix|<other-AHPLA>-suffix";
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

    private static String combine(String input, Mapper first, Mapper second, String suffix, Combiner combiner) {
        String left = Mapper.suffix(first.decorate(first.apply(input)), suffix);
        String right = Mapper.suffix(second.decorate(second.apply(input)), suffix);
        return combiner.combine(left, right);
    }

    private static String join(String left, String right) {
        return left + "|" + right;
    }
}
