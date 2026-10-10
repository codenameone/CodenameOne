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

import java.io.FileNotFoundException;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/// A relative path, which is what a desktop application opens its settings
/// file with, resolves under the application's home.
public class RelativeFileTest {

    @BeforeClass
    public static void startCodenameOne() {
        HeadlessImplementation.install();
    }

    @Test
    public void aRelativePathResolvesUnderTheApplicationHome() {
        assertEquals("file:///home/game2048_4_record.properties",
                new File("game2048_4_record.properties").storagePath());
        assertEquals("file:///etc/hosts", new File("/etc/hosts").storagePath());
    }

    /// An application that reads its record on start, and treats a missing
    /// one as "no record yet", must get the exception it catches.
    @Test
    public void readingAMissingRelativeFileIsFileNotFound() throws Exception {
        assertFalse(new File("no-such-record.properties").isFile());
        try {
            new FileReader("no-such-record.properties").close();
            fail("a file that was never written was opened");
        } catch (FileNotFoundException expected) {
            assertEquals("no-such-record.properties (No such file or directory)", expected.getMessage());
        }
    }
}
