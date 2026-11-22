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

/// An OpenID Connect provider a store's staff sign in with.
///
/// @param issuer The provider's issuer URL; its discovery document names the endpoints.
/// @param clientSecret Written, never read back: answers carry an empty one.
/// @param domainRoles One `domain=ROLE` per line: whom the provider may sign in, and as what.
/// @param signInPath Where a member starts signing in through it.
@Mapped
public record SsoProviderDto(
        String id,
        String name,
        String issuer,
        String clientId,
        String clientSecret,
        String domainRoles,
        boolean enabled,
        String signInPath) {
}
