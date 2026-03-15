package com.codenameone.examples.hellocodenameone.tests;

import java.util.Arrays;
import java.util.List;
import java.util.function.BinaryOperator;
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
