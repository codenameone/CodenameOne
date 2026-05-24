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
package com.codename1.media;

/// Tuning knobs for [TextToSpeech#speak(String, TtsOptions)].
public final class TtsOptions {
    private String languageTag;
    private String voiceId;
    private float rate = 1.0f;
    private float pitch = 1.0f;
    private float volume = 1.0f;

    /// BCP-47 language tag (`"en-US"`, `"ja-JP"` etc.). When null,
    /// the device's default voice is used.
    public TtsOptions setLanguageTag(String tag) {
        this.languageTag = tag;
        return this;
    }

    public String getLanguageTag() {
        return languageTag;
    }

    /// Platform-specific voice identifier obtained from
    /// [TextToSpeech#getAvailableVoices()]. When null the default
    /// voice for the language tag is used.
    public TtsOptions setVoiceId(String id) {
        this.voiceId = id;
        return this;
    }

    public String getVoiceId() {
        return voiceId;
    }

    /// Speaking rate. `1.0` is the platform default; `0.5` is half
    /// speed; `2.0` is double. Clamped per-platform.
    public TtsOptions setRate(float r) {
        this.rate = r;
        return this;
    }

    public float getRate() {
        return rate;
    }

    /// Pitch multiplier. `1.0` is the platform default. Clamped
    /// per-platform.
    public TtsOptions setPitch(float p) {
        this.pitch = p;
        return this;
    }

    public float getPitch() {
        return pitch;
    }

    /// Output volume in `[0.0, 1.0]`.
    public TtsOptions setVolume(float v) {
        this.volume = v;
        return this;
    }

    public float getVolume() {
        return volume;
    }
}
