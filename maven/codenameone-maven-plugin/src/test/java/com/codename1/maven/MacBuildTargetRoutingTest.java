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

import com.codename1.builders.Executor;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/// Guards the macOS (AppKit) build targets, and guards Mac Catalyst against
/// growing one.
///
/// Both halves fail quietly if they drift. A Mac target that stops hardening as
/// "mac" starts reading `harden.ios.enabled` instead, so a project's opt-out
/// silently stops applying. A `-source` target that stops being recognized as
/// local gets submitted to the build server rather than generating a project
/// locally.
public class MacBuildTargetRoutingTest {

    private static boolean isLocal(String target) throws Exception {
        Method m = CN1BuildMojo.class.getDeclaredMethod("isLocalBuildTarget", String.class);
        m.setAccessible(true);
        return (Boolean) m.invoke(null, target);
    }

    private static String hardenPlatform(String target) throws Exception {
        Method m = CN1BuildMojo.class.getDeclaredMethod("hardenPlatformForBuildTarget", String.class);
        m.setAccessible(true);
        return (String) m.invoke(null, target);
    }

    @Test
    public void theMacTargetsAreTheOnesThatAlreadyExisted() {
        assertEquals("mac-os-x-native", Executor.BUILD_TARGET_MAC_NATIVE);
        assertEquals("mac-source", Executor.BUILD_TARGET_MAC_NATIVE_PROJECT);
    }

    /// Mac Catalyst has no build target, and must not grow one. It IS an iPhone
    /// build -- IPhoneBuilder switches to the Catalyst slice on macNative.enabled
    /// alone -- so a target name would be a second spelling for what the hint
    /// already says, in the maven targeting, the ant template and both builders.
    @Test
    public void catalystHasNoTargetOfItsOwn() throws Exception {
        for (java.lang.reflect.Field f : Executor.class.getDeclaredFields()) {
            if (f.getName().startsWith("BUILD_TARGET_")) {
                Object v = f.get(null);
                assertFalse("Executor." + f.getName() + " reintroduces a Catalyst build target",
                        String.valueOf(v).contains("catalyst"));
            }
        }
    }

    @Test
    public void sourceTargetsBuildLocallyAndCloudTargetsDoNot() throws Exception {
        assertTrue(isLocal(Executor.BUILD_TARGET_MAC_NATIVE_PROJECT));
        assertFalse(isLocal(Executor.BUILD_TARGET_MAC_NATIVE));
    }

    @Test
    public void everyMacTargetHardensAsMac() throws Exception {
        // Not "ios", even though these run with platform=ios, and not null --
        // a Mac target that stops hardening as "mac" reads harden.ios.enabled
        // instead, and the project's opt-out silently stops applying.
        assertEquals("mac", hardenPlatform(Executor.BUILD_TARGET_MAC_NATIVE));
        assertEquals("mac", hardenPlatform(Executor.BUILD_TARGET_MAC_NATIVE_PROJECT));
        assertEquals("mac", hardenPlatform(Executor.BUILD_TARGET_MAC_NATIVE_LOCAL));
    }

    @Test
    public void nonMacTargetsCarryNoHardeningOverride() throws Exception {
        // The JavaSE Mac desktop bundle is a different product entirely and must
        // not be swept up by a substring match on "mac".
        assertEquals(null, hardenPlatform("mac-os-x-desktop"));
        assertEquals(null, hardenPlatform("ios-device"));
        assertEquals(null, hardenPlatform("android-device"));
    }

    @Test
    public void theLocalAppKitTargetIsBuiltHere() throws Exception {
        assertEquals("local-mac-device", Executor.BUILD_TARGET_MAC_NATIVE_LOCAL);
        // It has to be recognised as local, or the mojo submits it to the build
        // server and the developer's Xcode is never used.
        assertTrue(isLocal(Executor.BUILD_TARGET_MAC_NATIVE_LOCAL));
        assertEquals("mac", hardenPlatform(Executor.BUILD_TARGET_MAC_NATIVE_LOCAL));
    }

    /// Reads the mojo's own source for the cloud dispatch arm, because the
}
