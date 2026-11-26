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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/// The signature on a request Bazaar forwards to an app's storefront proxy.
///
/// **For app developers.** The forwarded query carries the shopper's own
/// parameters plus `shop` (the store's handle), `path_prefix`
/// (`/apps/<subpath>`), `timestamp` (epoch seconds) and `signature`. To
/// check it: take every parameter but `signature`, decoded, sort them by
/// name, join them as `name=value` with `&`, and compare `signature` with
/// the hex HMAC-SHA256 of that text keyed with the app's signing key (the
/// key its webhooks are signed with), in constant time. Refuse a
/// `timestamp` more than five minutes old. [#verify] does all of that.
public final class ProxySignature {
    /// How old a forwarded request may be.
    public static final long MAX_AGE_SECONDS = 300;

    private ProxySignature() {
    }

    /// The text that is signed: the parameters but `signature`, sorted by
    /// name, as `name=value` joined with `&`.
    public static String message(Map<String, String> params) {
        List<String> names = new ArrayList<String>();
        for (String n : params.keySet()) {
            if (!"signature".equals(n)) {
                names.add(n);
            }
        }
        Collections.sort(names);
        StringBuilder b = new StringBuilder();
        for (int iter = 0; iter < names.size(); iter++) {
            String n = names.get(iter);
            b.append(iter == 0 ? "" : "&").append(n).append('=').append(params.get(n));
        }
        return b.toString();
    }

    /// The hex signature of `params` under `signingKey`.
    public static String sign(Map<String, String> params, String signingKey) throws IOException {
        return Extensions.hex(Extensions.hmac(signingKey, message(params)));
    }

    /// True when `params` carry a valid signature under `signingKey` and a
    /// timestamp within [#MAX_AGE_SECONDS] of `nowSeconds`.
    public static boolean verify(Map<String, String> params, String signingKey, long nowSeconds)
            throws IOException {
        String given = params.get("signature");
        String ts = params.get("timestamp");
        if (given == null || ts == null || ts.length() == 0 || ts.length() > 12) {
            return false;
        }
        long at;
        try {
            at = Long.parseLong(ts);
        } catch (NumberFormatException bad) {
            return false;
        }
        if (Math.abs(nowSeconds - at) > MAX_AGE_SECONDS) {
            return false;
        }
        return Extensions.same(sign(params, signingKey), given);
    }
}
