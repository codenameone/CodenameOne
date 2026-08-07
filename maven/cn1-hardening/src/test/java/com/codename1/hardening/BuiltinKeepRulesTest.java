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
    public void lineTablesAreAlwaysKept() {
        // Retracing depends on SourceFile + LineNumberTable regardless of platform.
        for (String p : new String[] {"ios", "javase", "and"}) {
            boolean kept = false;
            for (String f : BuiltinKeepRules.flags(p)) {
                if (f.contains("SourceFile") && f.contains("LineNumberTable")) {
                    kept = true;
                }
            }
            assertTrue("line tables kept for " + p, kept);
        }
    }
}
