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
 * Applies a base theme resource before the test runs &mdash; equivalent to
 * the simulator's "Native Theme" menu, but scoped to a single test (or to a
 * class when placed at the class level).
 *
 * <p>The {@link #value()} is the classpath name of a {@code .res} file
 * containing a theme. The native themes bundled into the JavaSE simulator
 * jar can be used directly:
 *
 * <pre>
 * &#64;Test &#64;Theme("/iOSModernTheme.res")   void looksRightOnIos()       { ... }
 * &#64;Test &#64;Theme("/AndroidMaterialTheme.res") void looksRightOnAndroid() { ... }
 * &#64;Test &#64;Theme("/iPhoneTheme.res")     void looksRightOnLegacyIos() { ... }
 * </pre>
 *
 * <p>Custom app themes work too &mdash; ship the {@code .res} file under
 * {@code src/main/resources} or {@code src/test/resources} and reference it
 * with a leading slash. The theme is loaded via
 * {@code Resources.open(value)} and the first theme inside the resource is
 * installed via {@code UIManager.setThemeProps(...)} on the EDT, after
 * which the active form is refreshed.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Theme {
    /**
     * Classpath path to a {@code .res} file (must start with {@code /}).
     */
    String value();
}
