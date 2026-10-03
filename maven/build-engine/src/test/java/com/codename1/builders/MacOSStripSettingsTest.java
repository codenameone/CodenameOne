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

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

/// A Release macOS build is stripped by xcodebuild, before it is signed.
public class MacOSStripSettingsTest {

    @Test
    public void aReleaseBuildIsStripped() {
        assertEquals(Arrays.asList("DEPLOYMENT_POSTPROCESSING=YES", "STRIP_INSTALLED_PRODUCT=YES",
                "STRIP_STYLE=all", "DEBUG_INFORMATION_FORMAT=dwarf-with-dsym"),
                MacOSNativeBuilder.stripSettings("Release"));
        assertEquals(MacOSNativeBuilder.stripSettings("Release"), MacOSNativeBuilder.stripSettings(null));
    }

    @Test
    public void aDebugBuildKeepsItsSymbols() {
        assertEquals(Collections.emptyList(), MacOSNativeBuilder.stripSettings("Debug"));
        assertEquals(Collections.emptyList(), MacOSNativeBuilder.stripSettings(" debug "));
    }
}
