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
 * Build wrapper for the native Windows (ParparVM -> clang-cl) target. Distinct
 * from {@link BuildWindowsDesktopMojo}, which bundles the JVM/JavaSE app
 * ({@code windows-desktop}); this produces a native Win32 executable
 * ({@code windows-device}), the Windows analog of the iOS device build.
 */
@Mojo(name="buildWindowsDevice", requiresDependencyResolution = ResolutionScope.NONE,
        requiresDependencyCollection = ResolutionScope.NONE)
public class BuildWindowsDeviceMojo extends AbstractBuildWrapperMojo {
    @Override
    protected String getPlatform() {
        // "win", not "windows". This value activates the module profile in the
        // generated project's root pom, and that profile matches the value the
        // win module itself declares -- which is "win". Passing "windows"
        // matched no profile, so the win module never entered the reactor and
        // the wrapper's nested build reported success having produced nothing.
        // Nothing else reads the platform as "windows"; the build TARGET stays
        // "windows-device", which is a separate namespace.
        return "win";
    }

    @Override
    protected String getBuildTarget() {
        return "windows-device";
    }
}
