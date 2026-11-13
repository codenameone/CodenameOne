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
package com.codename1.impl.android.ai;

import com.codename1.impl.LocalLlmImpl;

/// llama.cpp on Android. The native half, `libcn1llama.so`, comes from the
/// `cn1-llama-android` AAR, which the builder adds to the Gradle project only
/// for applications that call `LlmClient.local`; it deletes this source from
/// every other project, so the two cannot drift apart.
///
/// The JNI functions are in `scripts/ai-natives/llama/android/cn1_llama_jni.cpp`,
/// named after this class.
public final class AndroidLocalLlmImpl extends LocalLlmImpl {

    /// Loads the library. Throws when it is absent, which
    /// `AndroidImplementation.createOptionalAiBackend` turns into "no backend".
    public AndroidLocalLlmImpl() {
        System.loadLibrary("cn1llama");
    }

    @Override
    public int open(String modelPath, int contextSize, int gpuLayers, int threads) {
        return nOpen(utf8(modelPath), contextSize, gpuLayers, threads);
    }

    @Override
    public String getLastError(int handle) {
        return string(nLastError(handle));
    }

    @Override
    public boolean start(int handle, String[] roles, String[] contents, int maxTokens,
            float temperature, float topP, int seed, String grammar) {
        byte[][] r = new byte[roles.length][];
        byte[][] c = new byte[contents.length][];
        for (int i = 0; i < roles.length; i++) {
            r[i] = utf8(roles[i]);
            c[i] = utf8(contents[i]);
        }
        return nStart(handle, r, c, maxTokens, temperature, topP, seed,
                grammar == null ? null : utf8(grammar));
    }

    @Override
    public String next(int handle) {
        return string(nNext(handle));
    }

    @Override
    public int getPromptTokens(int handle) {
        return nPromptTokens(handle);
    }

    @Override
    public int getCompletionTokens(int handle) {
        return nCompletionTokens(handle);
    }

    @Override
    public float[] embed(int handle, String text) {
        return nEmbed(handle, utf8(text));
    }

    @Override
    public void close(int handle) {
        nClose(handle);
    }

    // Text crosses JNI as real UTF-8 bytes, never jstring: JNI's string calls
    // speak modified UTF-8, in which every emoji a model emits is invalid,
    // and CheckJNI aborts the process on it.
    private static byte[] utf8(String s) {
        if (s == null) {
            // As the iOS glue does: an absent string is an empty one.
            s = "";
        }
        try {
            byte[] b = s.getBytes("UTF-8");
            byte[] z = new byte[b.length + 1];
            System.arraycopy(b, 0, z, 0, b.length);
            return z;
        } catch (java.io.UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String string(byte[] b) {
        if (b == null) {
            return null;
        }
        try {
            return new String(b, "UTF-8");
        } catch (java.io.UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /// Each `byte[]` argument is NUL-terminated UTF-8; each result is UTF-8
    /// without a terminator, or null.
    private static native int nOpen(byte[] modelPath, int contextSize, int gpuLayers,
            int threads);

    private static native byte[] nLastError(int handle);

    private static native boolean nStart(int handle, byte[][] roles, byte[][] contents,
            int maxTokens, float temperature, float topP, int seed, byte[] grammar);

    private static native byte[] nNext(int handle);

    private static native int nPromptTokens(int handle);

    private static native int nCompletionTokens(int handle);

    private static native float[] nEmbed(int handle, byte[] text);

    private static native void nClose(int handle);
}
