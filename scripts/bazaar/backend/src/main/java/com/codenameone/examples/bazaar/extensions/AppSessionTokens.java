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
package com.codenameone.examples.bazaar.extensions;

import com.codename1.backend.Jwt;
import com.codenameone.examples.bazaar.Ids;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/// The session token Bazaar appends to an installed app's own page when the
/// merchant opens it (`?shop=<storeId>&session=<token>`).
///
/// **For app developers.** The token is a JWT signed with HS256. Its key is
/// the app's signing key -- the same one its webhooks are signed with,
/// derived from the client secret as `hex(HMAC-SHA256(key = client secret,
/// message = "bazaar-webhooks"))` ([com.codenameone.examples.bazaar.platform.Apps#signingKey]),
/// used as the UTF-8 bytes of that hex string. Accept a token only when:
///
/// 1. the signature verifies with that key (compare in constant time);
/// 2. `exp` (seconds since the epoch) is in the future -- tokens live five
///    minutes, so a page should exchange one for its own session at once;
/// 3. `aud` is the app's own client id, so a token minted for another app
///    is useless here;
/// 4. `iss` is `bazaar`; `dest` is the store id and `sub` the merchant's
///    user name the page acts for. `jti` is unique per token, for a page
///    that wants to refuse a token it already used.
///
/// [#verify] does all four and is what an app on this server's library would
/// call; any JWT library does the same.
public final class AppSessionTokens {
    /// How long a token is good for.
    public static final long TTL_SECONDS = 300;
    public static final String ISSUER = "bazaar";

    private AppSessionTokens() {
    }

    /// A token for `username` of `storeId` opening the app of `clientId`.
    static String issue(String signingKey, String clientId, String storeId, String username)
            throws IOException {
        Map<String, Object> claims = new LinkedHashMap<String, Object>();
        claims.put("iss", ISSUER);
        claims.put("aud", clientId);
        claims.put("dest", storeId);
        claims.put("sub", username);
        claims.put("jti", Ids.next());
        return Jwt.issue(claims, signingKey.getBytes("UTF-8"), TTL_SECONDS);
    }

    /// The claims of `token` when it is signed with `signingKey`, unexpired,
    /// issued by Bazaar and meant for `clientId`; throws otherwise.
    public static Map verify(String token, String signingKey, String clientId)
            throws IOException {
        if (token == null || signingKey == null || signingKey.length() == 0) {
            throw new IOException("No token");
        }
        Map claims = Extensions.verifyJwt(token, signingKey);
        if (!ISSUER.equals(claims.get("iss"))) {
            throw new IOException("Not issued by Bazaar");
        }
        if (clientId == null || !clientId.equals(claims.get("aud"))) {
            throw new IOException("Meant for another app");
        }
        return claims;
    }
}
