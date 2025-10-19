package com.codename1.compat.java.util;

import org.junit.jupiter.api.Test;

import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ObjectsTest {
    @Test
    public void testEqualsHandlesNulls() {
        assertTrue(Objects.equals(null, null));
        assertFalse(Objects.equals(null, "a"));
        assertTrue(Objects.equals("test", "test"));
    }

    @Test
    public void testHashCodeHandlesNull() {
        assertEquals(0, Objects.hashCode(null));
        assertEquals("value".hashCode(), Objects.hashCode("value"));
    }

    @Test
    public void testToStringWithDefault() {
        assertEquals("null", Objects.toString(null));
        assertEquals("fallback", Objects.toString(null, "fallback"));
        assertEquals("42", Objects.toString(42, "fallback"));
    }

    @Test
    public void testCompareUsesComparator() {
        Comparator<Integer> comparator = Integer::compare;
        assertEquals(0, Objects.compare(null, null, comparator));
        assertTrue(Objects.compare(1, 2, comparator) < 0);
    }

    @Test
    public void testRequireNonNullThrows() {
        assertEquals("", Objects.requireNonNull("", "ignored"));
        NullPointerException npe = assertThrows(NullPointerException.class, () -> Objects.requireNonNull(null, "fail"));
        assertEquals("fail", npe.getMessage());
    }

    @Test
    public void testNonNull() {
        assertTrue(Objects.nonNull("value"));
        assertFalse(Objects.nonNull(null));
    }

    @Test
    public void testDeepEqualsHandlesArrayTypes() {
        assertTrue(Objects.deepEquals(new int[]{1, 2}, new int[]{1, 2}));
        assertFalse(Objects.deepEquals(new int[]{1}, new int[]{2}));
        assertTrue(Objects.deepEquals(new Object[]{new int[]{1}}, new Object[]{new int[]{1}}));
        assertFalse(Objects.deepEquals(new Object[]{new int[]{1}}, new Object[]{new int[]{2}}));
        assertFalse(Objects.deepEquals(new int[]{1}, new double[]{1}));
    }

    @Test
    public void testHashVarArgsMatchesArraysHashCode() {
        int expected = java.util.Arrays.hashCode(new Object[]{"a", 1});
        assertEquals(expected, Objects.hash("a", 1));
    }
}
