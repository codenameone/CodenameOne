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
 * Sets a property visible to the Codename One simulator before a test runs.
 *
 * <p>The {@link #scope() scope} controls when and where the value lands:
 * <ul>
 *   <li>{@link Scope#DISPLAY} (default) &mdash; applied via
 *       {@code Display.getInstance().setProperty(name, value)} after Display
 *       has been initialized. Use this for properties your app reads via
 *       {@code Display.getProperty(...)}.</li>
 *   <li>{@link Scope#SYSTEM} &mdash; applied via {@code System.setProperty}
 *       <em>before</em> Display is initialized. Use this when the simulator's
 *       startup needs to see the value (e.g. {@code java.awt.headless}).</li>
 * </ul>
 *
 * <p>For visual / theming concerns prefer the dedicated annotations
 * ({@link Theme}, {@link DarkMode}, {@link LargerText}, {@link Orientation},
 * {@link RTL}) &mdash; they apply on the EDT and trigger the right refresh
 * sequence. Build hints (the {@code codename1.arg.*} keys consumed by the
 * Maven plugin) are intentionally not supported here: they only mean
 * something to the build server, not to runtime code.
 *
 * <p>The annotation can be placed on a test class (applies to every test in
 * that class) or on a single {@code @Test} method (applies just to that
 * method, after any class-level properties). To set more than one property
 * on the same target wrap them in {@link SimulatorProperties} &mdash; the
 * source level of this module predates {@code @Repeatable}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface SimulatorProperty {

    /** Property key. */
    String name();

    /** Property value. */
    String value();

    /** Where the property is applied. Defaults to {@link Scope#DISPLAY}. */
    Scope scope() default Scope.DISPLAY;

    /**
     * Where a {@link SimulatorProperty} value is applied.
     */
    enum Scope {
        /**
         * Set via {@code Display.getInstance().setProperty(name, value)}
         * after Display init &mdash; the typical case for properties your
         * app reads with {@code Display.getProperty}.
         */
        DISPLAY,

        /**
         * Set via {@code System.setProperty(name, value)} before Display
         * init. Use for things the simulator reads at startup.
         */
        SYSTEM
    }
}
