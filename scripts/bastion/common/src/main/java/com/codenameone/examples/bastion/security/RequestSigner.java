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
package com.codenameone.examples.bastion.security;

import com.codename1.mapping.Mappers;
import com.codename1.security.Hash;
import com.codename1.security.PrivateKey;
import com.codename1.security.SecureRandom;
import com.codename1.security.Signature;
import com.codename1.util.Base64;
import com.codenameone.examples.bastion.api.SignedRequest;

import java.io.UnsupportedEncodingException;

/// Signs a request with the device key, in the form the server checks
/// ([SignedRequest], shared with it).
///
/// The signature covers the exact body bytes, so it is made over the very
/// text the typed client sends: the generated client writes a `@Body` as
/// `Mappers.toJson(body)` in UTF-8, and [#sign(String, String, Object)] hashes
/// that same string. Sign the object you then pass, unchanged, to the call.
public final class RequestSigner {
    private RequestSigner() {
    }

    /// The `Bastion-Signature` header for `method target` carrying `body` (a
    /// mapped DTO, or null for no body), or null when this device has no
    /// registered key yet -- the server then answers 403 for anything that
    /// needs one, and the app offers to register the device.
    public static String sign(String method, String target, Object body) {
        String deviceId = DeviceKey.deviceId();
        PrivateKey key = DeviceKey.privateKey();
        if (deviceId == null || key == null) {
            return null;
        }
        return header(deviceId, key, method, target, bodyBytes(body), nonce(),
                System.currentTimeMillis());
    }

    /// The bytes the generated client sends for `body`.
    public static byte[] bodyBytes(Object body) {
        if (body == null) {
            return new byte[0];
        }
        return utf8(Mappers.toJson(body));
    }

    /// The header for the given parts; the deterministic core of
    /// [#sign(String, String, Object)], which the tests check against the
    /// server's form.
    public static String header(String deviceId, PrivateKey privateKey, String method,
            String target, byte[] body, String nonce, long ts) {
        String text = SignedRequest.canonical(method, target, Hash.sha256(body), nonce, ts);
        byte[] der = Signature.sign(Signature.SHA256_WITH_ECDSA, privateKey, utf8(text));
        return SignedRequest.header(deviceId, ts, nonce, Base64.encodeUrlSafe(jose(der)));
    }

    /// The DER `SEQUENCE { INTEGER r, INTEGER s }` the platform signs into, as
    /// the 64-byte JOSE `r || s` the header carries (ES256, RFC 7518 3.4).
    static byte[] jose(byte[] der) {
        if (der == null || der.length < 8 || der[0] != 0x30) {
            throw new IllegalArgumentException("not a DER ECDSA signature");
        }
        int at = (der[1] & 0x80) != 0 ? 2 + (der[1] & 0x7f) : 2;
        byte[] out = new byte[64];
        for (int part = 0; part < 2; part++) {
            if (der[at] != 0x02) {
                throw new IllegalArgumentException("not a DER ECDSA signature");
            }
            int len = der[at + 1] & 0xff;
            int start = at + 2;
            int from = start;
            int n = len;
            while (n > 32 && der[from] == 0) {
                from++;
                n--;
            }
            if (n > 32) {
                throw new IllegalArgumentException("not a P-256 signature");
            }
            System.arraycopy(der, from, out, part * 32 + 32 - n, n);
            at = start + len;
        }
        return out;
    }

    /// 128 random bits, base64url: 22 characters, in the server's alphabet.
    public static String nonce() {
        return Base64.encodeUrlSafe(SecureRandom.bytes(16));
    }

    /// A fresh `Idempotency-Key` for one money action. Made once when the
    /// user starts the action and reused for every retry of it, so a retry
    /// after a lost answer is answered with the first receipt instead of
    /// moving the money twice.
    public static String idempotencyKey() {
        return Base64.encodeUrlSafe(SecureRandom.bytes(18));
    }

    static byte[] utf8(String text) {
        try {
            return text.getBytes("UTF-8");
        } catch (UnsupportedEncodingException impossible) {
            throw new IllegalStateException("UTF-8 is missing");
        }
    }
}
