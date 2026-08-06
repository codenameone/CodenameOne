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

package com.codename1.guibuilder;

import com.codename1.ui.css.CSSThemeCompiler;
import com.codename1.ui.util.MutableResource;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Hashtable;
import javax.swing.JPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * If the project's theme.css fails to compile the editor silently falls back to its own theme, so
 * the canvas shows styling that has nothing to do with the CSS being edited.
 */
class ProjectCssCompilesTest {
    @BeforeAll static void init() {
        if (!com.codename1.ui.Display.isInitialized()) com.codename1.ui.Display.init(new JPanel());
    }

    @Test
    void theDemoProjectThemeCompilesIntoAUsableTheme() throws Exception {
        File css = new File("../demo-project/src/main/css/theme.css");
        assertTrue(css.isFile(), "demo theme.css is missing at " + css.getAbsolutePath());
        MutableResource resources = new MutableResource();
        try {
            new CSSThemeCompiler().compile(
                    new String(Files.readAllBytes(css.toPath()), StandardCharsets.UTF_8),
                    resources, "ProjectTheme");
        } catch (RuntimeException ex) {
            fail("the demo theme.css does not compile, so the canvas can never show it: " + ex);
        }
        Hashtable theme = resources.getTheme("ProjectTheme");
        assertNotNull(theme, "compilation produced no theme");
        assertFalse(theme.isEmpty(), "the compiled theme is empty");
        System.out.println("PROJECT CSS keys: " + theme.size());
        int titleKeys = 0;
        for (Object key : theme.keySet()) {
            if (String.valueOf(key).startsWith("Title")) titleKeys++;
        }
        System.out.println("PROJECT CSS Title keys: " + titleKeys);
        assertTrue(titleKeys > 0, "the compiled theme has no Title styling even though theme.css sets it");
    }
}
