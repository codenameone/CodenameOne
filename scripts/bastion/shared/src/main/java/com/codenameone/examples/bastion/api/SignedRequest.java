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
package com.codenameone.examples.bastion.api;

/// The one definition of what a device signs, read by both the app (which
/// signs) and the server (which checks): a request is signed over
///
/// ```
/// METHOD|target|hex(sha256(body bytes))|idempotency-key|nonce|ts
/// ```
///
/// where `target` is the path with its query string exactly as the request
/// line carries it, the body is the exact bytes on the wire (an empty array
/// when there are none), the hex is lower case, `idempotency-key` is the
/// request's `Idempotency-Key` header (empty when it has none) -- so a captured
/// signature cannot be replayed under a fresh key to move the money again --
/// `nonce` is 16 to 64 URL-safe
/// characters used once, and `ts` is the device's clock in milliseconds. The
/// text is UTF-8, signed with ECDSA P-256 and SHA-256, and the signature sent
/// as base64url of the 64-byte `r || s` (the JOSE form, as in a JWT) in
///
/// ```
/// Bastion-Signature: v1;device=<id>;ts=<ms>;nonce=<nonce>;sig=<base64url>
/// ```
///
/// Kept free of every library so that it compiles, unchanged, into both.
public final class SignedRequest {
    /// The header the signature travels in.
    public static final String HEADER = "Bastion-Signature";
    /// The scheme every header starts with; a later one would be `v2;`.
    public static final String SCHEME = "v1";
    /// How far the device clock may be from the server's, either way.
    public static final long SKEW_MILLIS = 120000L;

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private SignedRequest() {
    }

    /// The text that is signed. `bodySha256` is the SHA-256 of the body bytes;
    /// `idempotencyKey` is null or empty when the request carries none.
    public static String canonical(String method, String target, byte[] bodySha256,
            String idempotencyKey, String nonce, long ts) {
        return method + "|" + target + "|" + hex(bodySha256) + "|"
                + (idempotencyKey == null ? "" : idempotencyKey) + "|" + nonce + "|" + ts;
    }

    /// The header's value.
    public static String header(String deviceId, long ts, String nonce, String signature) {
        return SCHEME + ";device=" + deviceId + ";ts=" + ts + ";nonce=" + nonce + ";sig="
                + signature;
    }

    /// One `name=value` field of a header, or null.
    public static String field(String header, String name) {
        if (header == null) {
            return null;
        }
        String wanted = name + "=";
        int start = 0;
        while (start <= header.length()) {
            int end = header.indexOf(';', start);
            if (end < 0) {
                end = header.length();
            }
            if (header.startsWith(wanted, start)) {
                return header.substring(start + wanted.length(), end);
            }
            start = end + 1;
        }
        return null;
    }

    /// Whether `nonce` is shaped as the server accepts one.
    public static boolean nonceShaped(String nonce) {
        if (nonce == null || nonce.length() < 16 || nonce.length() > 64) {
            return false;
        }
        for (int iter = 0; iter < nonce.length(); iter++) {
            char c = nonce.charAt(iter);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '_';
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    /// Lower-case hex.
    public static String hex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int iter = 0; iter < bytes.length; iter++) {
            out[iter * 2] = HEX[(bytes[iter] >> 4) & 0xf];
            out[iter * 2 + 1] = HEX[bytes[iter] & 0xf];
        }
        return new String(out);
    }
}
