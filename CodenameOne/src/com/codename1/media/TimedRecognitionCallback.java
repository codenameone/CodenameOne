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
package com.codename1.media;

/// Caption-aware [SpeechRecognizer] callback. Platform recognizers
/// that expose segment timing can call these overloads; text-only
/// recognizers continue to use [RecognitionCallback].
public interface TimedRecognitionCallback extends RecognitionCallback {
    /// Called with an interim timed recognition result.
    ///
    /// @param result partial timed recognition result
    void onPartialResult(TranscriptionResult result);

    /// Called with the final timed recognition result.
    ///
    /// @param result final timed recognition result
    /// @param confidence confidence score from `0.0` to `1.0`, when available
    /// @param alternatives provider-specific alternative transcripts
    void onResult(TranscriptionResult result, float confidence, String[] alternatives);

    /// No-op adapter. The timed overloads forward to the string-only
    /// overloads, so subclassing this preserves normal
    /// [RecognitionCallback] behaviour unless the timed methods are
    /// explicitly overridden.
    class Adapter extends RecognitionCallback.Adapter implements TimedRecognitionCallback {
        /// Forwards partial timed results to the string-only partial callback.
        ///
        /// @param result partial timed recognition result
        @Override
        public void onPartialResult(TranscriptionResult result) {
            onPartialResult(result == null ? "" : result.getText());
        }

        /// Forwards final timed results to the string-only result callback.
        ///
        /// @param result final timed recognition result
        /// @param confidence confidence score from `0.0` to `1.0`, when available
        /// @param alternatives provider-specific alternative transcripts
        @Override
        public void onResult(TranscriptionResult result, float confidence, String[] alternatives) {
            onResult(result == null ? "" : result.getText(), confidence, alternatives);
        }
    }
}
