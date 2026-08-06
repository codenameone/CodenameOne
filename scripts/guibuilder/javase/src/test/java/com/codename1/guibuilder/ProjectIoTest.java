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

import com.codename1.guibuilder.project.ProjectIO;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.JPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Saving is the one operation in this editor that can destroy work that already exists on disk, so
 * it is checked directly rather than through the designer.
 */
class ProjectIoTest {
    @BeforeAll
    static void initializeCodenameOneRuntime() {
        if (!com.codename1.ui.Display.isInitialized()) com.codename1.ui.Display.init(new JPanel());
    }

    @Test
    void writingReplacesTheFileAndLeavesNoTemporaryBehind(@TempDir Path dir) throws Exception {
        File target = dir.resolve("Form.gui").toFile();
        ProjectIO.write(target.getAbsolutePath(), "<component name=\"first\"/>");
        assertEquals("<component name=\"first\"/>", read(target));

        ProjectIO.write(target.getAbsolutePath(), "<component name=\"second\"/>");
        assertEquals("<component name=\"second\"/>", read(target), "the second save did not replace the first");

        for (File file : dir.toFile().listFiles()) {
            assertFalse(file.getName().endsWith(".cn1tmp"),
                    "a temporary file survived the save: " + file.getName());
        }
    }

    /**
     * The truncate-then-write that this replaced left an empty file behind when the write failed.
     * The target must not even be touched until the replacement content is complete on disk.
     */
    @Test
    void aFailedWriteLeavesThePreviousContentIntact(@TempDir Path dir) throws Exception {
        File target = dir.resolve("Form.gui").toFile();
        ProjectIO.write(target.getAbsolutePath(), "<component name=\"survivor\"/>");

        // A directory where the temporary file has to go makes openOutputStream fail the way a full
        // disk would, after the editor has already decided to save.
        assertTrue(new File(target.getAbsolutePath() + ".cn1tmp").mkdir());
        try {
            ProjectIO.write(target.getAbsolutePath(), "<component name=\"replacement\"/>");
            fail("the failed write reported success");
        } catch (Exception expected) {
            // the save is expected to fail; what matters is the state it leaves behind
        }
        assertEquals("<component name=\"survivor\"/>", read(target),
                "a failed save destroyed the previously saved form");
    }

    @Test
    void writingCreatesMissingParentDirectories(@TempDir Path dir) throws Exception {
        File target = dir.resolve("com").resolve("example").resolve("Form.gui").toFile();
        ProjectIO.write(target.getAbsolutePath(), "<component name=\"nested\"/>");
        assertEquals("<component name=\"nested\"/>", read(target));
    }

    /**
     * Every path in this editor arrives from the Maven plugin in the platform's own notation, so on
     * Windows the separator arithmetic runs over backslashes unless the URL is normalized first.
     */
    @Test
    void windowsPathsBecomeUsableFileUrls() {
        assertEquals("file://C:/Users/dev/app/src/main/guibuilder",
                ProjectIO.fsUrl("C:\\Users\\dev\\app\\src\\main\\guibuilder"));
        assertEquals("file:///home/dev/app", ProjectIO.fsUrl("/home/dev/app"));
        assertEquals("file:///home/dev/app", ProjectIO.fsUrl("file:///home/dev/app"));
        assertNull(ProjectIO.fsUrl(null));
    }

    private static String read(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
