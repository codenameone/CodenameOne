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

/// An image attachment for a multi-modal [ChatMessage]. Construct from
/// raw bytes (the provider encodes them as base64 inline data) or from
/// a publicly-reachable URL -- both modes are accepted by OpenAI,
/// Anthropic, and Gemini.
public final class ImagePart extends MessagePart {
    private final byte[] data;
    private final String mimeType;
    private final String url;

    /// Inline image bytes. `mimeType` must be set (e.g. `"image/png"`,
    /// `"image/jpeg"`); the providers reject inline images without it.
    public ImagePart(byte[] data, String mimeType) {
        if (data == null || mimeType == null) {
            throw new IllegalArgumentException("data and mimeType are required");
        }
        this.data = data;
        this.mimeType = mimeType;
        this.url = null;
    }

    /// Remote image by URL. Only HTTPS is portable across providers.
    public ImagePart(String url) {
        if (url == null) {
            throw new IllegalArgumentException("url is required");
        }
        this.data = null;
        this.mimeType = null;
        this.url = url;
    }

    public byte[] getData() {
        return data;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getUrl() {
        return url;
    }

    public boolean isUrl() {
        return url != null;
    }
}
