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
 * Applies a base theme before the test runs &mdash; the same effect as the
 * simulator's "Native Theme" menu, but scoped to a single test (or to a
 * class when placed at the class level).
 *
 * <p>Pick the theme one of two ways:
 *
 * <ul>
 *   <li>{@link #nativeTheme()} &mdash; one of the {@link NativeTheme}
 *       constants, which map to the .res files bundled into the simulator
 *       jar. This is the recommended form for cross-platform look-and-feel
 *       coverage:
 *       <pre>
 * &#64;Test &#64;Theme(nativeTheme = NativeTheme.IOS_MODERN)        void rendersUnderIosModern()    { ... }
 * &#64;Test &#64;Theme(nativeTheme = NativeTheme.ANDROID_MATERIAL)  void rendersUnderAndroid()      { ... }
 * &#64;Test &#64;Theme(nativeTheme = NativeTheme.IOS_FLAT)          void rendersUnderIosFlat()      { ... }
 * </pre></li>
 *   <li>{@link #value()} &mdash; a classpath path to any {@code .res} file
 *       containing a theme. Use this for app themes shipped under
 *       {@code src/main/resources} or {@code src/test/resources}:
 *       <pre>
 * &#64;Test &#64;Theme("/MyAppTheme.res")  void rendersAppTheme() { ... }
 * </pre></li>
 * </ul>
 *
 * <p>When both are set the {@link #nativeTheme()} wins and {@link #value()}
 * is ignored. When neither is set the annotation is a no-op &mdash; the
 * same as not annotating at all. The selected resource is loaded via
 * {@code Resources.open(...)} and the first theme inside the resource is
 * installed via {@code UIManager.setThemeProps(...)} on the EDT, after
 * which the active form is refreshed.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Theme {
    /**
     * Classpath path to a {@code .res} file (must start with {@code /}).
     * Defaults to the empty string, which means "use {@link #nativeTheme()}
     * if set, otherwise treat this annotation as a no-op".
     */
    String value() default "";

    /**
     * One of the native themes bundled into the simulator jar. Defaults to
     * {@link NativeTheme#NONE}, which means "fall back to {@link #value()}".
     */
    NativeTheme nativeTheme() default NativeTheme.NONE;
}
