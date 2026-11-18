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
package com.codename1.impl.javase;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/// Issue #6255: the desktop tools run on this port with the developer's project as their
/// working directory. The source tree's theme.res is the simulator's live copy and must
/// never replace a tool's own theme.
class JavaSEPortSourceThemeTest {
    private static final String MARKER = "project-theme-from-source-tree";

    @TempDir
    File project;

    private String previousUserDir;
    private boolean previousDesignMode;

    @BeforeEach
    void setUp() throws IOException {
        previousUserDir = System.getProperty("user.dir");
        previousDesignMode = JavaSEPort.isDesignMode();
        File resources = new File(project, "src" + File.separator + "main" + File.separator + "resources");
        Files.createDirectories(resources.toPath());
        Files.write(new File(resources, "theme.res").toPath(), MARKER.getBytes(StandardCharsets.UTF_8));
        System.setProperty("user.dir", project.getAbsolutePath());
    }

    @AfterEach
    void tearDown() {
        System.setProperty("user.dir", previousUserDir);
        JavaSEPort.setDesignMode(previousDesignMode);
    }

    @Test
    void aDesktopToolNeverReadsTheProjectsSourceTheme() throws IOException {
        JavaSEPort.setDesignMode(false);
        JavaSEPort port = new JavaSEPort();
        assertNotEquals(MARKER, read(port.getResourceAsStream(getClass(), "/theme.res")));
    }

    @Test
    void theSimulatorStillReadsTheLiveSourceTheme() throws IOException {
        JavaSEPort.setDesignMode(true);
        JavaSEPort port = new JavaSEPort();
        assertEquals(MARKER, read(port.getResourceAsStream(getClass(), "/theme.res")));
    }

    private static String read(InputStream in) throws IOException {
        if (in == null) {
            return null;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            in.close();
        }
    }
}
