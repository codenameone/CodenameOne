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

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The tri-state parsing of {@code harden.*} boolean arguments must match the engine's
 * {@code HardeningConfig.boolTri}. The regression: a bare {@code "false".equals(...)} in the Android
 * builder recognized only the literal {@code false}, so the documented aliases {@code harden.rename=off}
 * and {@code harden.rename=0} were misread as "renaming still requested" and the build was rejected
 * with R8 disabled even though the engine had disabled renaming.
 */
class HardeningBooleanArgTest {

    /** Executor is abstract; only hardenBoolArg is under test. */
    private static final class Probe extends Executor {
        @Override
        public boolean build(File sourceZip, BuildRequest request) {
            return false;
        }

        @Override
        protected String getDeviceIdCode() {
            return "";
        }

        @Override
        protected String generatePeerComponentCreationCode(String methodCallString) {
            return "";
        }

        @Override
        protected String convertPeerComponentToNative(String param) {
            return "";
        }

        boolean parse(String value, boolean def) {
            BuildRequest r = new BuildRequest();
            if (value != null) {
                r.putArgument("harden.rename", value);
            }
            return hardenBoolArg(r, "harden.rename", def);
        }
    }

    @Test
    void offAndZeroReadAsFalseJustLikeFalse() {
        Probe p = new Probe();
        assertFalse(p.parse("false", true), "false");
        assertFalse(p.parse("off", true), "off is a documented alias for false");
        assertFalse(p.parse("0", true), "0 is a documented alias for false");
        assertFalse(p.parse("OFF", true), "case-insensitive");
    }

    @Test
    void truthyAndDefaultsBehaveAsExpected() {
        Probe p = new Probe();
        assertTrue(p.parse("true", false), "true");
        assertTrue(p.parse("on", false), "on");
        assertTrue(p.parse("1", false), "1");
        // Unset and unrecognized both fall back to the default rather than flipping to false.
        assertTrue(p.parse(null, true), "unset -> default");
        assertTrue(p.parse("", true), "blank -> default");
        assertTrue(p.parse("maybe", true), "unrecognized -> default");
        assertFalse(p.parse("maybe", false), "unrecognized -> default (false)");
    }

    @Test
    void parparvmCTargetsAreRecognizedForRuntimeLiteralExclusion() {
        // The parparvm-java-api.jar runtime-literal exclusion applies exactly to the ParparVM-to-C
        // targets, where a compile-time literal is a constant-pool object that is never interned; the
        // DEX/JVM/JS targets intern their compile-time literals so no exclusion is needed there.
        Probe p = new Probe();
        for (String t : new String[] {"ios", "mac", "watch", "tv", "win", "linux"}) {
            assertTrue(p.isParparVMCPlatform(t), t + " translates to C via ParparVM");
        }
        for (String t : new String[] {"and", "android", "javase", "desktop", "javascript"}) {
            assertFalse(p.isParparVMCPlatform(t), t + " interns its compile-time literals");
        }
    }
}
