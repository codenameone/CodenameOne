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

/// `UnityEngine.AudioClip`: a sound of the project, by the name of the
/// resource it ships as.
///
/// The scene compiler reads what it can of the file -- a WAV says how long
/// it is and at what rate -- so that a script asking for `length` gets an
/// answer without the sound being opened here. A compressed file's length
/// is not known until something decodes it and reads as zero.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class AudioClip extends Object {
    /// The resource the sound ships as.
    public final String resource;
    private final float length;
    private final int samples;
    private final int channels;
    private final int frequency;

    public AudioClip(String name, String resource, float length, int samples, int channels, int frequency) {
        this.name = name;
        this.resource = resource;
        this.length = length;
        this.samples = samples;
        this.channels = channels;
        this.frequency = frequency;
    }

    public float get_length() {
        return length;
    }

    public int get_samples() {
        return samples;
    }

    public int get_channels() {
        return channels;
    }

    public int get_frequency() {
        return frequency;
    }

    /// Nothing is decoded ahead of time; the answer is that it is ready.
    public boolean LoadAudioData() {
        return true;
    }

    public boolean UnloadAudioData() {
        return true;
    }
}
