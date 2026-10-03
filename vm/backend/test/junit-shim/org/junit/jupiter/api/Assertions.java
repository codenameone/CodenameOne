/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package org.junit.jupiter.api;

import java.util.Iterator;
import java.util.function.Supplier;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.opentest4j.AssertionFailedError;

/// Assertions for compiled backend tests.
///
/// Part of the subset of JUnit 5's API that a compiled backend test translates
/// against; see `Test` for why it exists. The messages follow JUnit's shape --
/// `message ==> expected: <a> but was: <b>` -- so a report reads the same from
/// either runner.
public final class Assertions {
    private Assertions() {
    }

    // ------------------------------------------------------------------ fail

    public static <V> V fail() {
        throw new AssertionFailedError();
    }

    public static <V> V fail(String message) {
        throw new AssertionFailedError(message);
    }

    public static <V> V fail(String message, Throwable cause) {
        throw new AssertionFailedError(message, cause);
    }

    public static <V> V fail(Throwable cause) {
        throw new AssertionFailedError(cause == null ? null : cause.toString(), cause);
    }

    public static <V> V fail(Supplier<String> message) {
        throw new AssertionFailedError(text(message));
    }

    // ------------------------------------------------------------ true/false

    public static void assertTrue(boolean condition) {
        assertTrue(condition, (String) null);
    }

    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionFailedError(prefix(message) + "expected: <true> but was: <false>",
                    Boolean.TRUE, Boolean.FALSE);
        }
    }

    public static void assertTrue(boolean condition, Supplier<String> message) {
        if (!condition) {
            assertTrue(false, text(message));
        }
    }

    public static void assertFalse(boolean condition) {
        assertFalse(condition, (String) null);
    }

    public static void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionFailedError(prefix(message) + "expected: <false> but was: <true>",
                    Boolean.FALSE, Boolean.TRUE);
        }
    }

    public static void assertFalse(boolean condition, Supplier<String> message) {
        if (condition) {
            assertFalse(true, text(message));
        }
    }

    // ------------------------------------------------------------------ null

    public static void assertNull(Object actual) {
        assertNull(actual, (String) null);
    }

    public static void assertNull(Object actual, String message) {
        if (actual != null) {
            throw new AssertionFailedError(prefix(message) + "expected: <null> but was: <"
                    + actual + ">", null, actual);
        }
    }

    public static void assertNull(Object actual, Supplier<String> message) {
        if (actual != null) {
            assertNull(actual, text(message));
        }
    }

    public static void assertNotNull(Object actual) {
        assertNotNull(actual, (String) null);
    }

    public static void assertNotNull(Object actual, String message) {
        if (actual == null) {
            throw new AssertionFailedError(prefix(message) + "expected: not <null>");
        }
    }

    public static void assertNotNull(Object actual, Supplier<String> message) {
        if (actual == null) {
            assertNotNull(null, text(message));
        }
    }

    // ---------------------------------------------------------------- equals

    public static void assertEquals(Object expected, Object actual) {
        assertEquals(expected, actual, (String) null);
    }

    public static void assertEquals(Object expected, Object actual, String message) {
        if (!same(expected, actual)) {
            notEqual(expected, actual, message);
        }
    }

    public static void assertEquals(Object expected, Object actual, Supplier<String> message) {
        if (!same(expected, actual)) {
            notEqual(expected, actual, text(message));
        }
    }

    public static void assertEquals(byte expected, byte actual) {
        assertEquals(expected, actual, (String) null);
    }

    public static void assertEquals(byte expected, byte actual, String message) {
        if (expected != actual) {
            notEqual(Byte.valueOf(expected), Byte.valueOf(actual), message);
        }
    }

    public static void assertEquals(byte expected, byte actual, Supplier<String> message) {
        if (expected != actual) {
            notEqual(Byte.valueOf(expected), Byte.valueOf(actual), text(message));
        }
    }

    public static void assertEquals(short expected, short actual) {
        assertEquals(expected, actual, (String) null);
    }

    public static void assertEquals(short expected, short actual, String message) {
        if (expected != actual) {
            notEqual(Short.valueOf(expected), Short.valueOf(actual), message);
        }
    }

    public static void assertEquals(short expected, short actual, Supplier<String> message) {
        if (expected != actual) {
            notEqual(Short.valueOf(expected), Short.valueOf(actual), text(message));
        }
    }

    public static void assertEquals(int expected, int actual) {
        assertEquals(expected, actual, (String) null);
    }

    public static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            notEqual(Integer.valueOf(expected), Integer.valueOf(actual), message);
        }
    }

    public static void assertEquals(int expected, int actual, Supplier<String> message) {
        if (expected != actual) {
            notEqual(Integer.valueOf(expected), Integer.valueOf(actual), text(message));
        }
    }

    public static void assertEquals(long expected, long actual) {
        assertEquals(expected, actual, (String) null);
    }

    public static void assertEquals(long expected, long actual, String message) {
        if (expected != actual) {
            notEqual(Long.valueOf(expected), Long.valueOf(actual), message);
        }
    }

    public static void assertEquals(long expected, long actual, Supplier<String> message) {
        if (expected != actual) {
            notEqual(Long.valueOf(expected), Long.valueOf(actual), text(message));
        }
    }

    public static void assertEquals(char expected, char actual) {
        assertEquals(expected, actual, (String) null);
    }

    public static void assertEquals(char expected, char actual, String message) {
        if (expected != actual) {
            notEqual(Character.valueOf(expected), Character.valueOf(actual), message);
        }
    }

    public static void assertEquals(char expected, char actual, Supplier<String> message) {
        if (expected != actual) {
            notEqual(Character.valueOf(expected), Character.valueOf(actual), text(message));
        }
    }

    public static void assertEquals(float expected, float actual) {
        assertEquals(expected, actual, (String) null);
    }

    public static void assertEquals(float expected, float actual, String message) {
        // Box comparison, as JUnit does: NaN equals NaN, and 0.0 is not -0.0.
        if (!Float.valueOf(expected).equals(Float.valueOf(actual))) {
            notEqual(Float.valueOf(expected), Float.valueOf(actual), message);
        }
    }

    public static void assertEquals(float expected, float actual, Supplier<String> message) {
        if (!Float.valueOf(expected).equals(Float.valueOf(actual))) {
            notEqual(Float.valueOf(expected), Float.valueOf(actual), text(message));
        }
    }

    public static void assertEquals(float expected, float actual, float delta) {
        assertEquals(expected, actual, delta, (String) null);
    }

    public static void assertEquals(float expected, float actual, float delta, String message) {
        if (!Float.valueOf(expected).equals(Float.valueOf(actual))
                && !(Math.abs(expected - actual) <= delta)) {
            notEqual(Float.valueOf(expected), Float.valueOf(actual), message);
        }
    }

    public static void assertEquals(double expected, double actual) {
        assertEquals(expected, actual, (String) null);
    }

    public static void assertEquals(double expected, double actual, String message) {
        // Box comparison, as JUnit does: NaN equals NaN, and 0.0 is not -0.0.
        if (!Double.valueOf(expected).equals(Double.valueOf(actual))) {
            notEqual(Double.valueOf(expected), Double.valueOf(actual), message);
        }
    }

    public static void assertEquals(double expected, double actual, Supplier<String> message) {
        if (!Double.valueOf(expected).equals(Double.valueOf(actual))) {
            notEqual(Double.valueOf(expected), Double.valueOf(actual), text(message));
        }
    }

    public static void assertEquals(double expected, double actual, double delta) {
        assertEquals(expected, actual, delta, (String) null);
    }

    public static void assertEquals(double expected, double actual, double delta, String message) {
        if (!Double.valueOf(expected).equals(Double.valueOf(actual))
                && !(Math.abs(expected - actual) <= delta)) {
            notEqual(Double.valueOf(expected), Double.valueOf(actual), message);
        }
    }

    public static void assertNotEquals(Object unexpected, Object actual) {
        assertNotEquals(unexpected, actual, (String) null);
    }

    public static void assertNotEquals(Object unexpected, Object actual, String message) {
        if (same(unexpected, actual)) {
            throw new AssertionFailedError(prefix(message) + "expected: not equal but was: <"
                    + actual + ">");
        }
    }

    public static void assertNotEquals(long unexpected, long actual) {
        assertNotEquals(Long.valueOf(unexpected), Long.valueOf(actual), (String) null);
    }

    public static void assertNotEquals(long unexpected, long actual, String message) {
        assertNotEquals(Long.valueOf(unexpected), Long.valueOf(actual), message);
    }

    public static void assertNotEquals(double unexpected, double actual) {
        assertNotEquals(Double.valueOf(unexpected), Double.valueOf(actual), (String) null);
    }

    public static void assertNotEquals(double unexpected, double actual, String message) {
        assertNotEquals(Double.valueOf(unexpected), Double.valueOf(actual), message);
    }

    // ------------------------------------------------------------------ same

    public static void assertSame(Object expected, Object actual) {
        assertSame(expected, actual, (String) null);
    }

    public static void assertSame(Object expected, Object actual, String message) {
        if (expected != actual) {
            throw new AssertionFailedError(prefix(message) + "expected: same as <" + expected
                    + "> but was: <" + actual + ">", expected, actual);
        }
    }

    public static void assertNotSame(Object unexpected, Object actual) {
        assertNotSame(unexpected, actual, (String) null);
    }

    public static void assertNotSame(Object unexpected, Object actual, String message) {
        if (unexpected == actual) {
            throw new AssertionFailedError(prefix(message) + "expected: not same but was: <"
                    + actual + ">");
        }
    }

    // ---------------------------------------------------------------- arrays

    public static void assertArrayEquals(byte[] expected, byte[] actual) {
        assertArrayEquals(expected, actual, (String) null);
    }

    public static void assertArrayEquals(byte[] expected, byte[] actual, String message) {
        if (expected == actual) {
            return;
        }
        if (expected == null || actual == null) {
            notEqual(expected, actual, message);
        }
        if (expected.length != actual.length) {
            throw new AssertionFailedError(prefix(message) + "array lengths differ, expected: <"
                    + expected.length + "> but was: <" + actual.length + ">");
        }
        for (int iter = 0 ; iter < expected.length ; iter++) {
            if (!String.valueOf(expected[iter]).equals(String.valueOf(actual[iter]))) {
                throw new AssertionFailedError(prefix(message) + "array contents differ at index ["
                        + iter + "], expected: <" + expected[iter] + "> but was: <"
                        + actual[iter] + ">");
            }
        }
    }

    public static void assertArrayEquals(short[] expected, short[] actual) {
        assertArrayEquals(expected, actual, (String) null);
    }

    public static void assertArrayEquals(short[] expected, short[] actual, String message) {
        if (expected == actual) {
            return;
        }
        if (expected == null || actual == null) {
            notEqual(expected, actual, message);
        }
        if (expected.length != actual.length) {
            throw new AssertionFailedError(prefix(message) + "array lengths differ, expected: <"
                    + expected.length + "> but was: <" + actual.length + ">");
        }
        for (int iter = 0 ; iter < expected.length ; iter++) {
            if (!String.valueOf(expected[iter]).equals(String.valueOf(actual[iter]))) {
                throw new AssertionFailedError(prefix(message) + "array contents differ at index ["
                        + iter + "], expected: <" + expected[iter] + "> but was: <"
                        + actual[iter] + ">");
            }
        }
    }

    public static void assertArrayEquals(int[] expected, int[] actual) {
        assertArrayEquals(expected, actual, (String) null);
    }

    public static void assertArrayEquals(int[] expected, int[] actual, String message) {
        if (expected == actual) {
            return;
        }
        if (expected == null || actual == null) {
            notEqual(expected, actual, message);
        }
        if (expected.length != actual.length) {
            throw new AssertionFailedError(prefix(message) + "array lengths differ, expected: <"
                    + expected.length + "> but was: <" + actual.length + ">");
        }
        for (int iter = 0 ; iter < expected.length ; iter++) {
            if (!String.valueOf(expected[iter]).equals(String.valueOf(actual[iter]))) {
                throw new AssertionFailedError(prefix(message) + "array contents differ at index ["
                        + iter + "], expected: <" + expected[iter] + "> but was: <"
                        + actual[iter] + ">");
            }
        }
    }

    public static void assertArrayEquals(long[] expected, long[] actual) {
        assertArrayEquals(expected, actual, (String) null);
    }

    public static void assertArrayEquals(long[] expected, long[] actual, String message) {
        if (expected == actual) {
            return;
        }
        if (expected == null || actual == null) {
            notEqual(expected, actual, message);
        }
        if (expected.length != actual.length) {
            throw new AssertionFailedError(prefix(message) + "array lengths differ, expected: <"
                    + expected.length + "> but was: <" + actual.length + ">");
        }
        for (int iter = 0 ; iter < expected.length ; iter++) {
            if (!String.valueOf(expected[iter]).equals(String.valueOf(actual[iter]))) {
                throw new AssertionFailedError(prefix(message) + "array contents differ at index ["
                        + iter + "], expected: <" + expected[iter] + "> but was: <"
                        + actual[iter] + ">");
            }
        }
    }

    public static void assertArrayEquals(char[] expected, char[] actual) {
        assertArrayEquals(expected, actual, (String) null);
    }

    public static void assertArrayEquals(char[] expected, char[] actual, String message) {
        if (expected == actual) {
            return;
        }
        if (expected == null || actual == null) {
            notEqual(expected, actual, message);
        }
        if (expected.length != actual.length) {
            throw new AssertionFailedError(prefix(message) + "array lengths differ, expected: <"
                    + expected.length + "> but was: <" + actual.length + ">");
        }
        for (int iter = 0 ; iter < expected.length ; iter++) {
            if (!String.valueOf(expected[iter]).equals(String.valueOf(actual[iter]))) {
                throw new AssertionFailedError(prefix(message) + "array contents differ at index ["
                        + iter + "], expected: <" + expected[iter] + "> but was: <"
                        + actual[iter] + ">");
            }
        }
    }

    public static void assertArrayEquals(float[] expected, float[] actual) {
        assertArrayEquals(expected, actual, (String) null);
    }

    public static void assertArrayEquals(float[] expected, float[] actual, String message) {
        if (expected == actual) {
            return;
        }
        if (expected == null || actual == null) {
            notEqual(expected, actual, message);
        }
        if (expected.length != actual.length) {
            throw new AssertionFailedError(prefix(message) + "array lengths differ, expected: <"
                    + expected.length + "> but was: <" + actual.length + ">");
        }
        for (int iter = 0 ; iter < expected.length ; iter++) {
            if (!String.valueOf(expected[iter]).equals(String.valueOf(actual[iter]))) {
                throw new AssertionFailedError(prefix(message) + "array contents differ at index ["
                        + iter + "], expected: <" + expected[iter] + "> but was: <"
                        + actual[iter] + ">");
            }
        }
    }

    public static void assertArrayEquals(double[] expected, double[] actual) {
        assertArrayEquals(expected, actual, (String) null);
    }

    public static void assertArrayEquals(double[] expected, double[] actual, String message) {
        if (expected == actual) {
            return;
        }
        if (expected == null || actual == null) {
            notEqual(expected, actual, message);
        }
        if (expected.length != actual.length) {
            throw new AssertionFailedError(prefix(message) + "array lengths differ, expected: <"
                    + expected.length + "> but was: <" + actual.length + ">");
        }
        for (int iter = 0 ; iter < expected.length ; iter++) {
            if (!String.valueOf(expected[iter]).equals(String.valueOf(actual[iter]))) {
                throw new AssertionFailedError(prefix(message) + "array contents differ at index ["
                        + iter + "], expected: <" + expected[iter] + "> but was: <"
                        + actual[iter] + ">");
            }
        }
    }

    public static void assertArrayEquals(boolean[] expected, boolean[] actual) {
        assertArrayEquals(expected, actual, (String) null);
    }

    public static void assertArrayEquals(boolean[] expected, boolean[] actual, String message) {
        if (expected == actual) {
            return;
        }
        if (expected == null || actual == null) {
            notEqual(expected, actual, message);
        }
        if (expected.length != actual.length) {
            throw new AssertionFailedError(prefix(message) + "array lengths differ, expected: <"
                    + expected.length + "> but was: <" + actual.length + ">");
        }
        for (int iter = 0 ; iter < expected.length ; iter++) {
            if (!String.valueOf(expected[iter]).equals(String.valueOf(actual[iter]))) {
                throw new AssertionFailedError(prefix(message) + "array contents differ at index ["
                        + iter + "], expected: <" + expected[iter] + "> but was: <"
                        + actual[iter] + ">");
            }
        }
    }

    public static void assertArrayEquals(Object[] expected, Object[] actual) {
        assertArrayEquals(expected, actual, (String) null);
    }

    public static void assertArrayEquals(Object[] expected, Object[] actual, String message) {
        if (expected == actual) {
            return;
        }
        if (expected == null || actual == null) {
            notEqual(expected, actual, message);
        }
        if (expected.length != actual.length) {
            throw new AssertionFailedError(prefix(message) + "array lengths differ, expected: <"
                    + expected.length + "> but was: <" + actual.length + ">");
        }
        for (int iter = 0 ; iter < expected.length ; iter++) {
            Object e = expected[iter];
            Object a = actual[iter];
            if (e instanceof Object[] && a instanceof Object[]) {
                assertArrayEquals((Object[]) e, (Object[]) a, message);
            } else if (!nestedPrimitiveArrays(e, a, message) && !same(e, a)) {
                throw new AssertionFailedError(prefix(message) + "array contents differ at index ["
                        + iter + "], expected: <" + e + "> but was: <" + a + ">");
            }
        }
    }

    /// Compares two elements of an outer array that are primitive arrays of one
    /// type -- the rows of an `int[][]` -- as JUnit does, by content rather than
    /// identity; false when they are not, for the caller to compare otherwise.
    private static boolean nestedPrimitiveArrays(Object e, Object a, String message) {
        if (e instanceof int[] && a instanceof int[]) {
            assertArrayEquals((int[]) e, (int[]) a, message);
        } else if (e instanceof long[] && a instanceof long[]) {
            assertArrayEquals((long[]) e, (long[]) a, message);
        } else if (e instanceof byte[] && a instanceof byte[]) {
            assertArrayEquals((byte[]) e, (byte[]) a, message);
        } else if (e instanceof short[] && a instanceof short[]) {
            assertArrayEquals((short[]) e, (short[]) a, message);
        } else if (e instanceof char[] && a instanceof char[]) {
            assertArrayEquals((char[]) e, (char[]) a, message);
        } else if (e instanceof float[] && a instanceof float[]) {
            assertArrayEquals((float[]) e, (float[]) a, message);
        } else if (e instanceof double[] && a instanceof double[]) {
            assertArrayEquals((double[]) e, (double[]) a, message);
        } else if (e instanceof boolean[] && a instanceof boolean[]) {
            assertArrayEquals((boolean[]) e, (boolean[]) a, message);
        } else {
            return false;
        }
        return true;
    }

    public static void assertIterableEquals(Iterable<?> expected, Iterable<?> actual) {
        assertIterableEquals(expected, actual, (String) null);
    }

    public static void assertIterableEquals(Iterable<?> expected, Iterable<?> actual,
                                            String message) {
        if (expected == actual) {
            return;
        }
        if (expected == null || actual == null) {
            notEqual(expected, actual, message);
        }
        Iterator<?> e = expected.iterator();
        Iterator<?> a = actual.iterator();
        int index = 0;
        while (e.hasNext() && a.hasNext()) {
            Object x = e.next();
            Object y = a.next();
            if (x != y && x instanceof Iterable && y instanceof Iterable) {
                // Nested iterables compare by iteration, as JUnit's do: a List and
                // a Set holding the same elements in order are equal here.
                assertIterableEquals((Iterable<?>) x, (Iterable<?>) y, message);
            } else if (!same(x, y)) {
                throw new AssertionFailedError(prefix(message) + "iterable contents differ at index ["
                        + index + "], expected: <" + x + "> but was: <" + y + ">");
            }
            index++;
        }
        if (e.hasNext() || a.hasNext()) {
            throw new AssertionFailedError(prefix(message) + "iterable lengths differ at index ["
                    + index + "]");
        }
    }

    // ---------------------------------------------------------------- throws

    public static <T extends Throwable> T assertThrows(Class<T> expectedType, Executable executable) {
        return assertThrows(expectedType, executable, (String) null);
    }

    public static <T extends Throwable> T assertThrows(Class<T> expectedType, Executable executable,
                                                       String message) {
        try {
            executable.execute();
        } catch (Throwable actual) {
            if (expectedType.isInstance(actual)) {
                @SuppressWarnings("unchecked")
                T matched = (T) actual;
                return matched;
            }
            rethrowIfUnrecoverable(actual);
            throw new AssertionFailedError(prefix(message) + "Unexpected exception type thrown, "
                    + "expected: <" + expectedType.getName() + "> but was: <"
                    + actual.getClass().getName() + ">", actual);
        }
        throw new AssertionFailedError(prefix(message) + "Expected " + expectedType.getName()
                + " to be thrown, but nothing was thrown.");
    }

    public static <T extends Throwable> T assertThrows(Class<T> expectedType, Executable executable,
                                                       Supplier<String> message) {
        // The message is built only for a failure, as JUnit's is: a passing
        // assertion must not depend on the supplier, nor run its side effects.
        try {
            executable.execute();
        } catch (Throwable actual) {
            if (expectedType.isInstance(actual)) {
                @SuppressWarnings("unchecked")
                T matched = (T) actual;
                return matched;
            }
            rethrowIfUnrecoverable(actual);
            throw new AssertionFailedError(prefix(text(message)) + "Unexpected exception type "
                    + "thrown, expected: <" + expectedType.getName() + "> but was: <"
                    + actual.getClass().getName() + ">", actual);
        }
        throw new AssertionFailedError(prefix(text(message)) + "Expected " + expectedType.getName()
                + " to be thrown, but nothing was thrown.");
    }

    public static <T extends Throwable> T assertThrowsExactly(Class<T> expectedType,
                                                              Executable executable) {
        return assertThrowsExactly(expectedType, executable, (String) null);
    }

    public static <T extends Throwable> T assertThrowsExactly(Class<T> expectedType,
                                                              Executable executable, String message) {
        Throwable actual = null;
        try {
            executable.execute();
        } catch (Throwable thrown) {
            actual = thrown;
        }
        if (actual == null) {
            throw new AssertionFailedError(prefix(message) + "Expected " + expectedType.getName()
                    + " to be thrown, but nothing was thrown.");
        }
        if (actual.getClass() != expectedType) {
            rethrowIfUnrecoverable(actual);
            throw new AssertionFailedError(prefix(message) + "Unexpected exception type thrown, "
                    + "expected: <" + expectedType.getName() + "> but was: <"
                    + actual.getClass().getName() + ">", actual);
        }
        @SuppressWarnings("unchecked")
        T matched = (T) actual;
        return matched;
    }

    public static void assertDoesNotThrow(Executable executable) {
        assertDoesNotThrow(executable, (String) null);
    }

    public static void assertDoesNotThrow(Executable executable, String message) {
        try {
            executable.execute();
        } catch (Throwable thrown) {
            rethrowIfUnrecoverable(thrown);
            throw new AssertionFailedError(prefix(message) + "Unexpected exception thrown: "
                    + thrown, thrown);
        }
    }

    public static <T> T assertDoesNotThrow(ThrowingSupplier<T> supplier) {
        return assertDoesNotThrow(supplier, (String) null);
    }

    public static <T> T assertDoesNotThrow(ThrowingSupplier<T> supplier, String message) {
        try {
            return supplier.get();
        } catch (Throwable thrown) {
            rethrowIfUnrecoverable(thrown);
            throw new AssertionFailedError(prefix(message) + "Unexpected exception thrown: "
                    + thrown, thrown);
        }
    }

    public static <T> T assertInstanceOf(Class<T> expectedType, Object actual) {
        return assertInstanceOf(expectedType, actual, (String) null);
    }

    public static <T> T assertInstanceOf(Class<T> expectedType, Object actual, String message) {
        if (!expectedType.isInstance(actual)) {
            throw new AssertionFailedError(prefix(message) + "Unexpected type, expected: <"
                    + expectedType.getName() + "> but was: <"
                    + (actual == null ? "null" : actual.getClass().getName()) + ">");
        }
        @SuppressWarnings("unchecked")
        T matched = (T) actual;
        return matched;
    }

    // ------------------------------------------------------------------- all

    public static void assertAll(Executable... executables) {
        assertAll(null, executables);
    }

    public static void assertAll(String heading, Executable... executables) {
        StringBuilder failures = null;
        int count = 0;
        Throwable first = null;
        for (int iter = 0 ; iter < executables.length ; iter++) {
            try {
                executables[iter].execute();
            } catch (OutOfMemoryError unrecoverable) {
                // Rethrown at once, as JUnit does: running the rest after it is
                // unsafe and the report would hide what really failed.
                throw unrecoverable;
            } catch (Throwable thrown) {
                if (first == null) {
                    first = thrown;
                    failures = new StringBuilder();
                }
                count++;
                failures.append("\n\t").append(thrown.getMessage() == null ? thrown.toString()
                        : thrown.getMessage());
            }
        }
        if (first != null) {
            throw new AssertionFailedError((heading == null ? "Multiple Failures" : heading)
                    + " (" + count + " failure" + (count == 1 ? "" : "s") + ")" + failures, first);
        }
    }

    // --------------------------------------------------------------- helpers

    /// Rethrows an OutOfMemoryError unchanged, as JUnit's UnrecoverableExceptions
    /// does wherever an assertion catches what it runs: wrapping it would allocate
    /// while memory is gone and hide that the VM is compromised.
    private static void rethrowIfUnrecoverable(Throwable thrown) {
        if (thrown instanceof OutOfMemoryError) {
            throw (OutOfMemoryError) thrown;
        }
    }

    private static boolean same(Object expected, Object actual) {
        return expected == actual || (expected != null && expected.equals(actual));
    }

    private static void notEqual(Object expected, Object actual, String message) {
        String e = String.valueOf(expected);
        String a = String.valueOf(actual);
        if (e.equals(a) && expected != null && actual != null) {
            // Equal text, different types: say so, as JUnit does.
            e = expected.getClass().getName() + "@<" + e + ">";
            a = actual.getClass().getName() + "@<" + a + ">";
            throw new AssertionFailedError(prefix(message) + "expected: " + e + " but was: " + a,
                    expected, actual);
        }
        throw new AssertionFailedError(prefix(message) + "expected: <" + e + "> but was: <" + a
                + ">", expected, actual);
    }

    private static String prefix(String message) {
        return message == null || message.length() == 0 ? "" : message + " ==> ";
    }

    private static String text(Supplier<String> message) {
        return message == null ? null : message.get();
    }
}
