package com.codenameone.examples.hellocodenameone.tests;

import java.util.Arrays;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StreamApiTest extends BaseTest {

    @Override
    public boolean runTest() {
        try {
            List<String> values = Stream.of("alpha", "be", "gamma", "delta")
                    .filter(new Predicate<String>() {
                        public boolean test(String value) {
                            return value.length() > 3;
                        }
                    })
                    .map(new Function<String, String>() {
                        public String apply(String value) {
                            return value.toUpperCase();
                        }
                    })
                    .sorted()
                    .collect(Collectors.<String>toList());

            assertEqual(Arrays.asList(new String[]{"ALPHA", "DELTA", "GAMMA"}), values, "Unexpected mapped values");

            int reduced = Stream.of(Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3), Integer.valueOf(4))
                    .reduce(Integer.valueOf(0), new BinaryOperator<Integer>() {
                        public Integer apply(Integer left, Integer right) {
                            return Integer.valueOf(left.intValue() + right.intValue());
                        }
                    })
                    .intValue();

            assertEqual(10, reduced);

            String joined = Stream.of("A", "B", "C").collect(Collectors.joining());
            assertEqual("ABC", joined);

            assertTrue(Stream.of("aa", "bbb").anyMatch(new Predicate<String>() {
                public boolean test(String value) {
                    return value.length() == 3;
                }
            }));

            assertTrue(Stream.of("aa", "bbb").allMatch(new Predicate<String>() {
                public boolean test(String value) {
                    return value.length() >= 2;
                }
            }));

            assertTrue(Stream.of("aa", "bbb").noneMatch(new Predicate<String>() {
                public boolean test(String value) {
                    return value.length() == 1;
                }
            }));

            List<String> distinctSorted = Stream.of("delta", "beta", "alpha", "beta", "gamma", "alpha")
                    .distinct()
                    .sorted()
                    .skip(1)
                    .limit(2)
                    .collect(Collectors.<String>toList());
            assertEqual(Arrays.asList(new String[]{"beta", "delta"}), distinctSorted, "distinct/sorted/skip/limit pipeline failed");

            Object[] arrayResult = Stream.of("x", "y", "z").skip(2).toArray();
            assertEqual(1, arrayResult.length, "Unexpected toArray length after skip");
            assertEqual("z", arrayResult[0], "Unexpected toArray value after skip");

            long emptyCount = Stream.<String>empty().count();
            assertEqual(0L, emptyCount, "Stream.empty() should produce a stream with zero elements");

            long zeroLimitedCount = Stream.of(Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3)).limit(0).count();
            assertEqual(0L, zeroLimitedCount, "limit(0) should produce zero elements");

            long skippedPastEnd = Stream.of(Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3)).skip(99).count();
            assertEqual(0L, skippedPastEnd, "Skipping past the end should produce an empty stream");

            StringBuffer forEachOrder = new StringBuffer();
            Stream.of(Integer.valueOf(3), Integer.valueOf(1), Integer.valueOf(2)).sorted().forEach(new Consumer<Integer>() {
                public void accept(Integer value) {
                    forEachOrder.append(value.intValue());
                }
            });
            assertEqual("123", forEachOrder.toString(), "forEach should preserve sorted encounter order");

            assertTrue(!Stream.<String>empty().anyMatch(new Predicate<String>() {
                public boolean test(String value) {
                    return true;
                }
            }), "anyMatch on empty stream should be false");

            assertTrue(Stream.<String>empty().allMatch(new Predicate<String>() {
                public boolean test(String value) {
                    return false;
                }
            }), "allMatch on empty stream should be true");

            assertTrue(Stream.<String>empty().noneMatch(new Predicate<String>() {
                public boolean test(String value) {
                    return true;
                }
            }), "noneMatch on empty stream should be true");

            long distinctWithNullCount = Stream.of("x", "x", null, null, "y").distinct().count();
            assertEqual(3L, distinctWithNullCount, "distinct should keep one null and unique non-null values");
        } catch (Throwable t) {
            fail("Stream API test failed: " + t);
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
