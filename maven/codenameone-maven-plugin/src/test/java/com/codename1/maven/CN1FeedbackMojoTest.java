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
package com.codename1.maven;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CN1FeedbackMojoTest {
    @TempDir
    File tmp;

    @Test
    void eachNoteIsItsOwnFileAndNothingIsOverwritten() throws Exception {
        File dir = new File(tmp, ".cn1/feedback");
        Date when = new Date(1795500000000L);
        File a = CN1FeedbackMojo.write(dir, "The CSS error did not say which file", "8.0", when);
        File b = CN1FeedbackMojo.write(dir, "Second note", "8.0", when);
        assertNotEquals(a, b);
        assertEquals(2, dir.listFiles().length);
        String text = new String(Files.readAllBytes(a.toPath()), StandardCharsets.UTF_8);
        assertTrue(text.contains("The CSS error did not say which file"), text);
        assertTrue(text.contains("- Plugin: 8.0"), text);
        assertTrue(text.contains("- Time: 2026-"), text);
    }
}
