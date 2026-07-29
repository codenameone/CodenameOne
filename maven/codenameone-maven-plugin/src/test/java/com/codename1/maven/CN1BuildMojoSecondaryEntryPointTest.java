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

import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * The watch and TV entry points are declared next to {@code codename1.mainName},
 * without the {@code codename1.arg.} prefix. Local builds read them straight off
 * the settings file, but the build server only lifts {@code codename1.arg.*}
 * keys out of the uploaded file -- so they have to be mirrored into that
 * namespace or a cloud build produces no watch app at all.
 */
public class CN1BuildMojoSecondaryEntryPointTest {

    @Test
    public void watchMainReachesTheBuildServer() {
        Properties props = new Properties();
        props.setProperty("codename1.mainName", "MyApp");
        props.setProperty("codename1.watchMain", "com.mycompany.myapp.MyWatchMain");

        CN1BuildMojo.mirrorSecondaryEntryPointsToBuildArgs(props);

        assertEquals("com.mycompany.myapp.MyWatchMain",
                props.getProperty("codename1.arg.watchMain"));
        // The original declaration stays put -- it is a project setting, not a
        // build hint, and the local path still reads it from there.
        assertEquals("com.mycompany.myapp.MyWatchMain",
                props.getProperty("codename1.watchMain"));
    }

    @Test
    public void watchStandaloneAndTvMainReachTheBuildServer() {
        Properties props = new Properties();
        props.setProperty("codename1.watchMain", "com.mycompany.myapp.MyWatchMain");
        props.setProperty("codename1.watchStandalone", "true");
        props.setProperty("codename1.tvMain", "com.mycompany.myapp.MyTvMain");

        CN1BuildMojo.mirrorSecondaryEntryPointsToBuildArgs(props);

        assertEquals("true", props.getProperty("codename1.arg.watchStandalone"));
        assertEquals("com.mycompany.myapp.MyTvMain", props.getProperty("codename1.arg.tvMain"));
    }

    @Test
    public void surroundingWhitespaceIsTrimmed() {
        Properties props = new Properties();
        props.setProperty("codename1.watchMain", "  com.mycompany.myapp.MyWatchMain  ");

        CN1BuildMojo.mirrorSecondaryEntryPointsToBuildArgs(props);

        assertEquals("com.mycompany.myapp.MyWatchMain",
                props.getProperty("codename1.arg.watchMain"));
    }

    @Test
    public void aProjectWithoutSecondaryEntryPointsIsUntouched() {
        Properties props = new Properties();
        props.setProperty("codename1.mainName", "MyApp");
        // A blank declaration is the same as none: it must not switch a build on.
        props.setProperty("codename1.watchMain", "   ");

        CN1BuildMojo.mirrorSecondaryEntryPointsToBuildArgs(props);

        assertNull(props.getProperty("codename1.arg.watchMain"));
        assertNull(props.getProperty("codename1.arg.watchStandalone"));
        assertNull(props.getProperty("codename1.arg.tvMain"));
    }
}
