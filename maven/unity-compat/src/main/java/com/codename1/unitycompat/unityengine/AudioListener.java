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

/// `UnityEngine.AudioListener`: where the scene is heard from. Sound here
/// is not placed in space, so the listener itself does nothing; its static
/// `volume` scales every sound that starts after it is set.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class AudioListener extends Behaviour {
    private static boolean paused;

    @Override
    public Component $new() {
        return new AudioListener();
    }

    public static float get_volume() {
        return AudioSource.listenerVolume;
    }

    public static void set_volume(float value) {
        AudioSource.listenerVolume = value < 0f ? 0f : value > 1f ? 1f : value;
    }

    /// Kept and reported back.
    public static boolean get_pause() {
        return paused;
    }

    public static void set_pause(boolean value) {
        paused = value;
    }
}
