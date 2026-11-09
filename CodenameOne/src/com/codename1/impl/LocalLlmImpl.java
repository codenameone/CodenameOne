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
package com.codename1.impl;

/// Port contract behind `com.codename1.ai.LlmClient.local`: llama.cpp running
/// a GGUF model on the device.
///
/// Every method is blocking and is called from one worker thread per client,
/// never from the EDT. A port returns an instance only when the llama.cpp
/// library is linked into the application, which the builders do only for
/// applications that call `LlmClient.local`.
///
/// Handles are positive integers; `0` means the call failed and
/// [#getLastError(int)] with `0` describes why.
///
/// @hidden
public abstract class LocalLlmImpl {

    /// Loads a model.
    ///
    /// @param modelPath absolute native file system path of a `.gguf` file
    /// @param contextSize context window in tokens, `0` for the model's default
    /// @param gpuLayers layers to offload to the GPU, `-1` for all
    /// @param threads CPU threads, `0` for the port's default
    /// @return a handle, or `0` on failure
    public abstract int open(String modelPath, int contextSize, int gpuLayers, int threads);

    /// Describes the last failure.
    ///
    /// @param handle a handle, or `0` for the last failed [#open]
    /// @return the message, or `null` when there is none
    public abstract String getLastError(int handle);

    /// Starts a generation, replacing any that is in progress. The prompt is
    /// rendered by the chat template embedded in the model file.
    ///
    /// @param handle model handle
    /// @param roles `system`, `user` or `assistant`, one per message
    /// @param contents the text of each message
    /// @param maxTokens upper bound on generated tokens
    /// @param temperature sampling temperature, `0` for greedy
    /// @param topP nucleus sampling threshold
    /// @param seed sampling seed, `-1` for a random one
    /// @param grammar a GBNF grammar the output must match, or `null`
    /// @return `false` when the prompt could not be evaluated
    public abstract boolean start(int handle, String[] roles, String[] contents,
            int maxTokens, float temperature, float topP, int seed, String grammar);

    /// Generates the next piece of text.
    ///
    /// @param handle model handle
    /// @return the next piece (possibly empty while a multi-byte character is
    /// incomplete), or `null` once generation has ended
    public abstract String next(int handle);

    /// @param handle model handle
    /// @return tokens in the prompt of the last [#start]
    public abstract int getPromptTokens(int handle);

    /// @param handle model handle
    /// @return tokens generated since the last [#start]
    public abstract int getCompletionTokens(int handle);

    /// Computes a mean-pooled, L2-normalized embedding.
    ///
    /// @param handle model handle
    /// @param text the input
    /// @return the vector, or `null` on failure
    public abstract float[] embed(int handle, String text);

    /// Frees the model.
    ///
    /// @param handle model handle
    public abstract void close(int handle);
}
