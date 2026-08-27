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

/// The look a build asks the platform for.
///
/// ONE enum for every hint that selects a theme, not one per platform. The
/// domains overlap almost entirely and differ in a constant or two, which is not
/// a reason to make a developer learn three names for `modern` -- and three
/// enums drift, which is worse than the thing they were separated to prevent.
///
/// Where a hint really does accept less than all of this, its attribute says so
/// with `@Hint(valuePattern = ...)` and the annotation processor refuses the
/// rest. That is a build error naming the constant and the hint, which is what
/// the old arrangement could not give: passing a constant the builder does not
/// recognise is not an error there, it is a silent fallback to the default.
public enum ThemeMode {
    /// Say nothing, and let the build server apply its own default.
    @HintUnset
    DEFAULT,

    /// Follow the device's own light/dark setting.
    @HintValue("auto")
    AUTO,

    /// The current platform look.
    @HintValue(value = "modern", accepts = {"liquid", "material"})
    MODERN,

    /// The flat iOS 7 look.
    @HintValue(value = "ios7", accepts = {"flat"})
    IOS7,

    /// The Android Holo light look.
    @HintValue(value = "hololight", accepts = {"holo"})
    HOLOLIGHT,

    /// The pre-modern look for the platform.
    @HintValue(value = "legacy", accepts = {"iphone"})
    LEGACY,

    /// The theme the application ships, rather than a platform one.
    @HintValue("custom")
    CUSTOM;
}
