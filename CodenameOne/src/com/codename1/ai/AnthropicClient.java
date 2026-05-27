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

/// Anthropic /v1/messages client. Wire format differs from OpenAI in
/// three important ways: system messages live in a top-level `system`
/// string rather than a role; image parts use `{type:"image", source:
/// {type:"base64", media_type, data}}`; tool calls stream argument
/// JSON via `input_json_delta` events.
///
/// This is currently a scaffold -- the full request/response mapping
/// is tracked as a follow-up. The class compiles and registers under
/// `LlmClient.anthropic(...)` so app code using the API can be built;
/// runtime calls throw a clear `UnsupportedOperationException`.
class AnthropicClient extends LlmClient {
    private final String apiKey;

    AnthropicClient(String apiKey, String baseUrl) {
        super(baseUrl);
        this.apiKey = apiKey;
    }

    @Override
    public String getProvider() {
        return "anthropic";
    }

    @Override
    public AsyncResource<ChatResponse> chat(ChatRequest req) {
        AsyncResource<ChatResponse> r = new AsyncResource<ChatResponse>();
        r.error(new UnsupportedOperationException(
                "AnthropicClient is not yet implemented in this release. "
                + "Use LlmClient.openai(...) or run the model behind an OpenAI-compatible proxy."));
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
                "Anthropic does not publish a first-party embeddings endpoint. "
                + "Use a Voyage AI key via LlmClient.localOpenAiCompatible(\"https://api.voyageai.com/v1\", key, model)."));
        return r;
    }

    String getApiKey() {
        return apiKey;
    }
}
