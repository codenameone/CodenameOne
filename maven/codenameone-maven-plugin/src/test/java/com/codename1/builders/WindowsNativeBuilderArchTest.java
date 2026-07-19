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
package com.codename1.builders;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Platform-independent unit tests for the architecture resolution in
 * {@link WindowsNativeBuilder} (the native compile/link itself only runs on
 * Windows, but the arch -> triple / vcvars mapping is pure logic).
 */
class WindowsNativeBuilderArchTest {

    @Test
    void normalizesArchSynonyms() {
        assertEquals(WindowsNativeBuilder.ARCH_X64, WindowsNativeBuilder.normalizeArch("x64"));
        assertEquals(WindowsNativeBuilder.ARCH_X64, WindowsNativeBuilder.normalizeArch("x86_64"));
        assertEquals(WindowsNativeBuilder.ARCH_X64, WindowsNativeBuilder.normalizeArch("amd64"));
        assertEquals(WindowsNativeBuilder.ARCH_ARM64, WindowsNativeBuilder.normalizeArch("arm64"));
        assertEquals(WindowsNativeBuilder.ARCH_ARM64, WindowsNativeBuilder.normalizeArch("aarch64"));
        // Null and unknown values fall back to x64 rather than failing the build.
        assertEquals(WindowsNativeBuilder.ARCH_X64, WindowsNativeBuilder.normalizeArch(null));
        assertEquals(WindowsNativeBuilder.ARCH_X64, WindowsNativeBuilder.normalizeArch("mips"));
    }

    @Test
    void mapsArchToClangTriple() {
        assertEquals("x86_64-pc-windows-msvc", WindowsNativeBuilder.targetTriple("x64"));
        assertEquals("x86_64-pc-windows-msvc", WindowsNativeBuilder.targetTriple("amd64"));
        assertEquals("aarch64-pc-windows-msvc", WindowsNativeBuilder.targetTriple("arm64"));
        assertEquals("aarch64-pc-windows-msvc", WindowsNativeBuilder.targetTriple("aarch64"));
    }

    @Test
    void vcvarsUsesCrossFormWhenHostDiffersFromTarget() {
        // Native build: just the target arch.
        assertEquals("x64", WindowsNativeBuilder.vcvarsArchArg("x64", "x64"));
        assertEquals("arm64", WindowsNativeBuilder.vcvarsArchArg("arm64", "arm64"));
        // Cross build (e.g. building x64 on an arm64 host): host_target form.
        assertEquals("arm64_x64", WindowsNativeBuilder.vcvarsArchArg("arm64", "x64"));
        assertEquals("x64_arm64", WindowsNativeBuilder.vcvarsArchArg("x64", "arm64"));
    }

    @Test
    void restrictedCalendarCapabilityRequiresExplicitOptIn() throws Exception {
        WindowsNativeBuilder builder = new WindowsNativeBuilder();
        Method method = WindowsNativeBuilder.class.getDeclaredMethod("buildAppxManifest",
                BuildRequest.class, String.class, String.class);
        method.setAccessible(true);
        BuildRequest request = new BuildRequest();
        request.setPackageName("com.example.calendar");
        request.setDisplayName("Calendar Test");
        request.setVendor("Example");
        request.setVersion("1.0");

        String withoutOptIn = (String)method.invoke(builder, request, "x64", null);
        assertFalse(withoutOptIn.contains("Name=\"appointments\""));

        request.putArgument("windows.calendar.restrictedCapability", "true");
        String withOptIn = (String)method.invoke(builder, request, "x64", null);
        assertTrue(withOptIn.contains("<rescap:Capability Name=\"appointments\"/>"));
    }
}
