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
package com.codename1.hardening;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

/** The global ProGuard flags, especially the platform-dependent preverification. */
public class BuiltinKeepRulesTest {

    @Test
    public void realJvmTargetsKeepStackMapFrames() {
        // On JavaSE/desktop the hardened classes run on a real JVM, so -dontpreverify must be omitted
        // or a class ProGuard emitted unchanged (no frames) throws VerifyError on Java 7+.
        List<String> javase = BuiltinKeepRules.flags("javase");
        assertFalse("JavaSE output must be preverified", javase.contains("-dontpreverify"));
        assertFalse("desktop output must be preverified",
                BuiltinKeepRules.flags("desktop").contains("-dontpreverify"));
    }

    @Test
    public void translatedTargetsSkipPreverification() {
        // The ParparVM ports translate to C and JS, so their frames are never JVM-verified;
        // -dontpreverify stays (preverifying would only cost time).
        assertTrue(BuiltinKeepRules.flags("ios").contains("-dontpreverify"));
        assertTrue(BuiltinKeepRules.flags("mac").contains("-dontpreverify"));
        assertTrue(BuiltinKeepRules.flags("javascript").contains("-dontpreverify"));
        assertTrue("the no-arg default keeps the historical behaviour",
                BuiltinKeepRules.flags().contains("-dontpreverify"));
    }

    @Test
    public void packageNamesAreNotKept() {
        // Codename One has no getResource for nested packages, so package names are obfuscated too;
        // -keeppackagenames must NOT be present.
        for (String p : new String[] {"ios", "javase", "javascript", "win"}) {
            assertFalse("packages must be obfuscated for " + p,
                    BuiltinKeepRules.flags(p).contains("-keeppackagenames"));
        }
    }

    @Test
    public void lineNumbersKeptButSourceFileStripped() {
        // Retracing needs LineNumberTable; SourceFile is stripped (the retrace synthesizes the file
        // name from the class), matching DexGuard.
        for (String p : new String[] {"ios", "javase", "and"}) {
            boolean lineKept = false;
            boolean sourceKept = false;
            for (String f : BuiltinKeepRules.flags(p)) {
                if (f.startsWith("-keepattributes")) {
                    lineKept = f.contains("LineNumberTable");
                    sourceKept = f.contains("SourceFile");
                }
            }
            assertTrue("LineNumberTable kept for " + p, lineKept);
            assertFalse("SourceFile stripped for " + p, sourceKept);
        }
    }

    @Test
    public void keepsNameBoundBackgroundCallbacks() {
        // Background callbacks the OS restarts by their persisted class name (Geofence, background
        // location, background fetch) resolve the app's listener via Class.forName + newInstance, so
        // renaming one silently stops the callback. Their implementors must be kept.
        List<String> rules = BuiltinKeepRules.rules("com.example.MyApp");
        assertTrue(rules.contains(
                "-keep class * implements com.codename1.location.GeofenceListener { *; }"));
        assertTrue(rules.contains(
                "-keep class * implements com.codename1.location.LocationListener { *; }"));
        assertTrue(rules.contains(
                "-keep class * implements com.codename1.background.BackgroundFetch { *; }"));
        // The same rules are exported to R8 on Android (where R8 does the renaming).
        assertTrue(BuiltinKeepRules.forR8("com.example.MyApp").contains(
                "-keep class * implements com.codename1.location.GeofenceListener { *; }"));
    }
}
