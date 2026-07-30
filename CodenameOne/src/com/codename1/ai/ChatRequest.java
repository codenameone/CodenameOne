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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The full request to [LlmClient#chat(ChatRequest)] /
/// [LlmClient#chatStream(ChatRequest, StreamingListener)]. Built via
/// [#builder()]; immutable once constructed so the same request can be
/// re-used across retries.
///
/// Numeric tuning fields are boxed so a `null` means "don't send" --
/// the provider's own default is used instead of one we picked.
public final class ChatRequest {
    private final String model;
    private final List<ChatMessage> messages;
    private final Float temperature;
    private final Integer maxTokens;
    private final Float topP;
    private final List<String> stopSequences;
    private final Long seed;
    private final ResponseFormat responseFormat;
    private final List<Tool> tools;
    private final ToolChoice toolChoice;
    private final Map<String, String> metadata;
    private final SafetyFilter safetyFilter;

    private ChatRequest(Builder b) {
        this.model = b.model;
        this.messages = Collections.unmodifiableList(new ArrayList<ChatMessage>(b.messages));
        this.temperature = b.temperature;
        this.maxTokens = b.maxTokens;
        this.topP = b.topP;
        this.stopSequences = b.stopSequences == null ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(b.stopSequences));
        this.seed = b.seed;
        this.responseFormat = b.responseFormat;
        this.tools = b.tools == null ? Collections.<Tool>emptyList()
                : Collections.unmodifiableList(new ArrayList<Tool>(b.tools));
        this.toolChoice = b.toolChoice;
        this.metadata = b.metadata == null ? Collections.<String, String>emptyMap()
                : Collections.unmodifiableMap(new HashMap<String, String>(b.metadata));
        this.safetyFilter = b.safetyFilter;
    }

    /// Starts an empty request builder. At least one message is required.
    /// @return a new builder
    public static Builder builder() {
        return new Builder();
    }

    /// @return requested provider model, or {@code null} for the client default
    public String getModel() {
        return model;
    }

    /// @return immutable conversation messages in provider order
    public List<ChatMessage> getMessages() {
        return messages;
    }

    /// @return sampling temperature, or {@code null} for the provider default
    public Float getTemperature() {
        return temperature;
    }

    /// @return maximum generated tokens, or {@code null} for the provider default
    public Integer getMaxTokens() {
        return maxTokens;
    }

    /// @return nucleus-sampling probability, or {@code null} when unspecified
    public Float getTopP() {
        return topP;
    }

    /// @return immutable stop sequences; empty when none were requested
    public List<String> getStopSequences() {
        return stopSequences;
    }

    /// @return deterministic sampling seed, or {@code null} when unspecified
    public Long getSeed() {
        return seed;
    }

    /// @return requested text/JSON response format, or {@code null}
    public ResponseFormat getResponseFormat() {
        return responseFormat;
    }

    /// @return immutable tools offered to the model
    public List<Tool> getTools() {
        return tools;
    }

    /// @return tool-selection policy, or {@code null} for provider behavior
    public ToolChoice getToolChoice() {
        return toolChoice;
    }

    /// @return immutable provider metadata attached to this request
    public Map<String, String> getMetadata() {
        return metadata;
    }

    /// @return client-side preflight filter, or {@code null} when disabled
    public SafetyFilter getSafetyFilter() {
        return safetyFilter;
    }

    /// Returns a builder pre-populated with the values of this request.
    /// Useful for replaying a request with one field changed.
    public Builder toBuilder() {
        Builder b = new Builder();
        b.model = model;
        b.messages = new ArrayList<ChatMessage>(messages);
        b.temperature = temperature;
        b.maxTokens = maxTokens;
        b.topP = topP;
        b.stopSequences = new ArrayList<String>(stopSequences);
        b.seed = seed;
        b.responseFormat = responseFormat;
        b.tools = new ArrayList<Tool>(tools);
        b.toolChoice = toolChoice;
        b.metadata = new HashMap<String, String>(metadata);
        b.safetyFilter = safetyFilter;
        return b;
    }

    /// Mutable fluent builder for {@link ChatRequest}. Collection arguments
    /// are copied when the immutable request is built.
    public static final class Builder {
        private String model;
        private List<ChatMessage> messages = new ArrayList<ChatMessage>();
        private Float temperature;
        private Integer maxTokens;
        private Float topP;
        private List<String> stopSequences;
        private Long seed;
        private ResponseFormat responseFormat;
        private List<Tool> tools;
        private ToolChoice toolChoice;
        private Map<String, String> metadata;
        private SafetyFilter safetyFilter;

        Builder() {
        }

        /// Selects a provider model.
        /// @param model provider model id; {@code null} uses the client default
        /// @return this builder
        public Builder model(String model) {
            this.model = model;
            return this;
        }

        /// Replaces the conversation history.
        /// @param messages messages in chronological order; {@code null} clears them
        /// @return this builder
        public Builder messages(List<ChatMessage> messages) {
            this.messages = messages == null ? new ArrayList<ChatMessage>()
                    : new ArrayList<ChatMessage>(messages);
            return this;
        }

        /// Appends one conversation message.
        /// @param m message to append
        /// @return this builder
        public Builder addMessage(ChatMessage m) {
            this.messages.add(m);
            return this;
        }

        /// Sets sampling temperature.
        /// @param t provider-supported temperature, or {@code null} to omit
        /// @return this builder
        public Builder temperature(Float t) {
            this.temperature = t;
            return this;
        }

        /// Limits response length.
        /// @param n maximum generated token count, or {@code null} to omit
        /// @return this builder
        public Builder maxTokens(Integer n) {
            this.maxTokens = n;
            return this;
        }

        /// Sets nucleus sampling probability.
        /// @param p provider-supported probability, or {@code null} to omit
        /// @return this builder
        public Builder topP(Float p) {
            this.topP = p;
            return this;
        }

        /// Sets sequences that terminate generation.
        /// @param stops stop strings, or {@code null} for none
        /// @return this builder
        public Builder stopSequences(List<String> stops) {
            this.stopSequences = stops;
            return this;
        }

        /// Requests deterministic sampling where supported.
        /// @param seed provider sampling seed, or {@code null} to omit
        /// @return this builder
        public Builder seed(Long seed) {
            this.seed = seed;
            return this;
        }

        /// Requests text or structured JSON output.
        /// @param f desired format, or {@code null} for provider default
        /// @return this builder
        public Builder responseFormat(ResponseFormat f) {
            this.responseFormat = f;
            return this;
        }

        /// Replaces the callable tools advertised to the model.
        /// @param tools tool definitions, or {@code null} for none
        /// @return this builder
        public Builder tools(List<Tool> tools) {
            this.tools = tools;
            return this;
        }

        /// Controls whether and which tool the model may call.
        /// @param choice selection policy, or {@code null} for provider default
        /// @return this builder
        public Builder toolChoice(ToolChoice choice) {
            this.toolChoice = choice;
            return this;
        }

        /// Attaches provider-specific request metadata.
        /// @param meta metadata copied into the request, or {@code null}
        /// @return this builder
        public Builder metadata(Map<String, String> meta) {
            this.metadata = meta;
            return this;
        }

        /// Installs a client-side filter evaluated before network submission.
        /// @param f safety policy, or {@code null} to disable it
        /// @return this builder
        public Builder safetyFilter(SafetyFilter f) {
            this.safetyFilter = f;
            return this;
        }

        /// Validates and creates an immutable request.
        /// @return completed request
        /// @throws IllegalStateException when no messages were supplied
        public ChatRequest build() {
            if (messages.isEmpty()) {
                throw new IllegalStateException("at least one message is required");
            }
            return new ChatRequest(this);
        }
    }
}
