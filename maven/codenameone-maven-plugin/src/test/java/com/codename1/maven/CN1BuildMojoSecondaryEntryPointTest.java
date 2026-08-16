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

    /**
     * The local path honours the same override the cloud path does.
     *
     * <p>Reading only the unprefixed project setting made one invocation build two different
     * products: {@code -Dcodename1.arg.watchMain=...} reached the daemon, because the mirror
     * leaves an existing value alone, and was ignored on the developer's own machine. An override
     * that works in one place and silently does nothing in the other is worse than one that works
     * nowhere, because nothing about the local build says the flag was dropped.</p>
     */
    @Test
    public void theLocalRequestPrefersTheOverlaidArgument() throws Exception {
        Properties props = new Properties();
        props.setProperty("codename1.watchMain", "com.acme.FromProjectFile");
        props.setProperty("codename1.arg.watchMain", "com.acme.FromCommandLine");
        assertEquals("com.acme.FromCommandLine", localArgument(props, "watchMain"));

        // With no override the project file is still what decides.
        Properties plain = new Properties();
        plain.setProperty("codename1.watchMain", "com.acme.FromProjectFile");
        assertEquals("com.acme.FromProjectFile", localArgument(plain, "watchMain"));

        // And an override with no project setting at all enables the target, where before the
        // absent unprefixed value simply turned the watch build off.
        Properties overrideOnly = new Properties();
        overrideOnly.setProperty("codename1.arg.watchStandalone", "true");
        assertEquals("true", localArgument(overrideOnly, "watchStandalone"));
    }

    /** Runs the local helper and reports what it put on the request. */
    private static String localArgument(Properties props, String argName) throws Exception {
        com.codename1.builders.BuildRequest request = new com.codename1.builders.BuildRequest();
        java.lang.reflect.Method m = CN1BuildMojo.class.getDeclaredMethod(
                "putSecondaryEntryPointArguments",
                com.codename1.builders.BuildRequest.class, Properties.class);
        m.setAccessible(true);
        m.invoke(null, request, props);
        return request.getArg(argName, null);
    }

    /**
     * A command-line override survives the mirror.
     *
     * <p>This runs after overlayCommandLineBuildHints, so a {@code -Dcodename1.arg.watchMain=...}
     * is already in the properties -- and overwriting it with the project file's value made the
     * standard build-argument override silently do nothing, sending the cloud the wrong lifecycle
     * or a companion where a standalone Wear build was asked for.</p>
     */
    @Test
    public void aCommandLineArgumentIsNotOverwrittenByTheProjectFile() {
        Properties props = new Properties();
        props.setProperty("codename1.watchMain", "com.acme.FromProjectFile");
        props.setProperty("codename1.arg.watchMain", "com.acme.FromCommandLine");
        props.setProperty("codename1.watchStandalone", "false");
        props.setProperty("codename1.arg.watchStandalone", "true");

        CN1BuildMojo.mirrorSecondaryEntryPointsToBuildArgs(props);

        assertEquals("com.acme.FromCommandLine", props.getProperty("codename1.arg.watchMain"));
        assertEquals("true", props.getProperty("codename1.arg.watchStandalone"));
    }

    /** A blank argument is not an override, so the project file still gets mirrored. */
    @Test
    public void aBlankArgumentDoesNotSuppressTheMirror() {
        Properties props = new Properties();
        props.setProperty("codename1.watchMain", "com.acme.FromProjectFile");
        props.setProperty("codename1.arg.watchMain", "   ");

        CN1BuildMojo.mirrorSecondaryEntryPointsToBuildArgs(props);

        assertEquals("com.acme.FromProjectFile", props.getProperty("codename1.arg.watchMain"));
    }


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
