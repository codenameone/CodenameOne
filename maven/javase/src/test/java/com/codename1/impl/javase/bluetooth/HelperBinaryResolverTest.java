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
package com.codename1.impl.javase.bluetooth;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The helper-binary resolution order of {@link HelperBinaryResolver}: system
 * property, bundled classpath resource, {@code PATH} lookup -- exercised
 * with fake files so the tests never depend on this machine's setup.
 * {@code "TestOS"} is used as the os.name whenever the bundled-resource
 * step must deterministically not resolve (no resource ships for it).
 */
public class HelperBinaryResolverTest {

    @TempDir
    File tempDir;

    private File fakeBinary(String name) throws IOException {
        File f = new File(tempDir, name);
        FileOutputStream out = new FileOutputStream(f);
        out.write("#!/bin/sh\n".getBytes("UTF-8"));
        out.close();
        f.setExecutable(true, true);
        return f;
    }

    @Test
    public void systemPropertyWinsWhenTheFileExists() throws IOException {
        File binary = fakeBinary("my-helper");
        List<String> attempted = new ArrayList<>();
        File resolved = HelperBinaryResolver.resolveHelperBinary(
                binary.getAbsolutePath(), "TestOS", "x86_64", null, attempted);
        Assertions.assertEquals(binary.getAbsolutePath(),
                resolved.getAbsolutePath());
        Assertions.assertTrue(attempted.get(0).contains(
                HelperBinaryResolver.HELPER_PATH_PROPERTY));
    }

    @Test
    public void missingPropertyFileFallsThroughToLaterSteps()
            throws IOException {
        File onPath = fakeBinary(HelperBinaryResolver.HELPER_BASENAME);
        List<String> attempted = new ArrayList<>();
        File resolved = HelperBinaryResolver.resolveHelperBinary(
                new File(tempDir, "no-such-file").getAbsolutePath(),
                "TestOS", "x86_64", tempDir.getAbsolutePath(), attempted);
        Assertions.assertEquals(onPath.getAbsolutePath(),
                resolved.getAbsolutePath());
        String trace = String.valueOf(attempted);
        Assertions.assertTrue(trace.contains("no such file"), trace);
        Assertions.assertTrue(trace.contains("PATH lookup"), trace);
    }

    @Test
    public void pathLookupScansEntriesInOrder() throws IOException {
        File emptyDir = new File(tempDir, "empty");
        emptyDir.mkdirs();
        File binDir = new File(tempDir, "bin");
        binDir.mkdirs();
        File binary = new File(binDir, HelperBinaryResolver.HELPER_BASENAME);
        new FileOutputStream(binary).close();
        String pathEnv = emptyDir.getAbsolutePath() + File.pathSeparator
                + binDir.getAbsolutePath();
        List<String> attempted = new ArrayList<>();
        File resolved = HelperBinaryResolver.resolveFromPathEnv(pathEnv,
                HelperBinaryResolver.HELPER_BASENAME, attempted);
        Assertions.assertEquals(binary.getAbsolutePath(),
                resolved.getAbsolutePath());
    }

    @Test
    public void nothingFoundReportsEveryAttemptedLocation() {
        List<String> attempted = new ArrayList<>();
        File resolved = HelperBinaryResolver.resolveHelperBinary(null, "TestOS",
                "x86_64", "", attempted);
        Assertions.assertNull(resolved);
        String trace = String.valueOf(attempted);
        Assertions.assertTrue(trace.contains(
                HelperBinaryResolver.HELPER_PATH_PROPERTY), trace);
        Assertions.assertTrue(trace.contains("not set"), trace);
        Assertions.assertTrue(
                trace.contains("no bundled helper for os.name=TestOS"),
                trace);
        Assertions.assertTrue(trace.contains("os.arch=x86_64"), trace);
        Assertions.assertTrue(trace.contains("PATH lookup"), trace);
    }

    @Test
    public void bundledResourceLocationsAreKeyedByOs() {
        // macOS ships one universal Mach-O binary, so the arch is ignored
        Assertions.assertEquals(HelperBinaryResolver.HELPER_RESOURCE_DIR
                + "macos/cn1-ble-helper",
                HelperBinaryResolver.helperResourcePath("Mac OS X", "x86_64"));
        Assertions.assertEquals(HelperBinaryResolver.HELPER_RESOURCE_DIR
                + "macos/cn1-ble-helper",
                HelperBinaryResolver.helperResourcePath("Mac OS X", "aarch64"));
        Assertions.assertNull(
                HelperBinaryResolver.helperResourcePath("TestOS", "x86_64"));
        Assertions.assertEquals("cn1-ble-helper.exe",
                HelperBinaryResolver.helperExecutableName("Windows 11"));
        Assertions.assertEquals("cn1-ble-helper",
                HelperBinaryResolver.helperExecutableName("Mac OS X"));
    }

    @Test
    public void linuxAndWindowsResourcesAreKeyedByArchitecture() {
        // ELF and PE have no fat-binary format, so these are per-arch
        Assertions.assertEquals(HelperBinaryResolver.HELPER_RESOURCE_DIR
                + "linux/x64/cn1-ble-helper",
                HelperBinaryResolver.helperResourcePath("Linux", "amd64"));
        Assertions.assertEquals(HelperBinaryResolver.HELPER_RESOURCE_DIR
                + "linux/arm64/cn1-ble-helper",
                HelperBinaryResolver.helperResourcePath("Linux", "aarch64"));
        Assertions.assertEquals(HelperBinaryResolver.HELPER_RESOURCE_DIR
                + "windows/x64/cn1-ble-helper.exe",
                HelperBinaryResolver.helperResourcePath("Windows 11", "amd64"));
        Assertions.assertEquals(HelperBinaryResolver.HELPER_RESOURCE_DIR
                + "windows/arm64/cn1-ble-helper.exe",
                HelperBinaryResolver.helperResourcePath("Windows 11",
                        "aarch64"));
    }

    @Test
    public void architectureAliasesMapOntoTheBundledDirectories() {
        Assertions.assertEquals("x64",
                HelperBinaryResolver.normalizeArch("amd64"));
        Assertions.assertEquals("x64",
                HelperBinaryResolver.normalizeArch("x86_64"));
        Assertions.assertEquals("x64",
                HelperBinaryResolver.normalizeArch("X64"));
        Assertions.assertEquals("arm64",
                HelperBinaryResolver.normalizeArch("aarch64"));
        Assertions.assertEquals("arm64",
                HelperBinaryResolver.normalizeArch("arm64"));
        // 32-bit x86/ARM ship no binary: resolution falls through to PATH
        Assertions.assertNull(HelperBinaryResolver.normalizeArch("x86"));
        Assertions.assertNull(HelperBinaryResolver.normalizeArch("i386"));
        Assertions.assertNull(HelperBinaryResolver.normalizeArch("arm"));
        Assertions.assertNull(HelperBinaryResolver.normalizeArch(null));
    }

    @Test
    public void unknownArchitectureStillFallsBackToPath() throws IOException {
        File binary = fakeBinary(HelperBinaryResolver.HELPER_BASENAME);
        List<String> attempted = new ArrayList<>();
        File resolved = HelperBinaryResolver.resolveHelperBinary(null, "Linux",
                "riscv64", tempDir.getAbsolutePath(), attempted);
        Assertions.assertEquals(binary.getAbsolutePath(),
                resolved.getAbsolutePath());
        Assertions.assertTrue(String.valueOf(attempted).contains(
                "os.arch=riscv64"), String.valueOf(attempted));
    }
}
