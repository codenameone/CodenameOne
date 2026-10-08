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

import com.codename1.compat.testing.MainThreadRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/// The canonical path resolves `.` and `..`: a check that a name stays
/// inside a directory accepted `root/../outside`, which the native file
/// system then resolved outside it.
public class FileCanonicalPathTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Test
    public void resolvesDotSegments() throws IOException {
        assertEquals("file:///outside", new File("file:///root/../outside").getCanonicalPath());
        assertEquals("file:///a/c", new File("/a/./b/../c").getCanonicalPath());
        assertEquals("file:///a/c", new File(new File("file:///a/b"), "../c").getCanonicalFile().getPath());
        assertEquals("file:///", new File("/a/../..").getCanonicalPath());
        assertEquals("file:///data/files/x", new File("file:///data/files/x").getCanonicalPath());
    }

    @Test
    public void keepsAnAuthorityLessHome() throws IOException {
        assertEquals("file://home/x", new File("file://home/files/../x").getCanonicalPath());
    }
}
