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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/// Request payload for [LlmClient#embed(EmbeddingRequest)]. Carries
/// one or more input strings plus optional `dimensions` (OpenAI's
/// `dimensions`, Gemini's `outputDimensionality`). A `null` value means
/// "use the model's default dimensionality".
public final class EmbeddingRequest {
    private final String model;
    private final List<String> inputs;
    private final Integer dimensions;

    private EmbeddingRequest(Builder b) {
        this.model = b.model;
        this.inputs = Collections.unmodifiableList(new ArrayList<String>(b.inputs));
        this.dimensions = b.dimensions;
    }

    /// @return a new empty embedding request builder
    public static Builder builder() {
        return new Builder();
    }

    /// Creates a single-input request.
    /// @param model provider model id, or {@code null} for the client default
    /// @param text text to embed
    /// @return immutable embedding request
    public static EmbeddingRequest of(String model, String text) {
        return builder().model(model).inputs(Arrays.asList(text)).build();
    }

    /// @return requested model id, or {@code null} for the client default
    public String getModel() {
        return model;
    }

    /// @return immutable input strings in request order
    public List<String> getInputs() {
        return inputs;
    }

    /// @return requested output dimensions, or {@code null} for model default
    public Integer getDimensions() {
        return dimensions;
    }

    /// Mutable builder for an immutable {@link EmbeddingRequest}.
    public static final class Builder {
        private String model;
        private List<String> inputs = new ArrayList<String>();
        private Integer dimensions;

        Builder() {
        }

        /// @param m provider model id, or {@code null} for client default
        /// @return this builder
        public Builder model(String m) {
            this.model = m;
            return this;
        }

        /// Replaces all input strings.
        /// @param in strings to embed; {@code null} clears the list
        /// @return this builder
        public Builder inputs(List<String> in) {
            this.inputs = in == null ? new ArrayList<String>()
                    : new ArrayList<String>(in);
            return this;
        }

        /// @param s input string to append
        /// @return this builder
        public Builder addInput(String s) {
            this.inputs.add(s);
            return this;
        }

        /// @param d requested vector dimensions, or {@code null} for model default
        /// @return this builder
        public Builder dimensions(Integer d) {
            this.dimensions = d;
            return this;
        }

        /// @return validated immutable request
        /// @throws IllegalStateException when no input strings were supplied
        public EmbeddingRequest build() {
            if (inputs.isEmpty()) {
                throw new IllegalStateException("at least one input is required");
            }
            return new EmbeddingRequest(this);
        }
    }
}
