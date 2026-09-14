/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.security.vault;

/// Byte-level primitives the envelope format is defined in terms of.
///
/// Package private and deliberately self-contained. Everything here is specified rather than
/// inherited from a platform: a wire format that two ports must agree on byte for byte cannot be
/// built on `String.getBytes("UTF-8")`, whose behaviour for an unpaired surrogate differs between
/// runtimes -- some emit `?`, some emit U+FFFD, some throw -- which would make a password
/// containing one derive a different key on Android than in the browser.
final class Bytes {

    private Bytes() {
    }

    /// UTF-8 encoding of a `String`, specified here rather than delegated.
    ///
    /// Code points are encoded in the standard 1 to 4 byte forms. A surrogate that is not part of
    /// a valid pair -- high with no low following, or a low with no high before -- is encoded as
    /// U+FFFD, the replacement character, in its three byte form. That is one of the behaviours a
    /// platform might have picked; the point is that all of them now pick this one.
    ///
    /// No normalization is applied. Two strings that a user would call the same password but that
    /// differ in code points (a precomposed `e` with an acute accent against the two code point
    /// spelling) derive different keys, on every platform alike. Normalizing here would be worse
    /// than not: the core has no Unicode tables, so it could only normalize approximately, and an
    /// approximate normalization that differs between ports is the failure this avoids. An
    /// application that needs to accept both spellings must normalize before it calls.
    static byte[] utf8(String value) {
        if (value == null) {
            return new byte[0];
        }
        int length = value.length();
        // Worst case three bytes per char; a surrogate pair is two chars and four bytes, so this
        // bound holds for them too.
        byte[] out = new byte[length * 3];
        int at = 0;
        for (int iter = 0; iter < length; iter++) {
            int cp = value.charAt(iter);
            if (cp >= 0xd800 && cp <= 0xdbff) {
                int next = iter + 1 < length ? value.charAt(iter + 1) : -1;
                if (next >= 0xdc00 && next <= 0xdfff) {
                    cp = 0x10000 + ((cp - 0xd800) << 10) + (next - 0xdc00);
                    iter++;
                } else {
                    cp = 0xfffd;
                }
            } else if (cp >= 0xdc00 && cp <= 0xdfff) {
                cp = 0xfffd;
            }
            if (cp < 0x80) {
                out[at++] = (byte) cp;
            } else if (cp < 0x800) {
                out[at++] = (byte) (0xc0 | (cp >> 6));
                out[at++] = (byte) (0x80 | (cp & 0x3f));
            } else if (cp < 0x10000) {
                out[at++] = (byte) (0xe0 | (cp >> 12));
                out[at++] = (byte) (0x80 | ((cp >> 6) & 0x3f));
                out[at++] = (byte) (0x80 | (cp & 0x3f));
            } else {
                out[at++] = (byte) (0xf0 | (cp >> 18));
                out[at++] = (byte) (0x80 | ((cp >> 12) & 0x3f));
                out[at++] = (byte) (0x80 | ((cp >> 6) & 0x3f));
                out[at++] = (byte) (0x80 | (cp & 0x3f));
            }
        }
        byte[] exact = new byte[at];
        System.arraycopy(out, 0, exact, 0, at);
        zero(out);
        return exact;
    }

    /// The same encoding for a `char[]`, so a password never has to become a `String`.
    ///
    /// A `String` cannot be cleared: it is immutable and lives until it is collected, which on
    /// every port here means an unbounded window in which a heap dump contains the password. A
    /// `char[]` can be overwritten, and [#zero(char[])] does. This is best effort and not proof
    /// of erasure -- a managed runtime may have copied the array during a collection, and the
    /// browser certainly may -- but it removes the copy the application controls.
    static byte[] utf8(char[] value) {
        if (value == null) {
            return new byte[0];
        }
        // Routed through the String form would allocate exactly the String this exists to avoid,
        // so the loop is repeated against a char[] source.
        int length = value.length;
        byte[] out = new byte[length * 3];
        int at = 0;
        for (int iter = 0; iter < length; iter++) {
            int cp = value[iter];
            if (cp >= 0xd800 && cp <= 0xdbff) {
                int next = iter + 1 < length ? value[iter + 1] : -1;
                if (next >= 0xdc00 && next <= 0xdfff) {
                    cp = 0x10000 + ((cp - 0xd800) << 10) + (next - 0xdc00);
                    iter++;
                } else {
                    cp = 0xfffd;
                }
            } else if (cp >= 0xdc00 && cp <= 0xdfff) {
                cp = 0xfffd;
            }
            if (cp < 0x80) {
                out[at++] = (byte) cp;
            } else if (cp < 0x800) {
                out[at++] = (byte) (0xc0 | (cp >> 6));
                out[at++] = (byte) (0x80 | (cp & 0x3f));
            } else if (cp < 0x10000) {
                out[at++] = (byte) (0xe0 | (cp >> 12));
                out[at++] = (byte) (0x80 | ((cp >> 6) & 0x3f));
                out[at++] = (byte) (0x80 | (cp & 0x3f));
            } else {
                out[at++] = (byte) (0xf0 | (cp >> 18));
                out[at++] = (byte) (0x80 | ((cp >> 12) & 0x3f));
                out[at++] = (byte) (0x80 | ((cp >> 6) & 0x3f));
                out[at++] = (byte) (0x80 | (cp & 0x3f));
            }
        }
        byte[] exact = new byte[at];
        System.arraycopy(out, 0, exact, 0, at);
        zero(out);
        return exact;
    }

    /// Decodes bytes this class produced back to a `String`. Malformed sequences become U+FFFD
    /// rather than raising, because the caller has already authenticated the bytes: a decode
    /// failure at this point means a bug, and throwing from inside a decrypt would leak where.
    static String fromUtf8(byte[] data, int offset, int length) {
        if (data == null || length <= 0) {
            return "";
        }
        StringBuilder b = new StringBuilder(length);
        int at = offset;
        int end = offset + length;
        while (at < end) {
            int first = data[at++] & 0xff;
            int cp;
            int extra;
            if (first < 0x80) {
                cp = first;
                extra = 0;
            } else if ((first & 0xe0) == 0xc0) {
                cp = first & 0x1f;
                extra = 1;
            } else if ((first & 0xf0) == 0xe0) {
                cp = first & 0x0f;
                extra = 2;
            } else if ((first & 0xf8) == 0xf0) {
                cp = first & 0x07;
                extra = 3;
            } else {
                b.append(REPLACEMENT);
                continue;
            }
            if (at + extra > end) {
                b.append(REPLACEMENT);
                break;
            }
            boolean valid = true;
            for (int iter = 0; iter < extra; iter++) {
                int next = data[at + iter] & 0xff;
                if ((next & 0xc0) != 0x80) {
                    valid = false;
                    break;
                }
                cp = (cp << 6) | (next & 0x3f);
            }
            if (!valid) {
                b.append(REPLACEMENT);
                continue;
            }
            at += extra;
            if (cp > 0x10ffff) {
                b.append(REPLACEMENT);
            } else if (cp >= 0x10000) {
                cp -= 0x10000;
                b.append((char) (0xd800 + (cp >> 10)));
                b.append((char) (0xdc00 + (cp & 0x3ff)));
            } else {
                b.append((char) cp);
            }
        }
        return b.toString();
    }

    /// Big-endian 32 bit write. Every multi-byte field in the envelope is big-endian, stated once
    /// here so a port implementing the format elsewhere has one rule to follow.
    static void putInt(byte[] out, int offset, int value) {
        out[offset] = (byte) (value >>> 24);
        out[offset + 1] = (byte) (value >>> 16);
        out[offset + 2] = (byte) (value >>> 8);
        out[offset + 3] = (byte) value;
    }

    /// Big-endian 32 bit read.
    static int getInt(byte[] in, int offset) {
        return ((in[offset] & 0xff) << 24)
                | ((in[offset + 1] & 0xff) << 16)
                | ((in[offset + 2] & 0xff) << 8)
                | (in[offset + 3] & 0xff);
    }

    /// A copy of a slice, or an empty array for a zero length.
    static byte[] slice(byte[] in, int offset, int length) {
        byte[] out = new byte[length];
        if (length > 0) {
            System.arraycopy(in, offset, out, 0, length);
        }
        return out;
    }

    /// Concatenates, skipping nulls.
    static byte[] concat(byte[] a, byte[] b) {
        int aLen = a == null ? 0 : a.length;
        int bLen = b == null ? 0 : b.length;
        byte[] out = new byte[aLen + bLen];
        if (aLen > 0) {
            System.arraycopy(a, 0, out, 0, aLen);
        }
        if (bLen > 0) {
            System.arraycopy(b, 0, out, aLen, bLen);
        }
        return out;
    }

    /// Comparison whose running time does not depend on where the first difference is.
    ///
    /// Used wherever a tag, a key id or a derived value is compared. `java.util.Arrays.equals`
    /// returns at the first mismatching byte, which over enough attempts tells an attacker how
    /// much of a guess was right.
    static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null) {
            // Two nulls are equal, a null against an array is not. Written as two null tests
            // rather than `a == b` so it is a comparison against null and not between two
            // references -- equals() is not the alternative here, because Arrays.equals is
            // exactly the early-returning comparison this method exists to avoid.
            return a == null && b == null;
        }
        if (a.length != b.length) {
            return false;
        }
        int diff = 0;
        for (int iter = 0; iter < a.length; iter++) {
            diff |= a[iter] ^ b[iter];
        }
        return diff == 0;
    }

    /// Overwrites a buffer. Best effort: see the note on [#utf8(char[])].
    static void zero(byte[] data) {
        if (data != null) {
            for (int iter = 0; iter < data.length; iter++) {
                data[iter] = 0;
            }
        }
    }

    /// Overwrites a character buffer, for a password the caller is done with.
    static void zero(char[] data) {
        if (data != null) {
            for (int iter = 0; iter < data.length; iter++) {
                data[iter] = 0;
            }
        }
    }

    /// Lower case hex, for the few places a value has to become a `String` -- a storage key, a
    /// key id in a log line. Never used for key material that has somewhere better to go.
    static String toHex(byte[] data) {
        if (data == null) {
            return "";
        }
        StringBuilder b = new StringBuilder(data.length * 2);
        for (byte raw : data) {
            int v = raw & 0xff;
            b.append(HEX.charAt(v >>> 4));
            b.append(HEX.charAt(v & 0x0f));
        }
        return b.toString();
    }

    /// Parses what [#toHex(byte[])] wrote. Returns null for anything that is not an even length
    /// run of hex digits, so a caller can tell a corrupt entry from a short one without a throw.
    static byte[] fromHex(String hex) {
        if (hex == null || (hex.length() & 1) != 0) {
            return null;
        }
        byte[] out = new byte[hex.length() / 2];
        for (int iter = 0; iter < out.length; iter++) {
            int hi = digit(hex.charAt(iter * 2));
            int lo = digit(hex.charAt(iter * 2 + 1));
            if (hi < 0 || lo < 0) {
                return null;
            }
            out[iter] = (byte) ((hi << 4) | lo);
        }
        return out;
    }

    private static int digit(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return -1;
    }

    private static final String HEX = "0123456789abcdef";

    /// U+FFFD, written as an escape because a Java source file in this tree must be ASCII.
    private static final char REPLACEMENT = '\ufffd';
}
