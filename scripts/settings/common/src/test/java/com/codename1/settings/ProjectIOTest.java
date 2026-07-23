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

import com.codename1.settings.project.ProjectBinding;
import com.codename1.settings.project.ProjectIO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ProjectIOTest {
    @Test
    public void unixPathsGetTheStandardFileUrlPrefix() {
        assertEquals("file:///Users/dev/app", ProjectIO.fsUrl("/Users/dev/app"));
    }

    @Test
    public void windowsDriveLetterPathsGetASlashBeforeTheDrive() {
        // Without the extra slash, stripping file:// on the consumer side
        // yields /C:\... which java.io.File resolves drive-relative
        // (C:\C:\...) and every project-file read silently fails (#5443).
        assertEquals("file:///C:/Users/dev/app", ProjectIO.fsUrl("C:\\Users\\dev\\app"));
        assertEquals("file:///C:/Users/dev/app", ProjectIO.fsUrl("C:/Users/dev/app"));
    }

    @Test
    public void windowsPathsWithSpacesSurviveTheUrlRoundTrip() {
        assertEquals("file:///C:/Users/John Smith/My App",
                ProjectIO.fsUrl("C:\\Users\\John Smith\\My App"));
    }

    @Test
    public void existingUrlsPassThroughUntouched() {
        assertEquals("file:///C:/Users/dev/app", ProjectIO.fsUrl("file:///C:/Users/dev/app"));
        assertEquals("jar://something", ProjectIO.fsUrl("jar://something"));
        assertNull(ProjectIO.fsUrl(null));
    }

    @Test
    public void bindingParserKeepsWindowsBackslashesVerbatim() {
        ProjectBinding b = ProjectBinding.parse(
                "projectDir=C:\\Users\\John Smith\\My App\n"
                + "settings=C:\\Users\\John Smith\\My App\\codenameone_settings.properties\n");
        assertEquals("C:\\Users\\John Smith\\My App", b.projectDir());
        assertEquals("C:\\Users\\John Smith\\My App\\codenameone_settings.properties", b.settings());
    }
}
