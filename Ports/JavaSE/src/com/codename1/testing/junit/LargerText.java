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
package com.codename1.testing.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Runs the test with the simulator's accessibility text-scale set to
 * {@link #scale()}. Mirrors the Simulate &gt; Larger Text menu (AX2, AX3,
 * AX5...) and exists so layout regressions at larger font sizes can be
 * caught in a regular test run instead of by hand.
 *
 * <p>The default scale is {@code 1.3f}, which corresponds to the menu's
 * AX2 (the first step above default). Setting {@link #scale()} back to
 * {@code 1.0f} on a per-method annotation restores the default size for
 * that one test while a class-level annotation keeps the rest scaled.
 *
 * <pre>
 * &#64;CodenameOneTest
 * &#64;LargerText                 // class-level: 1.3x for every test
 * class AccessibilityTest {
 *
 *     &#64;Test
 *     void buttonsStillFit() { ... }
 *
 *     &#64;Test
 *     &#64;LargerText(scale = 2.0f) // method-level override: AX5
 *     void buttonsAtExtremeScale() { ... }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface LargerText {
    /**
     * Text-scale multiplier. {@code 1.0f} disables the larger-text mode.
     */
    float scale() default 1.3f;
}
