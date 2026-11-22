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
package com.codenameone.examples.bazaar.web.security;

import com.codename1.backend.HttpServer;
import com.codenameone.examples.bazaar.marketing.Campaigns;
import com.codenameone.examples.bazaar.marketing.Tokens;
import java.io.IOException;

/// The known-device cookie: set on every successful password sign-in, it is
/// the HMAC of the folded account name under a server secret, so it proves
/// that this browser signed in to that account before. A sign-in attempt
/// carrying a valid one for the account it names skips the account-wide
/// lockout (`signin-failures-account`): a stranger failing on purpose from
/// many addresses can lock out new browsers, but never the owner's own. The
/// per account and address rule and the per address rule still apply.
///
/// The secret is kept like a store's marketing secret ([Campaigns#secret])
/// under a key no store id can take, so it survives a restart and is shared
/// by every instance.
final class KnownDevices {
    static final String COOKIE = "bazaar_device";
    /// A year: the cookie is worth as much as the browser keeping it.
    static final int MAX_AGE = 365 * 24 * 3600;
    private static final String SECRET_KEY = "signin-known-device";

    private final Campaigns campaigns;

    KnownDevices(Campaigns campaigns) {
        this.campaigns = campaigns;
    }

    private String secret() throws IOException {
        try {
            return campaigns.secret(SECRET_KEY);
        } catch (RuntimeException raced) {
            // Two first sign-ins made it at once; the other one's is there now.
            return campaigns.secret(SECRET_KEY);
        }
    }

    /// The `Set-Cookie` value naming `account` (already folded).
    String cookie(String account, boolean secure) throws IOException {
        return COOKIE + "=" + Tokens.sign(secret(), "device|" + account) + "; Path=/; Max-Age="
                + MAX_AGE + "; SameSite=Lax; HttpOnly" + (secure ? "; Secure" : "");
    }

    /// Whether `request` carries a valid known-device cookie for `account`.
    boolean known(HttpServer.Request request, String account) {
        if (account.length() == 0) {
            return false;
        }
        String c = request.getCookie(COOKIE);
        if (c == null || c.length() == 0) {
            return false;
        }
        try {
            return Tokens.verify(secret(), "device|" + account, c);
        } catch (IOException e) {
            return false;
        }
    }
}
