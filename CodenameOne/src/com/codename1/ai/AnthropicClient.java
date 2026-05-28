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

/// Anthropic Messages client.
///
/// Talks to `https://api.anthropic.com/v1/chat/completions`, the
/// official OpenAI-compatible Messages shim. The wire format on that
/// endpoint is the same `/v1/chat/completions` shape that OpenAI and
/// every OpenAI-compatible provider speaks, so this client inherits the
/// full streaming, tool-call, response-format and image-attachment
/// implementation from `OpenAiClient`. Only the provider name, default
/// model, and the embeddings shape (Anthropic does not publish a
/// first-party embeddings endpoint) are overridden here.
///
/// Authentication uses `Authorization: Bearer sk-ant-...` -- identical
/// header layout to OpenAI -- which is why the inherited request
/// configuration works without modification.
class AnthropicClient extends OpenAiClient {

    AnthropicClient(String apiKey, String baseUrl) {
        super(apiKey, baseUrl);
        setDefaultModel("claude-sonnet-4-5");
    }

    @Override
    public String getProvider() {
        return "anthropic";
    }

    @Override
    public AsyncResource<EmbeddingResponse> embed(EmbeddingRequest req) {
        AsyncResource<EmbeddingResponse> r = new AsyncResource<EmbeddingResponse>();
        r.error(new UnsupportedOperationException(
                "Anthropic does not publish a first-party embeddings endpoint. "
                + "Use a Voyage AI key via LlmClient.localOpenAiCompatible("
                + "\"https://api.voyageai.com/v1\", key, \"voyage-3\") or "
                + "LlmClient.openai(...) with text-embedding-3-small."));
        return r;
    }
}
