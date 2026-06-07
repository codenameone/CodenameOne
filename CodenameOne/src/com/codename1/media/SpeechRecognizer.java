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

/// On-device speech-to-text.
///
/// - **iOS** -- backed by `SFSpeechRecognizer`. The first call
///   prompts the user for microphone + speech-recognition permission
///   (the latter requires `NSSpeechRecognitionUsageDescription` in
///   Info.plist, which the build server injects automatically when
///   this class is referenced).
/// - **Android** -- backed by `android.speech.SpeechRecognizer`. May
///   use Google's cloud speech endpoint on older devices; on Pixel
///   and modern flagships it runs fully on-device.
/// - **JavaSE simulator** -- no built-in implementation. Add
///   `cn1-ai-whisper` to enable on-device transcription via
///   whisper.cpp, or expect [#isSupported] to return false.
///
/// ```
/// if (SpeechRecognizer.isSupported()) {
///     SpeechRecognizer.recognizeOnce(new RecognitionCallback.Adapter() {
///         public void onResult(String t, float c, String[] a) {
///             form.findTextField().setText(t);
///         }
///     });
/// }
/// ```
public final class SpeechRecognizer {

    private SpeechRecognizer() {
    }

    /// True when the current platform implements on-device or
    /// platform-bundled speech recognition. Even when true, the user
    /// may still deny permission at runtime.
    public static boolean isSupported() {
        return Display.getInstance().isSpeechRecognitionSupported();
    }

    /// Captures one utterance with default options
    /// ([RecognitionOptions] `en-US`, partial results on, max 1
    /// alternative). Convenience wrapper around [#recognize].
    public static void recognizeOnce(RecognitionCallback callback) {
        recognize(new RecognitionOptions(), callback);
    }

    /// Starts a recognition session. Use
    /// [RecognitionOptions#setContinuous(boolean)] to keep listening
    /// across silences. Call [#stop()] to end a continuous session.
    public static void recognize(RecognitionOptions options, RecognitionCallback callback) {
        if (options == null) {
            options = new RecognitionOptions();
        }
        if (callback == null) {
            throw new IllegalArgumentException("callback is required");
        }
        Display.getInstance().startSpeechRecognition(options, callback);
    }

    /// Stops the active recognition session, if any. No-op when none.
    public static void stop() {
        Display.getInstance().stopSpeechRecognition();
    }
}
