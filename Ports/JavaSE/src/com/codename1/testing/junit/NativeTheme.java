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

/**
 * Enumerates the native themes bundled into the JavaSE simulator jar, in the
 * exact form they appear under the simulator's <em>Simulate &gt; Native Theme</em>
 * menu. Use these constants with {@link Theme#nativeTheme()} so a test can
 * declare its target look-and-feel by name instead of by resource path.
 *
 * <p>Each constant carries the {@code .res} resource path the
 * {@link CodenameOneExtension} loads at test time via
 * {@code Resources.open(resourcePath())}, plus the human-readable label the
 * simulator menu shows so test reports and review tooling can render them
 * consistently with the simulator UI.
 */
public enum NativeTheme {

    /**
     * Sentinel meaning "no native theme picked" -- the default for
     * {@link Theme#nativeTheme()}. When this is set, the extension falls
     * back to {@link Theme#value()} if a resource path was supplied,
     * otherwise the annotation is a no-op.
     */
    NONE("", ""),

    /** "iOS Modern (Liquid Glass)" -- the iOS Modern look. */
    IOS_MODERN("/iOSModernTheme.res", "iOS Modern (Liquid Glass)"),

    /** "iOS 7 (Flat)" -- the flat post-iOS-7 styling. */
    IOS_FLAT("/iOS7Theme.res", "iOS 7 (Flat)"),

    /** "iPhone (Pre-Flat)" -- the pre-iOS-7 skeuomorphic look. */
    IPHONE_PRE_FLAT("/iPhoneTheme.res", "iPhone (Pre-Flat)"),

    /** "Android Material" -- the Material Design theme. */
    ANDROID_MATERIAL("/AndroidMaterialTheme.res", "Android Material"),

    /** "Android Holo Light" -- the Holo era light theme. */
    ANDROID_HOLO_LIGHT("/android_holo_light.res", "Android Holo Light"),

    /** "Android Legacy" -- the pre-Material Android look. */
    ANDROID_LEGACY("/androidTheme.res", "Android Legacy");

    private final String resourcePath;
    private final String displayName;

    NativeTheme(String resourcePath, String displayName) {
        this.resourcePath = resourcePath;
        this.displayName = displayName;
    }

    /**
     * Returns the classpath path of the {@code .res} file bundled into the
     * simulator jar that backs this native theme, in the form expected by
     * {@code Resources.open(...)} (leading slash).
     */
    public String resourcePath() {
        return resourcePath;
    }

    /**
     * Returns the human-readable label this theme carries in the simulator's
     * <em>Native Theme</em> menu, suitable for inclusion in test reports.
     */
    public String displayName() {
        return displayName;
    }
}
