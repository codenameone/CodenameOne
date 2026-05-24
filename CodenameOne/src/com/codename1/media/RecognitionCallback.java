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

/// Listener for [SpeechRecognizer] results. Every method is invoked
/// on the EDT.
public interface RecognitionCallback {
    /// Best-effort transcript while the user is still speaking. May
    /// fire many times before [#onResult]. Skip overriding if
    /// `RecognitionOptions.setPartialResults(false)` was passed.
    void onPartialResult(String transcript);

    /// Final transcript for a single utterance. `confidence` is in
    /// `[0.0, 1.0]` when the platform supplies one, or `-1` when it
    /// doesn't. `alternatives` may be empty.
    void onResult(String transcript, float confidence, String[] alternatives);

    /// Recognition session ended (timeout, mic released, or
    /// `SpeechRecognizer.stop()`). No more callbacks will fire.
    void onEnd();

    /// Recognition failed (no permission, no network for online
    /// engines, hardware error).
    void onError(Throwable t);

    /// No-op adapter. Subclass and override only what you need.
    public static class Adapter implements RecognitionCallback {
        public void onPartialResult(String transcript) {
        }

        public void onResult(String transcript, float confidence, String[] alternatives) {
        }

        public void onEnd() {
        }

        public void onError(Throwable t) {
        }
    }
}
