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

/// An app a developer registered. `clientSecret` is answered once, when the
/// app is registered, and is empty ever after; the server keeps a hash. The
/// app's webhooks are signed with the lowercase hex HMAC-SHA256 of the text
/// `bazaar-webhooks` under that secret, which the app computes itself.
///
/// @param id The app's id; its metafields live in the namespace `app--<id>--`.
/// @param webhookUrl Where the mandatory webhooks (`app/uninstalled` and the
///        privacy topics) are sent; the app URL when left empty.
/// @param redirectUris The exact addresses an authorization may return to.
/// @param scopes The most the app may ask a store for.
/// @param status `DRAFT` (installable only by its developer) or `LISTED`.
@Mapped
public record AppDto(
        String id,
        String clientId,
        String name,
        String description,
        String iconUrl,
        String appUrl,
        String webhookUrl,
        List<String> redirectUris,
        List<String> scopes,
        String status,
        String developer,
        long createdAt,
        String clientSecret) {
}
