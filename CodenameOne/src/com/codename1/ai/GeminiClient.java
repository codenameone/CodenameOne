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

/// Google Gemini client.
///
/// Talks to `https://generativelanguage.googleapis.com/v1beta/openai/`,
/// Google's official OpenAI-compatible shim. The endpoint accepts the
/// standard `/chat/completions` and `/embeddings` shape -- including
/// streaming, tool calls, multi-modal image parts, and structured
/// JSON output -- so this client inherits the full
/// implementation from `OpenAiClient` and only overrides the provider
/// name and default model.
///
/// Authentication uses `Authorization: Bearer <gemini-api-key>`,
/// identical to the OpenAI header layout. Models are addressed by
/// their public identifiers (`gemini-2.0-flash`, `gemini-2.5-pro`,
/// `gemini-2.5-flash`, ...).
class GeminiClient extends OpenAiClient {

    GeminiClient(String apiKey, String baseUrl) {
        super(apiKey, baseUrl);
        setDefaultModel("gemini-2.0-flash");
    }

    @Override
    public String getProvider() {
        return "gemini";
    }
}
