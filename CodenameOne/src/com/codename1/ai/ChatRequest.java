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

    public static Builder builder() {
        return new Builder();
    }

    public String getModel() {
        return model;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public Float getTemperature() {
        return temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public Float getTopP() {
        return topP;
    }

    public List<String> getStopSequences() {
        return stopSequences;
    }

    public Long getSeed() {
        return seed;
    }

    public ResponseFormat getResponseFormat() {
        return responseFormat;
    }

    public List<Tool> getTools() {
        return tools;
    }

    public ToolChoice getToolChoice() {
        return toolChoice;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

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

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder messages(List<ChatMessage> messages) {
            this.messages = messages == null ? new ArrayList<ChatMessage>()
                    : new ArrayList<ChatMessage>(messages);
            return this;
        }

        public Builder addMessage(ChatMessage m) {
            this.messages.add(m);
            return this;
        }

        public Builder temperature(Float t) {
            this.temperature = t;
            return this;
        }

        public Builder maxTokens(Integer n) {
            this.maxTokens = n;
            return this;
        }

        public Builder topP(Float p) {
            this.topP = p;
            return this;
        }

        public Builder stopSequences(List<String> stops) {
            this.stopSequences = stops;
            return this;
        }

        public Builder seed(Long seed) {
            this.seed = seed;
            return this;
        }

        public Builder responseFormat(ResponseFormat f) {
            this.responseFormat = f;
            return this;
        }

        public Builder tools(List<Tool> tools) {
            this.tools = tools;
            return this;
        }

        public Builder toolChoice(ToolChoice choice) {
            this.toolChoice = choice;
            return this;
        }

        public Builder metadata(Map<String, String> meta) {
            this.metadata = meta;
            return this;
        }

        public Builder safetyFilter(SafetyFilter f) {
            this.safetyFilter = f;
            return this;
        }

        public ChatRequest build() {
            if (messages.isEmpty()) {
                throw new IllegalStateException("at least one message is required");
            }
            return new ChatRequest(this);
        }
    }
}
