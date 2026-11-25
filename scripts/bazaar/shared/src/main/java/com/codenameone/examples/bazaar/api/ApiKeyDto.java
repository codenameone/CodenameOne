/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar.api;

import com.codename1.annotations.Mapped;

import java.util.List;

/// A store's API key. `secret` is the whole key, answered once when the key
/// is made and empty ever after; `display` is its prefix and last four
/// characters, which is all of it a list shows.
///
/// @param scopes Some of the read_/write_ scopes of products, orders, customers,
///        inventory, discounts, fulfillments and metafields, and `read_analytics`.
/// @param lastUsedAt When a request last presented it, or 0.
/// @param revokedAt When it was withdrawn, or 0.
@Mapped
public record ApiKeyDto(
        String id,
        String name,
        List<String> scopes,
        String display,
        String createdBy,
        long createdAt,
        long lastUsedAt,
        long revokedAt,
        String secret) {
}
