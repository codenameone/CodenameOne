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
package com.codenameone.examples.bazaar.marketing;

import com.codename1.backend.Crypto;
import com.codenameone.examples.bazaar.Ids;

import java.io.IOException;

/// Small helpers the marketing features share: random codes and tokens,
/// HMAC signatures, and ASCII case folding.
public final class Tokens {
    /// No 0/O or 1/I: a code may be read aloud or typed from a receipt.
    static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private Tokens() {
    }

    /// `length` characters from [#ALPHABET], each from one secure random
    /// byte; `& 31` is exactly uniform because 256 is a multiple of 32.
    public static String code(int length) throws IOException {
        byte[] random = Crypto.randomBytes(length);
        StringBuilder out = new StringBuilder(length);
        for (int iter = 0; iter < length; iter++) {
            out.append(ALPHABET.charAt(random[iter] & 31));
        }
        return out.toString();
    }

    /// A URL-safe token of 128 random bits (32 hex digits).
    public static String token() throws IOException {
        return Ids.next();
    }

    /// The HMAC-SHA256 of `text` under `secret`, in hex.
    public static String sign(String secret, String text) throws IOException {
        return Ids.hex(Crypto.hmacSha256(secret.getBytes("UTF-8"), text.getBytes("UTF-8")));
    }

    /// Whether `signature` is [#sign] of `text`, compared in constant time.
    public static boolean verify(String secret, String text, String signature) throws IOException {
        if (signature == null) {
            return false;
        }
        return Crypto.equalsConstantTime(sign(secret, text).getBytes("UTF-8"),
                signature.getBytes("UTF-8"));
    }

    /// `text` trimmed with ASCII letters lower case. Folded by hand:
    /// `toLowerCase()` follows the locale, and a Turkish one folds `I` to a
    /// dotless i that no address contains.
    public static String fold(String text) {
        if (text == null) {
            return "";
        }
        String t = text.trim();
        StringBuilder out = new StringBuilder(t.length());
        for (int iter = 0; iter < t.length(); iter++) {
            char c = t.charAt(iter);
            out.append(c >= 'A' && c <= 'Z' ? (char) (c - 'A' + 'a') : c);
        }
        return out.toString();
    }

    /// An e-mail address in the form two of them are compared in: folded,
    /// and with a `+tag` dropped from the local part, so `ann+2@x.io` is
    /// recognised as `ann@x.io`.
    public static String mailbox(String email) {
        String e = fold(email);
        int at = e.indexOf('@');
        int plus = e.indexOf('+');
        if (at > 0 && plus > 0 && plus < at) {
            e = e.substring(0, plus) + e.substring(at);
        }
        return e;
    }

    /// Digits only, for comparing phone numbers typed differently.
    public static String digits(String phone) {
        if (phone == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < phone.length(); iter++) {
            char c = phone.charAt(iter);
            if (c >= '0' && c <= '9') {
                out.append(c);
            }
        }
        return out.toString();
    }

    /// `#rrggbb` when `colour` is one, else `fallback`.
    public static String colour(String colour, String fallback) {
        if (colour == null || colour.length() != 7 || colour.charAt(0) != '#') {
            return fallback;
        }
        for (int iter = 1; iter < 7; iter++) {
            char c = colour.charAt(iter);
            boolean hex = c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F';
            if (!hex) {
                return fallback;
            }
        }
        return colour;
    }

    /// Milliseconds as an instant; 0 or less is unset (the epoch).
    public static java.time.Instant at(long millis) {
        return java.time.Instant.ofEpochMilli(Math.max(0, millis));
    }

    /// Whether an instant is set (after the epoch).
    public static boolean set(java.time.Instant at) {
        return at != null && at.toEpochMilli() > 0;
    }
}
