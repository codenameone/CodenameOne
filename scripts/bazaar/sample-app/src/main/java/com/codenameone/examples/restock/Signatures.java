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
package com.codenameone.examples.restock;

import com.codename1.backend.Crypto;
import com.codename1.backend.Jwt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/// Everything an app checks before it believes a request came from Bazaar.
///
/// All of it rests on one key, the app's *signing key*, which the app derives
/// from its client secret and never receives over the wire:
///
/// ```
/// signingKey = hex(HMAC-SHA256(key = client secret, message = "bazaar-webhooks"))
/// ```
///
/// The key is then used as the UTF-8 bytes of that hex text for three
/// different signatures:
///
/// | What arrives | How it is signed |
/// |---|---|
/// | webhooks, block data and action calls | `X-Bazaar-Timestamp: <epoch seconds>` and `X-Bazaar-Signature: sha256=<hex HMAC of timestamp + "." + raw body>` |
/// | app proxy requests | `signature` query parameter: hex HMAC of the other parameters, sorted, each name and value percent-encoded, `name=value` joined with `&` |
/// | embedded page sessions | an HS256 JWT, `aud` = the app's client id, `iss` = `bazaar` |
public final class Signatures {
    /// How old a proxied request may be before it is refused.
    public static final long PROXY_MAX_AGE_SECONDS = 300;
    /// How far a signed call's `X-Bazaar-Timestamp` may be from this clock.
    public static final long BODY_MAX_AGE_SECONDS = 300;

    private Signatures() {
    }

    /// The signing key of an app whose client secret is `clientSecret`.
    public static String signingKey(String clientSecret) throws IOException {
        return hex(Crypto.hmacSha256(utf8(clientSecret), utf8("bazaar-webhooks")));
    }

    /// True when `header` (the `X-Bazaar-Signature` value) signs
    /// `timestamp` (the `X-Bazaar-Timestamp` value), a `.` and `body`, and the
    /// timestamp is within five minutes of `nowSeconds`: a call recorded once
    /// cannot be sent again later. Compute over the bytes exactly as they
    /// arrived, before parsing them.
    public static boolean body(String signingKey, byte[] body, String header, String timestamp,
            long nowSeconds) throws IOException {
        if (header == null || body == null) {
            return false;
        }
        long at = seconds(timestamp);
        if (at < 0 || Math.abs(nowSeconds - at) > BODY_MAX_AGE_SECONDS) {
            return false;
        }
        byte[] prefix = utf8(at + ".");
        byte[] message = new byte[prefix.length + body.length];
        System.arraycopy(prefix, 0, message, 0, prefix.length);
        System.arraycopy(body, 0, message, prefix.length, body.length);
        String expected = "sha256=" + hex(Crypto.hmacSha256(utf8(signingKey), message));
        return Crypto.equalsConstantTime(utf8(expected), utf8(header.trim()));
    }

    /// Epoch seconds written as digits, or -1 for anything else.
    static long seconds(String text) {
        if (text == null) {
            return -1;
        }
        String t = text.trim();
        if (t.length() == 0 || t.length() > 12) {
            return -1;
        }
        long at = 0;
        for (int iter = 0; iter < t.length(); iter++) {
            char c = t.charAt(iter);
            if (c < '0' || c > '9') {
                return -1;
            }
            at = at * 10 + (c - '0');
        }
        return at;
    }

    /// True when the decoded query `params` of a proxied request carry a
    /// valid `signature` and a `timestamp` within five minutes of `nowSeconds`.
    public static boolean proxy(Map<String, String> params, String signingKey, long nowSeconds)
            throws IOException {
        String given = params.get("signature");
        long at = seconds(params.get("timestamp"));
        if (given == null || at < 0) {
            return false;
        }
        if (Math.abs(nowSeconds - at) > PROXY_MAX_AGE_SECONDS) {
            return false;
        }
        List<String> names = new ArrayList<String>();
        for (String n : params.keySet()) {
            if (!"signature".equals(n)) {
                names.add(n);
            }
        }
        Collections.sort(names);
        StringBuilder message = new StringBuilder();
        for (int iter = 0; iter < names.size(); iter++) {
            String n = names.get(iter);
            message.append(iter == 0 ? "" : "&").append(encode(n)).append('=')
                    .append(encode(params.get(n)));
        }
        String expected = hex(Crypto.hmacSha256(utf8(signingKey), utf8(message.toString())));
        return Crypto.equalsConstantTime(utf8(expected), utf8(given));
    }

    /// The claims of an embedded page's `session` token, or an exception when
    /// it is forged, expired, not from Bazaar or minted for another app.
    /// `dest` is the store id and `sub` the merchant the page acts for.
    public static Map session(String token, String signingKey, String clientId)
            throws IOException {
        // Jwt.verify checks the HS256 signature and the expiry.
        Map claims = Jwt.verify(token, utf8(signingKey));
        if (!"bazaar".equals(claims.get("iss"))) {
            throw new IOException("Not issued by Bazaar");
        }
        if (clientId == null || !clientId.equals(claims.get("aud"))) {
            throw new IOException("Meant for another app");
        }
        return claims;
    }

    /// `text` percent-encoded as RFC 3986 does: the UTF-8 bytes, letters,
    /// digits and `-._~` as they are, every other byte `%XX` in upper case
    /// hex (a space is `%20`, never `+`). What the proxy signature encodes
    /// each name and value with.
    static String encode(String text) throws IOException {
        if (text == null) {
            return "";
        }
        byte[] b = utf8(text);
        String hexDigits = "0123456789ABCDEF";
        StringBuilder out = new StringBuilder(b.length + 8);
        for (int iter = 0; iter < b.length; iter++) {
            int c = b[iter] & 0xff;
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '.' || c == '_' || c == '~') {
                out.append((char) c);
            } else {
                out.append('%').append(hexDigits.charAt(c >> 4)).append(hexDigits.charAt(c & 0xf));
            }
        }
        return out.toString();
    }

    static byte[] utf8(String s) throws IOException {
        return s.getBytes("UTF-8");
    }

    static String hex(byte[] b) {
        String digits = "0123456789abcdef";
        StringBuilder out = new StringBuilder(b.length * 2);
        for (int iter = 0; iter < b.length; iter++) {
            out.append(digits.charAt((b[iter] >> 4) & 0xf)).append(digits.charAt(b[iter] & 0xf));
        }
        return out.toString();
    }
}
