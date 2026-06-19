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
package com.codename1.gamebuilder.project;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Headless tests for the {@code gamebuilder.input} binding parser.
class ProjectBindingTest {

    @Test
    void parsesAllKeysIgnoringCommentsAndUnknowns() {
        String input = "# Codename One game builder project binding\n"
                + "projectDir=/proj/common\n"
                + "gamesDir=/proj/common/src/main/resources/games\n"
                + "sourceDir=/proj/common/src/main/java\n"
                + "packageName=com.example.game\n"
                + "future=ignored\n"
                + "output=/home/u/.gameBuilder/abc.output\n";
        ProjectBinding b = ProjectBinding.parse(input);
        assertTrue(b.isValid());
        assertEquals("/proj/common", b.projectDir());
        assertEquals("/proj/common/src/main/resources/games", b.gamesDir());
        assertEquals("/proj/common/src/main/java", b.sourceDir());
        assertEquals("com.example.game", b.packageName());
        assertEquals("/home/u/.gameBuilder/abc.output", b.output());
    }

    @Test
    void invalidWithoutGamesDir() {
        assertFalse(ProjectBinding.parse("projectDir=/x\n").isValid());
        assertFalse(ProjectBinding.parse("").isValid());
        assertFalse(ProjectBinding.parse(null).isValid());
    }

    @Test
    void handlesWindowsLineEndingsAndWhitespace() {
        ProjectBinding b = ProjectBinding.parse("  gamesDir = C:\\proj\\games \r\n");
        assertEquals("C:\\proj\\games", b.gamesDir());
    }
}
