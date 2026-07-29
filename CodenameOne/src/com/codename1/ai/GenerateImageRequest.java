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

/// Request payload for [ImageGenerator#generate(GenerateImageRequest)].
/// Mutable image-generation request shared by the supported providers.
/// Provider-specific values that are unsupported may be ignored or rejected
/// by the selected {@link ImageGenerator}.
public final class GenerateImageRequest {
    private final String prompt;
    private String model;
    private String size = "1024x1024";
    private String style;
    private String quality;
    private int count = 1;
    private Long seed;

    /// Creates a request with required prompt text.
    /// @param prompt description of the image to generate
    public GenerateImageRequest(String prompt) {
        if (prompt == null || prompt.length() == 0) {
            throw new IllegalArgumentException("prompt is required");
        }
        this.prompt = prompt;
    }

    /// @return image description sent to the provider
    public String getPrompt() {
        return prompt;
    }

    /// @return requested model id, or {@code null} for generator default
    public String getModel() {
        return model;
    }

    /// @param model provider model id, or {@code null} for generator default
    /// @return this request
    public GenerateImageRequest setModel(String model) {
        this.model = model;
        return this;
    }

    /// @return requested dimensions such as {@code 1024x1024}
    public String getSize() {
        return size;
    }

    /// `"1024x1024"`, `"1024x1792"`, `"1792x1024"` for DALL-E 3.
    /// Default is `"1024x1024"`.
    public GenerateImageRequest setSize(String size) {
        this.size = size;
        return this;
    }

    /// @return provider style value, or {@code null} when unspecified
    public String getStyle() {
        return style;
    }

    /// @param style provider-specific rendering style, or {@code null}
    /// @return this request
    public GenerateImageRequest setStyle(String style) {
        this.style = style;
        return this;
    }

    /// @return provider quality tier, or {@code null} when unspecified
    public String getQuality() {
        return quality;
    }

    /// @param quality provider-specific quality tier, or {@code null}
    /// @return this request
    public GenerateImageRequest setQuality(String quality) {
        this.quality = quality;
        return this;
    }

    /// @return requested image count
    public int getCount() {
        return count;
    }

    /// Number of images to generate (DALL-E 3 supports 1; older
    /// models up to 10).
    public GenerateImageRequest setCount(int count) {
        this.count = Math.max(1, count);
        return this;
    }

    /// @return deterministic seed, or {@code null} when unspecified
    public Long getSeed() {
        return seed;
    }

    /// @param seed deterministic seed where supported, or {@code null}
    /// @return this request
    public GenerateImageRequest setSeed(Long seed) {
        this.seed = seed;
        return this;
    }
}
