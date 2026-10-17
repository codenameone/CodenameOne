/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided by
 * Oracle in the LICENSE file that accompanied this code.
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

import static org.junit.jupiter.api.Assertions.assertEquals;

class AndroidDesugaringDependencyTest {

    @Test
    void theGradle8BuildDesugarsWithTheNioFlavour() {
        assertEquals("    coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs_nio:2.1.5'\n",
                AndroidGradleBuilder.coreLibraryDesugaringDependency(true, "", ""));
        assertEquals("    coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs_nio:2.1.5'\n",
                AndroidGradleBuilder.coreLibraryDesugaringDependency(true, null, null));
    }

    @Test
    void theLegacyBuildHasNoDesugaring() {
        assertEquals("", AndroidGradleBuilder.coreLibraryDesugaringDependency(false, "", ""));
    }

    @Test
    void aDesugaredLibraryTheProjectDeclaresWins() {
        String[] declared = {
            "    coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:2.0.4'\n",
            "    coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs_minimal:2.1.5'\n",
            "    coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs_nio:2.1.5'\n",
        };
        for (String dep : declared) {
            assertEquals("", AndroidGradleBuilder.coreLibraryDesugaringDependency(true, dep, ""), dep);
            assertEquals("", AndroidGradleBuilder.coreLibraryDesugaringDependency(true, "", dep), dep);
        }
    }
}
