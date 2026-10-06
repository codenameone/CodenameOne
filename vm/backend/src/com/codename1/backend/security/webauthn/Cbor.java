/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend.security.webauthn;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Reads the CBOR (RFC 8949) an authenticator sends: an attestation object,
/// and the COSE key and the extensions inside authenticator data.
///
/// It reads what those are made of and refuses everything else:
///
/// | CBOR | Java |
/// |---|---|
/// | unsigned and negative integers that fit 64 bits signed | `Long` |
/// | byte string | `byte[]` |
/// | text string, which must be well-formed UTF-8 | `String` |
/// | array | `List` |
/// | map whose keys are integers or text, each once | `Map`, in the order sent |
/// | `false`, `true` | `Boolean` |
/// | `null` | `null` |
///
/// Strings, arrays and maps may be of indefinite length, which RFC 8949 allows
/// and some authenticators use. Tags, floating point numbers, `undefined`, the
/// other simple values and the reserved encodings are refused: nothing a
/// ceremony reads is one, and what is not read is not parsed.
///
/// The input is the client's, so nothing is taken on its word. A length is
/// checked against the bytes that remain before anything is allocated for it;
/// an array or a map is given no room ahead of its elements; nesting is
/// limited to [#MAX_DEPTH]; and an input over [#MAX_BYTES] is refused before
/// it is looked at. Every failure is a [WebAuthnException] with the reason
/// [WebAuthnException#MALFORMED_CBOR].
final class Cbor {
    /// The deepest nesting read. An attestation statement nests four deep.
    static final int MAX_DEPTH = 8;
    /// The largest input read, in bytes.
    static final int MAX_BYTES = 65536;

    private static final int BREAK = -2;

    private final byte[] data;
    private final int end;
    private int at;

    private Cbor(byte[] data, int offset, int end) {
        this.data = data;
        this.at = offset;
        this.end = end;
    }

    /// The one value `data` is; anything after it is refused.
    static Object decode(byte[] data) {
        int[] next = new int[1];
        Object value = decode(data, 0, next);
        if (next[0] != data.length) {
            throw malformed((data.length - next[0]) + " bytes follow the value");
        }
        return value;
    }

    /// The value that starts at `offset`.
    ///
    /// @param next receives, at index 0, the offset of the byte after the value
    static Object decode(byte[] data, int offset, int[] next) {
        if (data == null) {
            throw malformed("no input");
        }
        if (data.length > MAX_BYTES) {
            throw malformed("the input is " + data.length + " bytes, and at most " + MAX_BYTES
                    + " are read");
        }
        if (offset < 0 || offset >= data.length) {
            throw malformed("the input ends where a value should start");
        }
        Cbor reader = new Cbor(data, offset, data.length);
        Object[] out = new Object[1];
        if (reader.item(0, out) == BREAK) {
            throw malformed("a break outside a value of indefinite length");
        }
        next[0] = reader.at;
        return out[0];
    }

    private static WebAuthnException malformed(String what) {
        return new WebAuthnException(WebAuthnException.MALFORMED_CBOR, "Malformed CBOR: " + what);
    }

    private int remaining() {
        return end - at;
    }

    private int octet() {
        if (at >= end) {
            throw malformed("the input ends inside a value");
        }
        return data[at++] & 0xff;
    }

    /// The argument of an item head: its value, or -1 for indefinite length.
    private long argument(int info) {
        if (info < 24) {
            return info;
        }
        int width;
        switch (info) {
            case 24: width = 1; break;
            case 25: width = 2; break;
            case 26: width = 4; break;
            case 27: width = 8; break;
            case 31: return -1;
            default: throw malformed("the reserved additional information " + info);
        }
        if (remaining() < width) {
            throw malformed("the input ends inside a length");
        }
        long value = 0;
        for (int iter = 0 ; iter < width ; iter++) {
            value = (value << 8) | (data[at++] & 0xff);
        }
        if (value < 0) {
            // Over 63 bits: more than any length, and more than a Long holds.
            throw malformed("a number over 63 bits");
        }
        return value;
    }

    /// Reads one item into `out[0]`.
    ///
    /// @return the item's major type, or [#BREAK] for the break code
    private int item(int depth, Object[] out) {
        if (depth > MAX_DEPTH) {
            throw malformed("nested deeper than " + MAX_DEPTH);
        }
        int head = octet();
        int major = head >> 5;
        int info = head & 0x1f;
        if (major == 7) {
            switch (info) {
                case 20: out[0] = Boolean.FALSE; return major;
                case 21: out[0] = Boolean.TRUE; return major;
                case 22: out[0] = null; return major;
                case 31: return BREAK;
                case 25:
                case 26:
                case 27: throw malformed("a floating point number");
                default: throw malformed("the simple value " + info);
            }
        }
        long argument = argument(info);
        switch (major) {
            case 0:
            case 1:
                if (argument < 0) {
                    throw malformed("an integer of indefinite length");
                }
                out[0] = Long.valueOf(major == 0 ? argument : -1 - argument);
                return major;
            case 2:
                out[0] = bytes(major, argument);
                return major;
            case 3:
                out[0] = text(bytes(major, argument));
                return major;
            case 4:
                out[0] = array(argument, depth);
                return major;
            case 5:
                out[0] = map(argument, depth);
                return major;
            default:
                throw malformed("a tag");
        }
    }

    private byte[] bytes(int major, long length) {
        if (length >= 0) {
            return definite(length);
        }
        // Indefinite: definite chunks of the same type up to a break. Nothing
        // is allocated for a chunk before it is known to be there, so the
        // total is bounded by the input.
        List<byte[]> chunks = new ArrayList<byte[]>();
        int total = 0;
        while (true) {
            int head = octet();
            if (head == 0xff) {
                break;
            }
            if (head >> 5 != major) {
                throw malformed("a chunk of another type inside a string");
            }
            long chunk = argument(head & 0x1f);
            if (chunk < 0) {
                throw malformed("a chunk of indefinite length inside a string");
            }
            byte[] piece = definite(chunk);
            chunks.add(piece);
            total += piece.length;
        }
        byte[] whole = new byte[total];
        int to = 0;
        for (byte[] piece : chunks) {
            System.arraycopy(piece, 0, whole, to, piece.length);
            to += piece.length;
        }
        return whole;
    }

    private byte[] definite(long length) {
        if (length > remaining()) {
            throw malformed("a string of " + length + " bytes with " + remaining() + " left");
        }
        byte[] out = new byte[(int) length];
        System.arraycopy(data, at, out, 0, out.length);
        at += out.length;
        return out;
    }

    private List<Object> array(long count, int depth) {
        // Every element is at least a byte.
        if (count > remaining()) {
            throw malformed("an array of " + count + " elements with " + remaining()
                    + " bytes left");
        }
        List<Object> out = new ArrayList<Object>();
        Object[] one = new Object[1];
        for (long iter = 0 ; count < 0 || iter < count ; iter++) {
            if (item(depth + 1, one) == BREAK) {
                if (count >= 0) {
                    throw malformed("a break inside an array of definite length");
                }
                break;
            }
            out.add(one[0]);
        }
        return out;
    }

    private Map<Object, Object> map(long count, int depth) {
        // Every entry is at least two bytes.
        if (count > remaining() / 2) {
            throw malformed("a map of " + count + " entries with " + remaining()
                    + " bytes left");
        }
        Map<Object, Object> out = new LinkedHashMap<Object, Object>();
        Object[] one = new Object[1];
        for (long iter = 0 ; count < 0 || iter < count ; iter++) {
            int kind = item(depth + 1, one);
            if (kind == BREAK) {
                if (count >= 0) {
                    throw malformed("a break inside a map of definite length");
                }
                break;
            }
            Object key = one[0];
            if (kind != 0 && kind != 1 && kind != 3) {
                throw malformed("a map key that is neither an integer nor text");
            }
            if (out.containsKey(key)) {
                throw malformed("the map key " + key + " twice");
            }
            if (item(depth + 1, one) == BREAK) {
                throw malformed("a break where a map value should be");
            }
            out.put(key, one[0]);
        }
        return out;
    }

    /// `utf8` as text, refused unless it is well-formed UTF-8: no overlong
    /// form, no surrogate, nothing past U+10FFFF, no sequence cut short.
    private static String text(byte[] utf8) {
        int iter = 0;
        while (iter < utf8.length) {
            int lead = utf8[iter] & 0xff;
            int more;
            int least;
            int code;
            if (lead < 0x80) {
                iter++;
                continue;
            } else if (lead >= 0xc2 && lead <= 0xdf) {
                more = 1;
                least = 0x80;
                code = lead & 0x1f;
            } else if (lead >= 0xe0 && lead <= 0xef) {
                more = 2;
                least = 0x800;
                code = lead & 0x0f;
            } else if (lead >= 0xf0 && lead <= 0xf4) {
                more = 3;
                least = 0x10000;
                code = lead & 0x07;
            } else {
                throw malformed("text that is not UTF-8");
            }
            if (iter + more >= utf8.length) {
                throw malformed("text that is not UTF-8");
            }
            for (int tail = 1 ; tail <= more ; tail++) {
                int next = utf8[iter + tail] & 0xff;
                if ((next & 0xc0) != 0x80) {
                    throw malformed("text that is not UTF-8");
                }
                code = (code << 6) | (next & 0x3f);
            }
            if (code < least || code > 0x10ffff || (code >= 0xd800 && code <= 0xdfff)) {
                throw malformed("text that is not UTF-8");
            }
            iter += more + 1;
        }
        try {
            return new String(utf8, "UTF-8");
        } catch (java.io.UnsupportedEncodingException err) {
            throw new IllegalStateException("UTF-8 is required", err);
        }
    }
}
