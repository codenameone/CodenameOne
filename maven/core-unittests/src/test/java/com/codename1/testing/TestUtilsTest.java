package com.codename1.testing;

import com.codename1.test.UITestBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TestUtils class.
 * Tests focus on assertion methods and basic utilities.
 * Note: Tests that require showing forms or complex UI interactions are excluded
 * as they require full application context and would make the tests brittle.
 */
class TestUtilsTest extends UITestBase {

    // Note: Verbose mode, wait, and logging tests are excluded as they use Log internally
    // which requires full framework initialization

    // Assertion tests - Basic
    @Test
    void assertBoolPassesOnTrue() {
        assertDoesNotThrow(() -> TestUtils.assertBool(true));
    }

    @Test
    void assertBoolFailsOnFalse() {
        assertThrows(RuntimeException.class, () -> TestUtils.assertBool(false));
    }

    @Test
    void assertBoolWithMessageFailsWithMessage() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> TestUtils.assertBool(false, "Custom error"));
        assertEquals("Custom error", exception.getMessage());
    }

    @Test
    void failAlwaysFails() {
        assertThrows(RuntimeException.class, () -> TestUtils.fail());
    }

    @Test
    void failWithMessageFailsWithMessage() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> TestUtils.fail("Failure message"));
        assertEquals("Failure message", exception.getMessage());
    }

    @Test
    void assertTruePassesOnTrue() {
        assertDoesNotThrow(() -> TestUtils.assertTrue(true));
    }

    @Test
    void assertTrueFailsOnFalse() {
        assertThrows(RuntimeException.class, () -> TestUtils.assertTrue(false));
    }

    @Test
    void assertFalsePassesOnFalse() {
        assertDoesNotThrow(() -> TestUtils.assertFalse(false));
    }

    @Test
    void assertFalseFailsOnTrue() {
        assertThrows(RuntimeException.class, () -> TestUtils.assertFalse(true));
    }

    // Null assertions
    @Test
    void assertNullPassesOnNull() {
        assertDoesNotThrow(() -> TestUtils.assertNull(null));
    }

    @Test
    void assertNullFailsOnNonNull() {
        assertThrows(RuntimeException.class, () -> TestUtils.assertNull("not null"));
    }

    @Test
    void assertNotNullPassesOnNonNull() {
        assertDoesNotThrow(() -> TestUtils.assertNotNull("not null"));
    }

    @Test
    void assertNotNullFailsOnNull() {
        assertThrows(RuntimeException.class, () -> TestUtils.assertNotNull(null));
    }

    // Same/Not Same assertions
    @Test
    void assertSamePassesOnSameObject() {
        Object obj = new Object();
        assertDoesNotThrow(() -> TestUtils.assertSame(obj, obj));
    }

    @Test
    void assertSameFailsOnDifferentObjects() {
        assertThrows(RuntimeException.class, () -> TestUtils.assertSame(new Object(), new Object()));
    }

    @Test
    void assertNotSamePassesOnDifferentObjects() {
        assertDoesNotThrow(() -> TestUtils.assertNotSame(new Object(), new Object()));
    }

    @Test
    void assertNotSameFailsOnSameObject() {
        Object obj = new Object();
        assertThrows(RuntimeException.class, () -> TestUtils.assertNotSame(obj, obj));
    }

    // Primitive equality tests
    @Test
    void assertEqualPassesForEqualBytes() {
        assertDoesNotThrow(() -> TestUtils.assertEqual((byte) 5, (byte) 5));
    }

    @Test
    void assertEqualFailsForUnequalBytes() {
        assertThrows(RuntimeException.class, () -> TestUtils.assertEqual((byte) 5, (byte) 6));
    }

    @Test
    void assertEqualPassesForEqualShorts() {
        assertDoesNotThrow(() -> TestUtils.assertEqual((short) 100, (short) 100));
    }

    @Test
    void assertEqualFailsForUnequalShorts() {
        assertThrows(RuntimeException.class, () -> TestUtils.assertEqual((short) 100, (short) 101));
    }

    @Test
    void assertEqualPassesForEqualInts() {
        assertDoesNotThrow(() -> TestUtils.assertEqual(42, 42));
    }

    @Test
    void assertEqualFailsForUnequalInts() {
        assertThrows(RuntimeException.class, () -> TestUtils.assertEqual(42, 43));
    }

    @Test
    void assertEqualPassesForEqualLongs() {
        assertDoesNotThrow(() -> TestUtils.assertEqual(1000L, 1000L));
    }

    @Test
    void assertEqualFailsForUnequalLongs() {
        assertThrows(RuntimeException.class, () -> TestUtils.assertEqual(1000L, 1001L));
    }

    // Float/Double equality with tolerance
    @Test
    void assertEqualPassesForEqualFloatsWithinTolerance() {
        assertDoesNotThrow(() -> TestUtils.assertEqual(1.0f, 1.01f, 2.0));
    }

    @Test
    void assertEqualFailsForFloatsOutsideTolerance() {
        assertThrows(RuntimeException.class, () -> TestUtils.assertEqual(1.0f, 2.0f, 0.1));
    }

    @Test
    void assertEqualPassesForEqualDoublesWithinTolerance() {
        assertDoesNotThrow(() -> TestUtils.assertEqual(1.0, 1.01, 2.0));
    }

    @Test
    void assertEqualFailsForDoublesOutsideTolerance() {
        assertThrows(RuntimeException.class, () -> TestUtils.assertEqual(1.0, 2.0, 0.1));
    }

    @Test
    void assertRangePassesForValuesWithinAbsoluteError() {
        assertDoesNotThrow(() -> TestUtils.assertRange(10.0, 10.5, 1.0));
    }

    @Test
    void assertRangeFailsForValuesOutsideAbsoluteError() {
        assertThrows(RuntimeException.class, () -> TestUtils.assertRange(10.0, 15.0, 1.0));
    }

    // Object equality
    @Test
    void assertEqualPassesForEqualObjects() {
        assertDoesNotThrow(() -> TestUtils.assertEqual("test", "test"));
    }

    @Test
    void assertEqualFailsForUnequalObjects() {
        assertThrows(RuntimeException.class, () -> TestUtils.assertEqual("test", "other"));
    }

    @Test
    void assertEqualHandlesNullObjects() {
        assertDoesNotThrow(() -> TestUtils.assertEqual(null, null));
    }

    // Not equal tests
    @Test
    void assertNotEqualPassesForUnequalInts() {
        assertDoesNotThrow(() -> TestUtils.assertNotEqual(5, 6));
    }

    @Test
    void assertNotEqualFailsForEqualInts() {
        assertThrows(RuntimeException.class, () -> TestUtils.assertNotEqual(5, 5));
    }

    @Test
    void assertNotEqualPassesForUnequalObjects() {
        assertDoesNotThrow(() -> TestUtils.assertNotEqual("test", "other"));
    }

    @Test
    void assertNotEqualFailsForEqualObjects() {
        assertThrows(RuntimeException.class, () -> TestUtils.assertNotEqual("test", "test"));
    }

    // Array equality tests
    @Test
    void assertArrayEqualPassesForEqualByteArrays() {
        byte[] arr1 = {1, 2, 3};
        byte[] arr2 = {1, 2, 3};
        assertDoesNotThrow(() -> TestUtils.assertArrayEqual(arr1, arr2));
    }

    @Test
    void assertArrayEqualFailsForUnequalByteArrays() {
        byte[] arr1 = {1, 2, 3};
        byte[] arr2 = {1, 2, 4};
        assertThrows(RuntimeException.class, () -> TestUtils.assertArrayEqual(arr1, arr2));
    }

    @Test
    void assertArrayEqualFailsForDifferentLengthArrays() {
        byte[] arr1 = {1, 2, 3};
        byte[] arr2 = {1, 2};
        assertThrows(RuntimeException.class, () -> TestUtils.assertArrayEqual(arr1, arr2));
    }

    @Test
    void assertArrayEqualPassesForEqualIntArrays() {
        int[] arr1 = {10, 20, 30};
        int[] arr2 = {10, 20, 30};
        assertDoesNotThrow(() -> TestUtils.assertArrayEqual(arr1, arr2));
    }

    @Test
    void assertArrayEqualPassesForEqualObjectArrays() {
        String[] arr1 = {"a", "b", "c"};
        String[] arr2 = {"a", "b", "c"};
        assertDoesNotThrow(() -> TestUtils.assertArrayEqual(arr1, arr2));
    }

    // Exception assertions
    @Test
    void assertExceptionPassesWhenExpectedExceptionThrown() {
        assertDoesNotThrow(() ->
                TestUtils.assertException(new RuntimeException(), () -> {
                    throw new RuntimeException();
                })
        );
    }

    @Test
    void assertExceptionFailsWhenNoExceptionThrown() {
        assertThrows(RuntimeException.class, () ->
                TestUtils.assertException(new RuntimeException(), () -> {
                    // No exception
                })
        );
    }

    @Test
    void assertExceptionFailsWhenWrongExceptionThrown() {
        assertThrows(RuntimeException.class, () ->
                TestUtils.assertException(new RuntimeException(), () -> {
                    throw new IllegalArgumentException();
                })
        );
    }

    @Test
    void assertNoExceptionPassesWhenNoExceptionThrown() {
        assertDoesNotThrow(() ->
                TestUtils.assertNoException(() -> {
                    // No exception
                })
        );
    }

    @Test
    void assertNoExceptionFailsWhenExceptionThrown() {
        assertThrows(RuntimeException.class, () ->
                TestUtils.assertNoException(() -> {
                    throw new RuntimeException();
                })
        );
    }
}
