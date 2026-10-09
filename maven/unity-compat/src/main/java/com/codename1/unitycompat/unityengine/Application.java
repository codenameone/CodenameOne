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

    private Application() {
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

    public static boolean get_isEditor() {
        return false;
    }

    public static boolean get_isMobilePlatform() {
        return false;
    }

    /// Recorded for the host to act on: see [UnityRuntime#quitRequested].
    public static void Quit() {
        quit = true;
    }

    static boolean quitRequested() {
        return quit;
    }
}
