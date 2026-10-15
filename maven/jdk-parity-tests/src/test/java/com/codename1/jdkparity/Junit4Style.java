/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.jdkparity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;

/**
 * JUnit 4's assertion signatures, message first, over JUnit 5.
 *
 * The differential tests here were written for the compat-jdk layer under
 * JUnit 4 and moved to this module with their assertions as they were; this
 * keeps a message argument in the place those tests put it.
 */
final class Junit4Style {
    private Junit4Style() {
    }

    static void assertEquals(Object expected, Object actual) {
        Assertions.assertEquals(expected, actual);
    }

    static void assertEquals(String message, Object expected, Object actual) {
        Assertions.assertEquals(expected, actual, message);
    }

    static void assertEquals(long expected, long actual) {
        Assertions.assertEquals(expected, actual);
    }

    static void assertEquals(String message, long expected, long actual) {
        Assertions.assertEquals(expected, actual, message);
    }

    static void assertNotEquals(Object unexpected, Object actual) {
        Assertions.assertNotEquals(unexpected, actual);
    }

    static void assertNotEquals(String message, Object unexpected, Object actual) {
        Assertions.assertNotEquals(unexpected, actual, message);
    }

    static void assertTrue(boolean condition) {
        Assertions.assertTrue(condition);
    }

    static void assertTrue(String message, boolean condition) {
        Assertions.assertTrue(condition, message);
    }

    static void assertFalse(boolean condition) {
        Assertions.assertFalse(condition);
    }

    static void assertFalse(String message, boolean condition) {
        Assertions.assertFalse(condition, message);
    }

    static void assertNull(Object actual) {
        Assertions.assertNull(actual);
    }

    static void assertNull(String message, Object actual) {
        Assertions.assertNull(actual, message);
    }

    static void assertNotNull(Object actual) {
        Assertions.assertNotNull(actual);
    }

    static void assertNotNull(String message, Object actual) {
        Assertions.assertNotNull(actual, message);
    }

    static void assertSame(Object expected, Object actual) {
        Assertions.assertSame(expected, actual);
    }

    static void assertSame(String message, Object expected, Object actual) {
        Assertions.assertSame(expected, actual, message);
    }

    static void assertNotSame(Object unexpected, Object actual) {
        Assertions.assertNotSame(unexpected, actual);
    }

    static void assertArrayEquals(Object[] expected, Object[] actual) {
        Assertions.assertArrayEquals(expected, actual);
    }

    static void assertArrayEquals(byte[] expected, byte[] actual) {
        Assertions.assertArrayEquals(expected, actual);
    }

    static void fail() {
        Assertions.fail();
    }

    static void fail(String message) {
        Assertions.fail(message);
    }

    /** Skips the rest of a test that compares with Java 8's behaviour on a later JDK. */
    static void assumeJava8(String changed) {
        Assumptions.assumeTrue(System.getProperty("java.specification.version").startsWith("1."),
                "After Java 8 the JDK " + changed + "; this comparison runs on Java 8 only");
    }
}
