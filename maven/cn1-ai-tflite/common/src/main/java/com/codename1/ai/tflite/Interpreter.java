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

package com.codename1.ai.tflite;

import com.codename1.ai.LlmException;
import com.codename1.system.NativeLookup;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

/// TensorFlow Lite on-device inference.
///
/// Loads a `.tflite` model and runs inference against `float[]` inputs.
/// Bridges to `TensorFlowLiteObjC` on iOS and `org.tensorflow:tensorflow-lite`
/// on Android.
///
public final class Interpreter {
    private Interpreter() { }

    /// True only when the running platform has a native bridge wired up.
    public static boolean isSupported() {
        NativeInterpreter bridge = NativeLookup.create(NativeInterpreter.class);
        return bridge != null && bridge.isSupported();
    }

    /// Loads a TensorFlow Lite model from the supplied bytes and runs
    /// inference against a float32 input tensor. Returns the output as
    /// `float[]`. The model file is held in a native handle keyed by
    /// the SHA-1 of the input bytes; repeated calls reuse the loaded
    /// model.
    public static AsyncResource<float[]> run(final byte[] modelBytes,
                                              final float[] input,
                                              final int outputLength) {
        final AsyncResource<float[]> out = new AsyncResource<float[]>();
        final NativeInterpreter bridge = NativeLookup.create(NativeInterpreter.class);
        if (bridge == null || !bridge.isSupported()) {
            out.error(new LlmException("Interpreter.run is not supported on this platform.",
                    -1, null, null, null, LlmException.ErrorType.UNKNOWN));
            return out;
        }
        Display.getInstance().scheduleBackgroundTask(new Runnable() {
            @Override public void run() {
                try {
                    final float[] r = bridge.run(modelBytes, input, outputLength);
                    Display.getInstance().callSerially(new Runnable() {
                        @Override public void run() { out.complete(r == null ? new float[0] : r); }
                    });
                } catch (final Throwable t) {
                    Display.getInstance().callSerially(new Runnable() {
                        @Override public void run() {
                            out.error(new LlmException("Interpreter.run failed: " + t.getMessage(),
                                    -1, null, null, t, LlmException.ErrorType.UNKNOWN));
                        }
                    });
                }
            }
        });
        return out;
    }
}
