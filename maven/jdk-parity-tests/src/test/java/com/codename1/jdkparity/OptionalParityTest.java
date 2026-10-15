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

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The relocate-and-run mechanism, proven on one class: JavaAPI's Optional,
 * renamed to {@code jdkparity.java.util.Optional}, runs on this JVM beside the
 * JDK's and answers the same.
 */
class OptionalParityTest {

    @Test
    void relocatedClassIsJavaApis() {
        // Not the JDK class under another name: it was loaded from this module.
        assertNotEquals(java.util.Optional.class.getName(), jdkparity.java.util.Optional.class.getName());
        assertEquals("jdkparity.java.util.Optional", jdkparity.java.util.Optional.class.getName());
    }

    @Test
    void presentValue() {
        java.util.Optional<String> jdk = java.util.Optional.of("cn1");
        jdkparity.java.util.Optional<String> api = jdkparity.java.util.Optional.of("cn1");
        assertEquals(jdk.isPresent(), api.isPresent());
        assertEquals(jdk.get(), api.get());
        assertEquals(jdk.orElse("x"), api.orElse("x"));
        assertEquals(jdk.map(String::length).get(), api.map(String::length).get());
        assertEquals(jdk.filter(s -> s.startsWith("c")).isPresent(), api.filter(s -> s.startsWith("c")).isPresent());
        assertEquals(jdk.filter(s -> s.startsWith("z")).isPresent(), api.filter(s -> s.startsWith("z")).isPresent());
        assertEquals(jdk.hashCode(), api.hashCode());
        assertEquals(jdk.toString(), api.toString());
        assertEquals(api, jdkparity.java.util.Optional.of("cn1"));
    }

    @Test
    void emptyValue() {
        java.util.Optional<String> jdk = java.util.Optional.ofNullable(null);
        jdkparity.java.util.Optional<String> api = jdkparity.java.util.Optional.ofNullable(null);
        assertEquals(jdk.isPresent(), api.isPresent());
        assertEquals(jdk.orElse("x"), api.orElse("x"));
        assertEquals(jdk.orElseGet(() -> "y"), api.orElseGet(() -> "y"));
        assertEquals(jdk.hashCode(), api.hashCode());
        assertEquals(jdk.toString(), api.toString());
        assertEquals(api, jdkparity.java.util.Optional.empty());
        assertThrows(NoSuchElementException.class, jdk::get);
        assertThrows(NoSuchElementException.class, api::get);
        assertThrows(NullPointerException.class, () -> java.util.Optional.of(null));
        assertThrows(NullPointerException.class, () -> jdkparity.java.util.Optional.of(null));
    }
}
