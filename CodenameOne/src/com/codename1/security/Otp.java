/*
 * Copyright (c) 2008-2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.security;

/// Counter-based (HOTP, RFC 4226) and time-based (TOTP, RFC 6238) one-time
/// password generators. Compatible with any standard authenticator app
/// (Google Authenticator, Microsoft Authenticator, 1Password, etc.).
///
/// #### Generate a 6-digit Google-Authenticator-compatible code
///
/// ```java
/// byte[] secret = Base32.decode("JBSWY3DPEHPK3PXP"); // shared secret
/// String code = Otp.totp(secret); // default 6 digits, 30 second step,
///                                 // SHA-1, current time
/// ```
///
/// #### Verify a code (allowing +/-1 step of clock skew)
///
/// ```java
/// boolean ok = Otp.verifyTotp(secret, userInput, 1);
/// ```
public final class Otp {

    private Otp() {}

    /// Generates an HOTP code (RFC 4226) using SHA-1 and the given digit count.
    ///
    /// #### Parameters
    ///
    /// - `secret`: the shared secret
    ///
    /// - `counter`: the moving factor -- caller is responsible for incrementing
    ///   it after every successful authentication
    ///
    /// - `digits`: number of decimal digits in the output (typically 6, may
    ///   be 6, 7 or 8)
    public static String hotp(byte[] secret, long counter, int digits) {
        return hotp(secret, counter, digits, Hash.SHA1);
    }

    /// Generates an HOTP code with a configurable hash algorithm. Most
    /// authenticator apps assume SHA-1; only override if the issuer publishes a
    /// different `algorithm` parameter in its provisioning URI.
    public static String hotp(byte[] secret, long counter, int digits, String hashAlgorithm) {
        if (digits < 1 || digits > 10) {
            throw new CryptoException("digits must be between 1 and 10");
        }
        byte[] counterBytes = new byte[8];
        for (int i = 7; i >= 0; i--) {
            counterBytes[i] = (byte) (counter & 0xff);
            counter >>>= 8;
        }
        byte[] mac = Hmac.create(hashAlgorithm, secret).doFinal(counterBytes);
        // RFC 4226 dynamic truncation
        int offset = mac[mac.length - 1] & 0x0f;
        int code = ((mac[offset] & 0x7f) << 24)
                 | ((mac[offset + 1] & 0xff) << 16)
                 | ((mac[offset + 2] & 0xff) << 8)
                 | (mac[offset + 3] & 0xff);
        int mod = 1;
        for (int i = 0; i < digits; i++) {
            mod *= 10;
        }
        code %= mod;
        return pad(Integer.toString(code), digits);
    }

    /// Generates a TOTP code (RFC 6238) for the current system time, using
    /// SHA-1, 6 digits and a 30-second step.
    public static String totp(byte[] secret) {
        return totp(secret, System.currentTimeMillis(), 30, 6, Hash.SHA1);
    }

    /// Generates a TOTP code for the current system time with a custom digit
    /// count and step size.
    public static String totp(byte[] secret, int digits, int stepSeconds) {
        return totp(secret, System.currentTimeMillis(), stepSeconds, digits, Hash.SHA1);
    }

    /// Generates a TOTP code with full control over all parameters.
    ///
    /// #### Parameters
    ///
    /// - `secret`: shared secret
    ///
    /// - `currentTimeMillis`: timestamp to derive the code from
    ///
    /// - `stepSeconds`: window size -- 30 in the vast majority of deployments
    ///
    /// - `digits`: number of decimal digits in the output (typically 6 or 8)
    ///
    /// - `hashAlgorithm`: hash to use -- almost always [Hash#SHA1]
    public static String totp(byte[] secret, long currentTimeMillis, int stepSeconds,
                              int digits, String hashAlgorithm) {
        if (stepSeconds <= 0) {
            throw new CryptoException("stepSeconds must be positive");
        }
        long counter = (currentTimeMillis / 1000L) / stepSeconds;
        return hotp(secret, counter, digits, hashAlgorithm);
    }

    /// Verifies a TOTP code, allowing `tolerance` steps of clock skew on either
    /// side of `now` (so a tolerance of 1 will accept the previous, current
    /// and next code).
    public static boolean verifyTotp(byte[] secret, String code, int tolerance) {
        return verifyTotp(secret, code, tolerance,
                System.currentTimeMillis(), 30, 6, Hash.SHA1);
    }

    /// Builds the canonical `otpauth://totp/...` URI that authenticator apps
    /// (Google Authenticator, Microsoft Authenticator, 1Password, Authy, ...)
    /// consume when the user scans a QR code on your enrolment screen. The
    /// format is documented at
    /// <https://github.com/google/google-authenticator/wiki/Key-Uri-Format>.
    ///
    /// Render the returned string as a QR code (server-side render, or a
    /// QR-generation cn1lib) and show it to the user; they scan it, the
    /// authenticator stores `secret` against the `issuer:accountName` label,
    /// and from then on it produces six-digit codes that match
    /// [#totp(byte[])] on your side using the same `secret`.
    ///
    /// #### Parameters
    ///
    /// - `issuer`: the human-readable service name shown in the authenticator
    ///   ("Acme Bank"). Must not contain a `:`.
    ///
    /// - `accountName`: the user's identifier within your service
    ///   ("alice@example.com"). Must not contain a `:`.
    ///
    /// - `secret`: shared secret (the bytes you also pass to
    ///   [#totp(byte[])]) -- encoded as Base32 in the URI per the spec.
    ///
    /// - `digits`: number of digits in each code (typically 6).
    ///
    /// - `stepSeconds`: time-step size, typically 30.
    ///
    /// - `hashAlgorithm`: hash, typically [Hash#SHA1] for authenticator
    ///   compatibility. SHA-256 and SHA-512 are accepted but not all
    ///   authenticator apps support them.
    public static String otpauthUri(String issuer, String accountName, byte[] secret,
                                    int digits, int stepSeconds, String hashAlgorithm) {
        if (issuer == null || issuer.indexOf(':') >= 0) {
            throw new CryptoException("issuer must be non-null and must not contain ':'");
        }
        if (accountName == null || accountName.indexOf(':') >= 0) {
            throw new CryptoException("accountName must be non-null and must not contain ':'");
        }
        String alg = uriAlgorithm(hashAlgorithm);
        StringBuilder b = new StringBuilder(128);
        b.append("otpauth://totp/");
        appendUriComponent(b, issuer);
        b.append(':');
        appendUriComponent(b, accountName);
        b.append("?secret=");
        b.append(Base32.encode(secret).replace("=", ""));
        b.append("&issuer=");
        appendUriComponent(b, issuer);
        b.append("&algorithm=");
        b.append(alg);
        b.append("&digits=");
        b.append(digits);
        b.append("&period=");
        b.append(stepSeconds);
        return b.toString();
    }

    /// Convenience overload using the typical 6 digits / 30 seconds / SHA-1
    /// settings most authenticator apps expect.
    public static String otpauthUri(String issuer, String accountName, byte[] secret) {
        return otpauthUri(issuer, accountName, secret, 6, 30, Hash.SHA1);
    }

    private static String uriAlgorithm(String hashAlgorithm) {
        String n = hashAlgorithm == null ? "" : MessageDigestImpl.normalise(hashAlgorithm);
        if ("SHA1".equals(n)) {
            return "SHA1";
        }
        if ("SHA256".equals(n)) {
            return "SHA256";
        }
        if ("SHA512".equals(n)) {
            return "SHA512";
        }
        throw new CryptoException("unsupported OTP URI algorithm: " + hashAlgorithm);
    }

    /// Minimal RFC 3986 percent-encoder for URI path / query segments. The
    /// Google Authenticator KeyUri format only ever contains issuer + account
    /// name written by the host app, so we only need to escape the small set
    /// of unsafe characters that show up there in practice.
    private static void appendUriComponent(StringBuilder out, String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean unreserved = (c >= 'A' && c <= 'Z')
                    || (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9')
                    || c == '-' || c == '_' || c == '.' || c == '~';
            if (unreserved) {
                out.append(c);
            } else if (c < 0x80) {
                appendPercent(out, c);
            } else {
                // UTF-8 encode multi-byte chars
                if (c < 0x800) {
                    appendPercent(out, 0xc0 | (c >>> 6));
                    appendPercent(out, 0x80 | (c & 0x3f));
                } else {
                    appendPercent(out, 0xe0 | (c >>> 12));
                    appendPercent(out, 0x80 | ((c >>> 6) & 0x3f));
                    appendPercent(out, 0x80 | (c & 0x3f));
                }
            }
        }
    }

    private static void appendPercent(StringBuilder out, int b) {
        out.append('%');
        out.append(HEX[(b >>> 4) & 0x0f]);
        out.append(HEX[b & 0x0f]);
    }

    private static final char[] HEX = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    /// Verifies a TOTP code with full parameter control.
    public static boolean verifyTotp(byte[] secret, String code, int tolerance,
                                     long currentTimeMillis, int stepSeconds,
                                     int digits, String hashAlgorithm) {
        if (code == null) {
            return false;
        }
        long counter = (currentTimeMillis / 1000L) / stepSeconds;
        for (int s = -tolerance; s <= tolerance; s++) {
            String candidate = hotp(secret, counter + s, digits, hashAlgorithm);
            if (constantTimeEqualsString(candidate, code.trim())) {
                return true;
            }
        }
        return false;
    }

    private static boolean constantTimeEqualsString(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= (a.charAt(i) ^ b.charAt(i));
        }
        return diff == 0;
    }

    private static String pad(String s, int digits) {
        if (s.length() >= digits) {
            return s;
        }
        StringBuilder b = new StringBuilder(digits);
        for (int i = s.length(); i < digits; i++) {
            b.append('0');
        }
        b.append(s);
        return b.toString();
    }
}
