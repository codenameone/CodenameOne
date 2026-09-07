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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The builder's arithmetic on a minor-versioned Android platform.
 *
 * <p>Android 17 is the first release that ships no unsuffixed platform:
 * sdkmanager offers android-37.0, android-37.1 and android-37.2 and no
 * "android-37" at all. The name is what the SDK list gives the builder, so
 * from API 37 onward the platform string it carries around has a minor in it
 * for the first time, and the arithmetic below predates that.</p>
 */
class AndroidMinorPlatformVersionTest {

    /**
     * The build-tools cap in {@link AndroidGradleBuilder#compileSdkInt}.
     *
     * <p>The ladder maps a build-tools version back to the compile SDK it can
     * drive. It stopped at 36, so build-tools 37 matched no rung and the cap
     * quietly stopped applying -- the compile SDK then came from the newest
     * installed platform with nothing holding it down.</p>
     */
    @Test
    void buildTools37CapsTheCompileSdkAt37() {
        assertEquals(37, AndroidGradleBuilder.compileSdkInt("38", "37.0.0", "28",
                false, false, false, false));
    }

    /**
     * The rung below still has to behave, or the new one is untested luck.
     */
    @Test
    void buildTools36StillCapsAt36() {
        assertEquals(36, AndroidGradleBuilder.compileSdkInt("38", "36.0.0", "28",
                false, false, false, false));
    }

    /**
     * A platform name carrying a minor used to make this answer 0.
     *
     * <p>{@code compileSdkInt} ended in {@code Integer.parseInt}, which throws
     * on "37.2" and was caught into a 0 return meaning "could not be
     * determined". Every caller reads that as "impose no floor", so the
     * manifest fragments lost the compile SDK they check attribute values
     * against.</p>
     *
     * <p>The build-tools version here deliberately matches no rung of the
     * ladder, so the platform string reaches the end of the method unreduced
     * -- which is the path that used to throw. Reducing it at the call site
     * would have left this one intact.</p>
     */
    @Test
    void aMinorVersionedPlatformStillYieldsAnApiLevel() {
        assertEquals(37, AndroidGradleBuilder.compileSdkInt("37.2", "38.0.0",
                "28", false, false, false, false));
    }

    /**
     * The minor must not be folded into the number itself.
     *
     * <p>Gathering the digits of "37.2" and then parsing them gives 372, which
     * is greater than every floor in the class -- so the ranging floor below
     * would compare satisfied, leave the string alone, and then fail the final
     * parse into the same 0. Both halves have to drop the minor first, and
     * this is the case that tells the two implementations apart: it answers 37
     * now and answered 0 before.</p>
     */
    @Test
    void aMinorVersionedPlatformSurvivesAFeatureFloor() {
        assertEquals(37, AndroidGradleBuilder.compileSdkInt("37.2", "38.0.0",
                "28", true, true, false, false));
    }

    /**
     * The value AGP reads to stop warning about an untested compile SDK.
     *
     * <p>AGP names the platform it resolved rather than the number requested,
     * so at API 37 it asks for "37.0" and the bare "37" the build requested
     * suppressed nothing. The property is a comma-separated list, so both
     * spellings go in and whichever one AGP resolves to matches.</p>
     */
    @Test
    void theSuppressionKeyCarriesBothSpellings() {
        String value = AndroidGradleBuilder.suppressUnsupportedCompileSdkValue(37);
        assertEquals("37,37.0", value);
        assertTrue(value.contains("37.0"),
                "AGP 8.13.2 asks for the resolved platform name, which is 37.0");
    }
}
