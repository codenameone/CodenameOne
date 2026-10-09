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
package com.codename1.compat.jdk;

import com.codename1.compat.testing.HeadlessImplementation;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/// The properties the JDK never answers null for are not null here either.
public class SystemPropertiesTest {

    @BeforeClass
    public static void startCodenameOne() {
        HeadlessImplementation.install();
    }

    /// `System.getProperty("os.arch").toUpperCase().contains("ARM")` is how a
    /// desktop application asks whether it is on an embedded board.
    @Test
    public void theArchitectureIsAnsweredAndClaimsNoProcessor() {
        assertEquals("unknown", JdkSystem.getProperty("os.arch"));
        assertFalse(JdkSystem.getProperty("os.arch").toUpperCase().contains("ARM"));
    }

    @Test
    public void aKeyNothingKnowsIsStillNull() {
        assertNull(JdkSystem.getProperty("no.such.property.anywhere"));
        assertEquals("fallback", JdkSystem.getProperty("no.such.property.anywhere", "fallback"));
    }

    @Test
    public void theHomeDirectoryHasNoTrailingSeparator() {
        assertEquals("file:///home", JdkSystem.getProperty("user.home"));
    }
}
