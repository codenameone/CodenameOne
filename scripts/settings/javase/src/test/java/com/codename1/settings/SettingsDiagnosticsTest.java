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
package com.codename1.settings;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SettingsDiagnosticsTest {

    @Test
    public void onScreenCaptureSitsBesideTheScreenshot() {
        assertEquals("out.onscreen.png", CodenameOneSettingsStub.onScreenPath("out.png"));
        assertEquals("/tmp/a.b/shot.onscreen.png",
                CodenameOneSettingsStub.onScreenPath("/tmp/a.b/shot.png"));
        assertEquals("C:\\Users\\Me\\a.b\\shot.onscreen.png",
                CodenameOneSettingsStub.onScreenPath("C:\\Users\\Me\\a.b\\shot.png"));
    }

    /**
     * A directory component containing a dot must not be mistaken for the
     * screenshot's extension - the suffix has to land on the file name.
     */
    @Test
    public void onScreenCaptureHandlesExtensionlessNames() {
        assertEquals("shot.onscreen.png", CodenameOneSettingsStub.onScreenPath("shot"));
        assertEquals("/tmp/a.b/shot.onscreen.png",
                CodenameOneSettingsStub.onScreenPath("/tmp/a.b/shot"));
    }

    /**
     * The unresponsive-EDT fallback is the report that matters most in a bug
     * report, so it must produce a usable file with no Codename One Display
     * booted at all.
     */
    @Test
    public void unresponsiveEdtReportIsWrittenWithoutADisplay(@TempDir Path dir) throws Exception {
        File target = dir.resolve("diag.txt").toFile();
        SettingsDiagnostics.writeUnresponsiveEdt(target, null);
        assertTrue(target.isFile(), "the fallback report must still be written");
        String report = new String(Files.readAllBytes(target.toPath()), StandardCharsets.UTF_8);
        assertTrue(report.contains("edt.responsive=false"), report);
        assertTrue(report.contains("[environment]"), report);
        assertTrue(report.contains("os.name="), report);
        assertTrue(report.contains("[edt-stack]"), report);
    }

    @Test
    public void sampledUiidsCoverBothThemeVariants() {
        for (String uiid : SettingsDiagnostics.SAMPLED_UIIDS) {
            if (uiid.endsWith("Dark")) {
                continue;
            }
            boolean hasDark = false;
            for (String candidate : SettingsDiagnostics.SAMPLED_UIIDS) {
                hasDark |= candidate.equals(uiid + "Dark");
            }
            assertTrue(hasDark, "dark variant missing for sampled UIID " + uiid);
        }
    }
}
