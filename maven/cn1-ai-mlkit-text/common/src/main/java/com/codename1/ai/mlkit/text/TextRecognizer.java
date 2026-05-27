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

package com.codename1.ai.mlkit.text;

import com.codename1.ai.LlmException;
import com.codename1.system.NativeLookup;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

/// ML Kit Text Recognition (OCR).
///
/// Extracts text strings from images entirely on-device via Google's ML Kit.
/// Bridges to `GoogleMLKit/TextRecognition` on iOS and
/// `com.google.mlkit:text-recognition` on Android.
///
public final class TextRecognizer {
    private TextRecognizer() { }

    /// True only when the running platform has a native bridge wired up.
    public static boolean isSupported() {
        NativeTextRecognizer bridge = NativeLookup.create(NativeTextRecognizer.class);
        return bridge != null && bridge.isSupported();
    }

    /// Runs OCR on the supplied image bytes (JPEG or PNG). Completes with
    /// the recognised text. Empty image -> empty string. No text -> empty
    /// string. Hard errors fire `AsyncResource.error(...)`.
    public static AsyncResource<String> recognize(final byte[] imageBytes) {
        final AsyncResource<String> out = new AsyncResource<String>();
        if (imageBytes == null || imageBytes.length == 0) {
            Display.getInstance().callSerially(new Runnable() {
                @Override public void run() { out.complete(""); }
            });
            return out;
        }
        final NativeTextRecognizer bridge = NativeLookup.create(NativeTextRecognizer.class);
        if (bridge == null || !bridge.isSupported()) {
            out.error(new LlmException(
                    "TextRecognizer.recognize is not supported on this platform.",
                    -1, null, null, null, LlmException.ErrorType.UNKNOWN));
            return out;
        }
        Display.getInstance().scheduleBackgroundTask(new Runnable() {
            @Override public void run() {
                try {
                    final String result = bridge.recognize(imageBytes);
                    Display.getInstance().callSerially(new Runnable() {
                        @Override public void run() { out.complete(result == null ? "" : result); }
                    });
                } catch (final Throwable t) {
                    Display.getInstance().callSerially(new Runnable() {
                        @Override public void run() {
                            out.error(new LlmException("TextRecognizer.recognize failed: " + t.getMessage(),
                                    -1, null, null, t, LlmException.ErrorType.UNKNOWN));
                        }
                    });
                }
            }
        });
        return out;
    }
}
