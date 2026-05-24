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

import com.codename1.util.AsyncResource;

/// Google Gemini client. The native wire format diverges from OpenAI's:
/// system messages live in `systemInstruction`, content is split into
/// `parts` with `inline_data` / `text`, tool calls arrive atomically
/// at stream end rather than fragment-by-fragment.
///
/// Google publishes an OpenAI-compatibility endpoint at
/// `https://generativelanguage.googleapis.com/v1beta/openai/` that
/// works with [LlmClient#localOpenAiCompatible] today; this dedicated
/// client (which handles the native shape end-to-end) is a follow-up.
class GeminiClient extends LlmClient {
    private final String apiKey;

    GeminiClient(String apiKey, String baseUrl) {
        super(baseUrl);
        this.apiKey = apiKey;
    }

    @Override
    public String getProvider() {
        return "gemini";
    }

    @Override
    public AsyncResource<ChatResponse> chat(ChatRequest req) {
        AsyncResource<ChatResponse> r = new AsyncResource<ChatResponse>();
        r.error(new UnsupportedOperationException(
                "GeminiClient (native) is not yet implemented in this release. "
              + "Use LlmClient.localOpenAiCompatible("
              + "\"https://generativelanguage.googleapis.com/v1beta/openai\", apiKey, model) "
              + "to reach Gemini through Google's OpenAI-compatible shim."));
        return r;
    }

    @Override
    public AsyncResource<ChatResponse> chatStream(ChatRequest req, StreamingListener listener) {
        return chat(req);
    }

    @Override
    public AsyncResource<EmbeddingResponse> embed(EmbeddingRequest req) {
        AsyncResource<EmbeddingResponse> r = new AsyncResource<EmbeddingResponse>();
        r.error(new UnsupportedOperationException(
                "GeminiClient.embed is not yet implemented. Use the OpenAI-compatible shim "
              + "or LlmClient.openai(...) with text-embedding-3-small."));
        return r;
    }

    String getApiKey() {
        return apiKey;
    }
}
