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
package com.codename1.builders;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The app stub installs the telemetry bootstrap exactly when the project has one.
/// Nothing else instantiates it on iOS or Android, so a missing line here is an
/// annotation that compiles, generates, and does nothing on the device.
class TelemetryBootstrapInstallTest {

    @Test
    void theStubInstallsTheBootstrapWhenTheProjectHasOne(@TempDir File dir) throws Exception {
        File zip = new File(dir, "app.jar");
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip));
        try {
            out.putNextEntry(new ZipEntry("cn1app/TelemetryBootstrap.class"));
            out.write(new byte[] {(byte) 0xca, (byte) 0xfe});
            out.closeEntry();
        } finally {
            out.close();
        }
        String install = Executor.annotationFrameworksInstallSource(zip, "    ");
        assertTrue(install.contains("new cn1app.TelemetryBootstrap();"), install);
    }

    @Test
    void anUnpackedClassesDirectoryIsProbedLikeTheJar(@TempDir File dir) throws Exception {
        // The JavaScript and native desktop builders hold the directory they
        // unpacked the app into, not the jar. Asking only a jar made every
        // annotation framework inert on those platforms.
        touch(new File(dir, "cn1app/TelemetryBootstrap.class"));
        touch(new File(dir, "cn1app/MapperBootstrap.class"));
        touch(new File(dir, "com/codename1/router/generated/Routes.class"));
        String install = Executor.annotationFrameworksInstallSource(dir, "    ");
        assertTrue(install.contains("new cn1app.TelemetryBootstrap();"), install);
        assertTrue(install.contains("new cn1app.MapperBootstrap();"), install);
        assertTrue(Executor.routeDispatcherInstallSource(dir, "    ")
                .contains("new com.codename1.router.generated.Routes();"));
    }

    private static void touch(File f) throws Exception {
        assertTrue(f.getParentFile().isDirectory() || f.getParentFile().mkdirs());
        new FileOutputStream(f).close();
    }

    @Test
    void anAppWithoutItGetsNoInstallLine(@TempDir File dir) throws Exception {
        File zip = new File(dir, "app.jar");
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip));
        try {
            out.putNextEntry(new ZipEntry("com/example/App.class"));
            out.write(new byte[] {(byte) 0xca, (byte) 0xfe});
            out.closeEntry();
        } finally {
            out.close();
        }
        assertFalse(Executor.annotationFrameworksInstallSource(zip, "    ")
                .contains("TelemetryBootstrap"));
    }
}
