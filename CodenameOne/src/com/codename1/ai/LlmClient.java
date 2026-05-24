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

/// Provider-agnostic chat / embeddings client. Built via one of the
/// static factory methods:
///
/// ```
/// LlmClient gpt    = LlmClient.openai("sk-...");
/// LlmClient claude = LlmClient.anthropic("sk-ant-...");
/// LlmClient gemini = LlmClient.gemini("AIza...");
/// LlmClient ollama = LlmClient.ollama();                   // localhost:11434
/// LlmClient local  = LlmClient.localOpenAiCompatible(
///                        "http://10.0.0.5:8080/v1", "", "qwen2.5-7b");
/// ```
///
/// All calls return [AsyncResource] so they compose naturally with the
/// rest of the Codename One async API. `chatStream` additionally fires
/// per-token deltas through a [StreamingListener]; both that listener
/// and the final `AsyncResource` complete on the EDT.
///
/// #### Simulator behaviour
///
/// When running in the JavaSE simulator with `cn1.ai.simulatorRedirect`
/// set to `ollama` (or `auto` with Ollama detected on the loopback),
/// the static factories transparently route through a local Ollama
/// endpoint instead of the public provider — so unchanged production
/// code can be debugged offline without API charges.
public abstract class LlmClient {
    private String baseUrl;
    private int httpTimeoutMs = 60000;

    protected LlmClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /// OpenAI / OpenAI-compatible (Together, Groq, Fireworks, vLLM,
    /// Ollama, etc.). Uses the public endpoint by default; override
    /// with [#setBaseUrl(String)].
    public static LlmClient openai(String apiKey) {
        return SimulatorRedirect.maybeWrap(new OpenAiClient(apiKey,
                "https://api.openai.com/v1"));
    }

    public static LlmClient anthropic(String apiKey) {
        return SimulatorRedirect.maybeWrap(new AnthropicClient(apiKey,
                "https://api.anthropic.com/v1"));
    }

    public static LlmClient gemini(String apiKey) {
        return SimulatorRedirect.maybeWrap(new GeminiClient(apiKey,
                "https://generativelanguage.googleapis.com/v1beta"));
    }

    /// Default Ollama install: `http://localhost:11434/v1`, model
    /// `llama3.2`.
    public static LlmClient ollama() {
        return ollama("llama3.2");
    }

    public static LlmClient ollama(String defaultModel) {
        return ollama("http://localhost:11434/v1", defaultModel);
    }

    public static LlmClient ollama(String baseUrl, String defaultModel) {
        OpenAiClient c = new OpenAiClient("ollama", baseUrl);
        c.setDefaultModel(defaultModel);
        return c;
    }

    /// Generic OpenAI-compatible endpoint (llama.cpp server, vLLM,
    /// LM Studio, a custom proxy). `apiKey` may be empty for local
    /// services that don't authenticate.
    public static LlmClient localOpenAiCompatible(String baseUrl, String apiKey, String defaultModel) {
        OpenAiClient c = new OpenAiClient(apiKey == null ? "" : apiKey, baseUrl);
        c.setDefaultModel(defaultModel);
        return c;
    }

    /// Non-streaming chat. Equivalent to `chatStream` with a no-op
    /// listener but optimized — the provider skips the SSE response
    /// and returns a single JSON object.
    public abstract AsyncResource<ChatResponse> chat(ChatRequest req);

    /// Streaming chat. `listener` fires for every content delta /
    /// tool-call fragment on the EDT. The returned `AsyncResource`
    /// completes with the aggregated final response once the stream
    /// ends; cancel it to close the underlying socket.
    public abstract AsyncResource<ChatResponse> chatStream(ChatRequest req, StreamingListener listener);

    public abstract AsyncResource<EmbeddingResponse> embed(EmbeddingRequest req);

    /// One of `"openai"`, `"anthropic"`, `"gemini"`, `"ollama"`,
    /// `"local"`. Used by `ChatView` and tests to vary behaviour by
    /// provider.
    public abstract String getProvider();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getHttpTimeoutMs() {
        return httpTimeoutMs;
    }

    public void setHttpTimeoutMs(int httpTimeoutMs) {
        this.httpTimeoutMs = httpTimeoutMs;
    }

    /// Helper for subclasses: applies the active [SafetyFilter] (if
    /// any) before the network call. Returns the rejection reason on
    /// failure, `null` to proceed.
    protected String runSafetyFilter(ChatRequest req) {
        if (req.getSafetyFilter() == null) {
            return null;
        }
        return req.getSafetyFilter().check(req.getMessages());
    }
}
