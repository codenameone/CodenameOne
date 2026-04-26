/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui.animations;

/// Pluggable time source for animations across the framework. Defaults to
/// `System.currentTimeMillis()` but can be overridden with an explicit value to
/// support deterministic playback (e.g. UI tests), to advance animations at a
/// custom pace (slow-motion, fast-forward), or to step through animation frames
/// manually.
///
/// All methods are static and the class holds only primitive state to keep the
/// hot path cheap — `now()` is invoked from every animation tick.
///
/// @author Shai Almog
public final class AnimationTime {
    private static long overrideTime;
    private static boolean overridden;

    private AnimationTime() {
    }

    /// Returns the current animation time in milliseconds. Returns
    /// `System.currentTimeMillis()` unless an override has been set via
    /// [setTime(long)][#setTime(long)].
    ///
    /// #### Returns
    ///
    /// the current animation time in milliseconds
    public static long now() {
        if (overridden) {
            return overrideTime;
        }
        return System.currentTimeMillis();
    }

    /// Overrides the value returned by [now()][#now()]. Once set, every animation
    /// reading the clock will see the same `time` value until either this method
    /// is called again or [reset()][#reset()] is invoked. Advancing animations
    /// while overridden requires repeatedly calling this method with increasing
    /// values.
    ///
    /// #### Parameters
    ///
    /// - `time`: the time in milliseconds that [now()][#now()] should return
    public static void setTime(long time) {
        overrideTime = time;
        overridden = true;
    }

    /// Clears any override and restores [now()][#now()] to delegate to
    /// `System.currentTimeMillis()`.
    public static void reset() {
        overridden = false;
    }

    /// Returns true when an override time is currently active.
    ///
    /// #### Returns
    ///
    /// true when [now()][#now()] is returning an overridden value
    public static boolean isOverridden() {
        return overridden;
    }
}
