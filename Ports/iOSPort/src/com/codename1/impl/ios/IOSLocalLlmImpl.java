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
package com.codename1.impl.ios;

import com.codename1.impl.LocalLlmImpl;

/// llama.cpp on iOS, over `CN1Llama.m`. The natives are compiled in only when
/// the builder links the llama.cpp xcframework (INCLUDE_CN1_LLAMA), which it
/// does for applications that call `LlmClient.local`; otherwise
/// `cn1LlamaIsSupported` answers false and no instance is created.
final class IOSLocalLlmImpl extends LocalLlmImpl {
    @Override
    public int open(String modelPath, int contextSize, int gpuLayers, int threads) {
        return IOSImplementation.nativeInstance.cn1LlamaOpen(modelPath, contextSize,
                gpuLayers, threads);
    }

    @Override
    public String getLastError(int handle) {
        return IOSImplementation.nativeInstance.cn1LlamaLastError(handle);
    }

    @Override
    public boolean start(int handle, String[] roles, String[] contents, int maxTokens,
            float temperature, float topP, int seed, String grammar) {
        return IOSImplementation.nativeInstance.cn1LlamaStart(handle, roles, contents,
                maxTokens, temperature, topP, seed, grammar);
    }

    @Override
    public String next(int handle) {
        return IOSImplementation.nativeInstance.cn1LlamaNext(handle);
    }

    @Override
    public int getPromptTokens(int handle) {
        return IOSImplementation.nativeInstance.cn1LlamaPromptTokens(handle);
    }

    @Override
    public int getCompletionTokens(int handle) {
        return IOSImplementation.nativeInstance.cn1LlamaCompletionTokens(handle);
    }

    @Override
    public float[] embed(int handle, String text) {
        return IOSImplementation.nativeInstance.cn1LlamaEmbed(handle, text);
    }

    @Override
    public void close(int handle) {
        IOSImplementation.nativeInstance.cn1LlamaClose(handle);
    }
}
