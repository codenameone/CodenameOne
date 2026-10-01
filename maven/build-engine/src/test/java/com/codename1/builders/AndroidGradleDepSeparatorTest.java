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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The builder side half of the android.gradleDep separator guard. It has to stand on its own
 * because a project built by an older Maven plugin still arrives with the value already welded
 * together, and the builder is the only thing left that can say so.
 */
class AndroidGradleDepSeparatorTest {

    @Test
    void reportsTheStatementsRunTogether() {
        // Verbatim from the failing build: the project's own two dependencies, with the
        // CN1JailbreakDetect library's rootbeer pin concatenated straight onto the end.
        String excerpt = AndroidGradleBuilder.findUnseparatedGradleStatement(
                "implementation 'co.infinum:goldeneye:1.1.2';"
                        + "implementation 'com.google.firebase:firebase-messaging:23.2.1'"
                        + "implementation 'com.scottyab:rootbeer-lib:0.0.8'");

        assertNotNull(excerpt);
        assertTrue(excerpt.contains("23.2.1'implementation"), excerpt);
    }

    @Test
    void acceptsSeparatedStatements() {
        assertNull(AndroidGradleBuilder.findUnseparatedGradleStatement(
                "implementation 'co.infinum:goldeneye:1.1.2';"
                        + "implementation 'com.google.firebase:firebase-messaging:23.2.1';"
                        + "implementation 'com.scottyab:rootbeer-lib:0.0.8'"));
        assertNull(AndroidGradleBuilder.findUnseparatedGradleStatement(
                "implementation 'a:b:1'\nimplementation 'c:d:2'"));
    }

    @Test
    void acceptsTheOtherDeclarationForms() {
        assertNull(AndroidGradleBuilder.findUnseparatedGradleStatement(
                "implementation(name:'ZBarScannerLibrary', ext:'aar')"));
        assertNull(AndroidGradleBuilder.findUnseparatedGradleStatement(
                "implementation 'com.example:implementation-helper:1.0'"));
        assertNull(AndroidGradleBuilder.findUnseparatedGradleStatement(
                "implementation ('com.facebook.android:facebook-android-sdk:16.0.0')"
                        + "{ exclude module: 'bolts-android' }"));
    }

    @Test
    void toleratesEmptyValues() {
        assertNull(AndroidGradleBuilder.findUnseparatedGradleStatement(null));
        assertNull(AndroidGradleBuilder.findUnseparatedGradleStatement(""));
    }
}
