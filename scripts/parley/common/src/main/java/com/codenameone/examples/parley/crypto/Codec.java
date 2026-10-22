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
package com.codenameone.examples.parley.crypto;

import com.codename1.io.JSONParser;
import com.codename1.util.Base64;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

/// The small conversions every part of the protocol needs: base64, UTF-8,
/// joined byte strings and numbers read out of parsed JSON.
public final class Codec {
    private Codec() {
    }

    public static String b64(byte[] bytes) {
        return Base64.encodeNoNewline(bytes);
    }

    /// Decodes base64, answering null for a value that is missing or is not
    /// base64, so a damaged field fails a check instead of throwing.
    public static byte[] unb64(Object value) {
        if (!(value instanceof String) || ((String) value).length() == 0) {
            return null;
        }
        try {
            return Base64.decode(((String) value).getBytes("UTF-8"));
        } catch (UnsupportedEncodingException | RuntimeException bad) {
            return null;
        }
    }

    public static byte[] utf8(String text) {
        try {
            return text.getBytes("UTF-8");
        } catch (UnsupportedEncodingException never) {
            throw new IllegalStateException(never.getMessage());
        }
    }

    public static String text(byte[] bytes) {
        try {
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException never) {
            throw new IllegalStateException(never.getMessage());
        }
    }

    public static byte[] concat(byte[]... parts) {
        int length = 0;
        for (byte[] part : parts) {
            length += part.length;
        }
        byte[] out = new byte[length];
        int at = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, out, at, part.length);
            at += part.length;
        }
        return out;
    }

    public static byte[] slice(byte[] bytes, int from, int length) {
        byte[] out = new byte[length];
        System.arraycopy(bytes, from, out, 0, length);
        return out;
    }

    /// A number out of parsed JSON, which arrives as a Double, a Long or an
    /// Integer depending on the parser's settings.
    public static long num(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException notANumber) {
                return 0;
            }
        }
        return 0;
    }

    public static String str(Object value) {
        return value instanceof String ? (String) value : "";
    }

    public static String json(Map<String, ?> map) {
        return JSONParser.mapToJson(map);
    }

    /// Parses a JSON object; null for anything else.
    public static Map<String, Object> parse(String json) {
        if (json == null || json.length() == 0) {
            return null;
        }
        try {
            return JSONParser.parseJSON(json);
        } catch (IOException | RuntimeException bad) {
            return null;
        }
    }
}
