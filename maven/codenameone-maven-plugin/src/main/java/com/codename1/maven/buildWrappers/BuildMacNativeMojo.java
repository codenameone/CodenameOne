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
package com.codename1.maven.buildWrappers;


import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Build wrapper for the native Mac cloud target ({@code mac-os-x-native}), which
 * produces a native macOS {@code .app} -- the Mac analog of the iOS and Windows
 * device builds.
 *
 * <p>Distinct from {@link BuildMacDesktopMojo}, which bundles the JVM/JavaSE app
 * ({@code mac-os-x-desktop}). There is deliberately no wrapper for Mac Catalyst:
 * Catalyst IS an iOS build, turned on with the {@code macNative.enabled} hint, so
 * it is reached through the iOS wrappers and giving it one of its own would be a
 * second spelling for what the hint already says.</p>
 *
 * <p>Filled a uniformity gap: {@code windows-device} had a wrapper mojo + IDE entry
 * but the equivalent native-Mac target was reachable only by hand.</p>
 */
@Mojo(name="buildMacNative", requiresDependencyResolution = ResolutionScope.NONE,
        requiresDependencyCollection = ResolutionScope.NONE)
public class BuildMacNativeMojo extends AbstractBuildWrapperMojo {
    @Override
    protected String getPlatform() {
        return "ios";
    }

    @Override
    protected String getBuildTarget() {
        return "mac-os-x-native";
    }
}
