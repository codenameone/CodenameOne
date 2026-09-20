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
package com.codename1.annotations.buildhints;

/// Which iOS design generation the modern theme targets.
///
/// Separate from [ThemeMode] rather than more constants on it, because the two
/// answer different questions and compose: `themeMode` picks the LOOK -- modern,
/// iOS 7, pre-flat -- and this picks which generation of the modern look.
/// Folding `modern27` into ThemeMode would have made every combination a new
/// constant, on an enum already shared with Android and the desktop.
///
/// Consulted only when the theme resolves to modern/liquid. Every other
/// themeMode ignores it, because there is only one iOS 7 and one pre-flat look.
public enum IosThemeGeneration {
    /// Say nothing, and let the build server apply its own default, which is
    /// [#IOS26]. An application that never sets this keeps the theme it has.
    @HintUnset
    DEFAULT,

    /// The iOS 26 Liquid Glass look, shipped as `iOSModernTheme.res`.
    @HintValue("26")
    IOS26,

    /// The iOS 27 look, shipped as `iOSModern27Theme.res`.
    ///
    /// It is a real design change rather than a version bump: 31 of the 68
    /// native reference tiles differ from iOS 26, concentrated in the Liquid
    /// Glass surfaces and the bar and button chrome built on them.
    @HintValue("27")
    IOS27;
}
