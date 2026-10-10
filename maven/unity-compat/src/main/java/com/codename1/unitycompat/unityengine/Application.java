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

/// `UnityEngine.Application`, as far as a script can tell where it runs.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class Application {
    private static int targetFrameRate = -1;
    private static boolean quit;
    /// A number of `RuntimePlatform`; see [UnityRuntime#platform(int)] for
    /// who sets it and why this is what it starts as.
    private static int platform = UnityRuntime.PLATFORM_LINUX;

    private Application() {
    }

    /// Forgets what a script asked for; the platform is the host's to say
    /// and stays.
    static void reset() {
        targetFrameRate = -1;
        quit = false;
    }

    public static int get_targetFrameRate() {
        return targetFrameRate;
    }

    /// Kept and reported back; the host decides how often a frame is drawn.
    public static void set_targetFrameRate(int value) {
        targetFrameRate = value;
    }

    public static boolean get_isPlaying() {
        return true;
    }

    /// Never: what runs here is a built player, whatever it runs on -- the
    /// simulator included. [#get_platform()] agrees, and is never one of
    /// the editor's members.
    public static boolean get_isEditor() {
        return false;
    }

    static void platform(int runtimePlatform) {
        platform = runtimePlatform;
    }

    /// `Application.platform`, a `RuntimePlatform` by its number. The
    /// runtime has no display to ask, so the host says which
    /// ([UnityRuntime#platform(int)]); `UnityGameView` does, from Codename
    /// One's platform name.
    public static int get_platform() {
        return platform;
    }

    /// True on the two platforms Unity documents as handheld, iOS and
    /// Android, and derived from [#get_platform()] so that the two can never
    /// disagree. A phone's browser is `WebGLPlayer` and answers false:
    /// Codename One's JavaScript port names its platform without saying what
    /// it runs on, and a game that asks this wants to know whether to show
    /// touch controls, for which `Input.touchSupported` is the exact answer
    /// there.
    public static boolean get_isMobilePlatform() {
        return platform == UnityRuntime.PLATFORM_IOS || platform == UnityRuntime.PLATFORM_ANDROID;
    }

    /// Recorded for the host to act on: see [UnityRuntime#quitRequested].
    public static void Quit() {
        quit = true;
    }

    static boolean quitRequested() {
        return quit;
    }
}
