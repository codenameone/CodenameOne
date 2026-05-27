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

import com.codename1.ui.Display;

/// Speaks text aloud using the platform's built-in synthesizer.
///
/// - **iOS** -- `AVSpeechSynthesizer`.
/// - **Android** -- `android.speech.tts.TextToSpeech`.
/// - **JavaSE simulator** -- best-effort: `say` on macOS, `espeak`
///   on Linux, SAPI via PowerShell on Windows. When none of those
///   tools are present, [#isSupported] returns false and [#speak]
///   silently no-ops so simulator code paths keep working.
///
/// No permissions or Info.plist entries are required.
///
/// ```
/// TextToSpeech.speak("Welcome to the demo");
/// ```
public final class TextToSpeech {

    private TextToSpeech() {
    }

    public static boolean isSupported() {
        return Display.getInstance().isTextToSpeechSupported();
    }

    /// Speaks `text` using the platform default voice. Returns
    /// immediately; the utterance plays asynchronously.
    public static void speak(String text) {
        speak(text, null);
    }

    public static void speak(String text, TtsOptions options) {
        if (text == null || text.length() == 0) {
            return;
        }
        Display.getInstance().textToSpeechSpeak(text,
                options == null ? new TtsOptions() : options);
    }

    /// Stops any ongoing utterance. No-op when nothing is playing.
    public static void stop() {
        Display.getInstance().textToSpeechStop();
    }

    /// Returns the platform-supplied voice identifiers. May be empty
    /// on platforms that don't enumerate voices (e.g. the simulator
    /// when relying on the system `say` binary).
    public static String[] getAvailableVoices() {
        return Display.getInstance().textToSpeechAvailableVoices();
    }
}
