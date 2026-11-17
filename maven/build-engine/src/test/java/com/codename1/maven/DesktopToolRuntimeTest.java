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

import com.codename1.build.BuildExecutionException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// A tool compiled for a newer Java than the build runs on must fail the goal
/// with the version to install, not "launch" into a JNI error dialog.
public class DesktopToolRuntimeTest {
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void readsMainClassVersion() throws Exception {
        File jar = jarWithMainClass(61);
        assertEquals(61, DesktopTool.classFileMajorVersion(jar,
                DesktopTool.CERTIFICATE_WIZARD.mainClass()));
        assertEquals(-1, DesktopTool.classFileMajorVersion(jar, "no.Such"));
    }

    @Test
    public void refusesToolNewerThanRuntime() throws Exception {
        File jar = jarWithMainClass(DesktopTool.runtimeMaxClassFileVersion() + 1);
        try {
            DesktopTool.CERTIFICATE_WIZARD.requireCompatibleRuntime(jar);
            fail("A tool newer than the runtime must be refused");
        } catch (BuildExecutionException ex) {
            assertTrue(ex.getMessage(), ex.getMessage().contains("requires Java "));
            assertTrue(ex.getMessage(), ex.getMessage().contains("JAVA_HOME"));
        }
    }

    @Test
    public void acceptsToolTheRuntimeCanLoad() throws Exception {
        DesktopTool.CERTIFICATE_WIZARD.requireCompatibleRuntime(
                jarWithMainClass(DesktopTool.runtimeMaxClassFileVersion()));
        DesktopTool.CERTIFICATE_WIZARD.requireCompatibleRuntime(null);
    }

    private File jarWithMainClass(int major) throws Exception {
        File jar = tmp.newFile("tool-" + major + ".jar");
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jar))) {
            out.putNextEntry(new JarEntry(
                    DesktopTool.CERTIFICATE_WIZARD.mainClass().replace('.', '/') + ".class"));
            out.write(new byte[]{(byte) 0xca, (byte) 0xfe, (byte) 0xba, (byte) 0xbe, 0, 0,
                    (byte) (major >> 8), (byte) major});
            out.closeEntry();
        }
        return jar;
    }
}
