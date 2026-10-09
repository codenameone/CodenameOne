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
package com.codename1.unitycompat.unityengine;

/// `UnityEngine.Time`.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class Time {
    static float deltaTime;
    static float unscaledDeltaTime;
    static float fixedDeltaTime = 0.02f;
    static float time;
    static float unscaledTime;
    static float fixedTime;
    static float timeScale = 1f;
    static int frameCount;
    /// True while `FixedUpdate` runs, where Unity has `deltaTime` answer
    /// with the fixed step.
    static boolean inFixedUpdate;

    private Time() {
    }

    static void reset() {
        deltaTime = 0f;
        unscaledDeltaTime = 0f;
        time = 0f;
        unscaledTime = 0f;
        fixedTime = 0f;
        timeScale = 1f;
        frameCount = 0;
        inFixedUpdate = false;
    }

    public static float get_deltaTime() {
        return inFixedUpdate ? fixedDeltaTime : deltaTime;
    }

    public static float get_unscaledDeltaTime() {
        return unscaledDeltaTime;
    }

    public static float get_fixedDeltaTime() {
        return fixedDeltaTime;
    }

    /// A step of zero or less would never let a frame end, so it is refused.
    public static void set_fixedDeltaTime(float value) {
        if (value > 0f) {
            fixedDeltaTime = value;
        }
    }

    public static float get_time() {
        return inFixedUpdate ? fixedTime : time;
    }

    public static float get_fixedTime() {
        return fixedTime;
    }

    public static float get_unscaledTime() {
        return unscaledTime;
    }

    /// One scene's worth: the clock restarts when a scene is loaded.
    public static float get_timeSinceLevelLoad() {
        return time;
    }

    public static float get_timeScale() {
        return timeScale;
    }

    public static void set_timeScale(float value) {
        timeScale = value < 0f ? 0f : value;
    }

    public static int get_frameCount() {
        return frameCount;
    }
}
