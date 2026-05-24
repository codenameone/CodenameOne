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
package com.codename1.ai;

/// Callback for [LlmClient#chatStream]. Every method is invoked on the
/// EDT; implementations can update UI directly without further
/// marshalling. The terminal [ChatResponse] is delivered via the
/// `AsyncResource` returned by `chatStream`, not via this listener.
///
/// Tool-call streaming differs by provider: OpenAI and Anthropic emit
/// argument JSON in fragments (potentially many `onToolCallDelta`
/// calls per tool); Gemini delivers tool calls atomically at the end
/// (a single call with the complete `argumentsFragment` set to the
/// full JSON). Either pattern is valid.
public interface StreamingListener {
    /// A chunk of assistant text. Append it to whatever text buffer
    /// you're rendering.
    void onContentDelta(String textDelta);

    /// A tool-call fragment. `index` lets you correlate fragments
    /// that belong to the same call when multiple tools are streamed
    /// in parallel. `name` is non-null on the first fragment for each
    /// call. `argumentsFragment` is the next slice of the arguments
    /// JSON; concatenate fragments for the same `index` to reassemble.
    /// `id` is the provider's tool-call id, present on the first
    /// fragment.
    void onToolCallDelta(int index, String id, String name, String argumentsFragment);

    /// Token-accounting update. Most providers send this once at the
    /// end; some send incremental counts.
    void onUsage(Usage usage);

    /// Mid-stream error (e.g. connection reset). The `AsyncResource`
    /// returned by `chatStream` will also complete with this same
    /// exception, so listeners can typically ignore this and react to
    /// the resource. Implemented for parity with other SDKs.
    void onError(Throwable t);

    /// No-op default implementation. Subclass and override only what
    /// you need.
    public static class Adapter implements StreamingListener {
        public void onContentDelta(String textDelta) {
        }

        public void onToolCallDelta(int index, String id, String name, String argumentsFragment) {
        }

        public void onUsage(Usage usage) {
        }

        public void onError(Throwable t) {
        }
    }
}
