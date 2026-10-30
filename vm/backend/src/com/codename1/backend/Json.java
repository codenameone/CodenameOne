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
package com.codename1.backend;

import com.codename1.io.JSONParser;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/// JSON for server-side binaries: the core's strict reader and a streaming writer.
///
/// Reading is com.codename1.io.JSONParser#parseStrict, the same code the device
/// runs (JSONParser is shared with the backend). This class once carried a parser
/// of its own because JSONParser reached com.codename1.processing.Result and,
/// through it, the XML DOM; that dependency is gone. Writing stays here because it
/// streams into a ByteSink and honours Writable and Encoder, which the core writer
/// has no notion of.
///
/// Values map as: object to LinkedHashMap (insertion-ordered so a round trip is
/// stable), array to ArrayList, string to String, number to Double or Long,
/// true/false to Boolean, null to null. Anything malformed -- trailing content, a
/// raw control character in a string, a number outside the JSON grammar or a
/// long, nesting deeper than 512 -- throws IOException.
public final class Json {
    private Json() {
    }

    /// Parses a JSON object. Throws IOException on anything malformed.
    public static Map parseObject(String json) throws IOException {
        Object value = parse(json);
        if (!(value instanceof Map)) {
            throw new IOException("Expected a JSON object");
        }
        return (Map) value;
    }

    /// Parses any JSON value. Throws IOException on anything malformed.
    public static Object parse(String json) throws IOException {
        return JSONParser.parseStrict(json);
    }

    // ------------------------------------------------------------------
    // Writing
    // ------------------------------------------------------------------

    public static String write(Object value) {
        StringBuilder out = new StringBuilder();
        writeValue(out, value);
        return out.toString();
    }

    /// Writes JSON as UTF-8 bytes straight into a reusable buffer.
    ///
    /// The String-returning form above builds a StringBuilder, grows its char\[\]
    /// several times, copies it into a String and then encodes that to bytes --
    /// four allocations for a document the server is about to write to a socket
    /// and discard. Measured on a small JSON response, that chain was most of the
    /// per-request allocation once the response head had been dealt with, and it
    /// kept the collector's run-ahead cap firing.
    ///
    /// Same output as [#write(Object)], byte for byte.
    public static void write(Object value, ByteSink out) {
        writeValue(out, value);
    }

    /// A value that knows how to write itself as JSON.
    ///
    /// The point of this interface is to let generated code skip the Map. A codec
    /// emitted by the annotation processor knows its field names and types at build
    /// time, so it can write straight into the sink; without somewhere to hang that,
    /// a handler's return value has to become a LinkedHashMap first and be walked
    /// back with instanceof dispatch per value. Measured on the benchmark's
    /// one-field object, just removing the per-request map was worth 22%.
    public interface Writable {
        void writeTo(ByteSink out);
    }

    /// Writes values of one type as JSON, shared by every request that answers
    /// with that type.
    ///
    /// The stateless counterpart of [Writable]. A Writable carries the value it
    /// writes, so a route answering with a codec had to allocate one per request
    /// to hold that value; an Encoder is handed the value instead, so a generated
    /// route keeps ONE in a static field and a request allocates only what the
    /// method returned. See `HttpServer.Request.respondJson(int, Object, Encoder)`.
    public interface Encoder {
        /// Writes `value`, never null, into `out`.
        void write(Object value, ByteSink out);
    }

    /// One JSON string, escaped, straight into `out`.
    ///
    /// Public because generated code calls it. A codec that knows its field types
    /// at build time emits a direct call here instead of putting the value in a Map
    /// and letting [#write] rediscover its type at run time.
    public static void writeString(String value, ByteSink out) {
        if (value == null) {
            out.putAscii("null");
            return;
        }
        writeString(out, value);
    }

    /// One JSON value of unknown type, for the cases a generated codec cannot
    /// resolve statically (an unmodelled java.* type, a heterogeneous collection).
    /// The generated code uses the typed calls wherever it can and falls back here
    /// only where it must.
    public static void writeValue(Object value, ByteSink out) {
        writeValue(out, value);
    }

    private static void writeValue(ByteSink out, Object value) {
        if (value instanceof Writable) {
            // Checked first: a Writable is a DTO with a generated writer, and the
            // clauses below would otherwise fall through to its toString().
            ((Writable) value).writeTo(out);
            return;
        }
        if (value == null) {
            out.putAscii("null");
            return;
        }
        if (value instanceof String) {
            writeString(out, (String) value);
            return;
        }
        if (value instanceof Integer || value instanceof Long
                || value instanceof Short || value instanceof Byte) {
            out.putNumber(((Number) value).longValue());
            return;
        }
        if (value instanceof Boolean) {
            out.putAscii(((Boolean) value).booleanValue() ? "true" : "false");
            return;
        }
        if (value instanceof Double || value instanceof Float) {
            double d = ((Number) value).doubleValue();
            // JSON has no Infinity or NaN; emitting them produces a document no
            // parser will read back. Same rule as the String form.
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                out.putAscii("null");
                return;
            }
            // The VALUE's own spelling, not the widened one. Float 1.2f widened
            // to double prints as 1.2000000476837158, while the String writer
            // prints Float.toString and gets 1.2 -- so one handler answered two
            // different numbers depending on which protocol the client happened
            // to negotiate, since HTTP/1.1 writes through this sink and HTTP/2
            // through the other. The two have to agree; the byte[] branch below
            // carries the same warning for the same reason.
            out.putAscii(value.toString());
            return;
        }
        if (value instanceof Map) {
            out.put('{');
            Map map = (Map) value;
            Iterator it = map.entrySet().iterator();
            boolean first = true;
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                Object key = entry.getKey();
                if (!first) {
                    out.put(',');
                }
                first = false;
                writeString(out, key == null ? "null" : String.valueOf(key));
                out.put(':');
                writeValue(out, entry.getValue());
            }
            out.put('}');
            return;
        }
        if (value instanceof List) {
            out.put('[');
            List list = (List) value;
            for (int iter = 0 ; iter < list.size() ; iter++) {
                if (iter > 0) {
                    out.put(',');
                }
                writeValue(out, list.get(iter));
            }
            out.put(']');
            return;
        }
        if (value instanceof Collection) {
            // A Set is a JSON array too. Falling through to the String branch below
            // wrote its toString() as a quoted "[a, b]", which parses as a string and
            // is silently the wrong shape rather than an error. Indexed above because
            // a List answers get(i) without building an iterator.
            out.put('[');
            Iterator it = ((Collection) value).iterator();
            boolean first = true;
            while (it.hasNext()) {
                if (!first) {
                    out.put(',');
                }
                first = false;
                writeValue(out, it.next());
            }
            out.put(']');
            return;
        }
        if (value instanceof byte[]) {
            writeString(out, Base64Url.encode((byte[]) value));
            return;
        }
        writeString(out, String.valueOf(value));
    }

    private static void writeString(ByteSink out, String value) {
        out.put('"');
        int n = value.length();
        for (int iter = 0 ; iter < n ; iter++) {
            char c = value.charAt(iter);
            switch (c) {
                case '"':  out.putAscii("\\\""); break;
                case '\\': out.putAscii("\\\\"); break;
                case '\n': out.putAscii("\\n"); break;
                case '\r': out.putAscii("\\r"); break;
                case '\t': out.putAscii("\\t"); break;
                case '\b': out.putAscii("\\b"); break;
                case '\f': out.putAscii("\\f"); break;
                default:
                    if (c < 0x20) {
                        // Control characters must be escaped, and the six-character
                        // form is the only one JSON allows for those without a
                        // short escape. (Spelling it out rather than writing the
                        // escape prefix: Java expands that sequence inside
                        // COMMENTS too, and the file stops compiling.)
                        out.putAscii("\\u00");
                        out.put(hexDigit((c >> 4) & 0xf));
                        out.put(hexDigit(c & 0xf));
                    } else if (c < 0x80) {
                        out.put(c);
                    } else if (c >= 0xd800 && c <= 0xdbff && iter + 1 < n
                            && value.charAt(iter + 1) >= 0xdc00
                            && value.charAt(iter + 1) <= 0xdfff) {
                        // A surrogate PAIR is one code point. Encoding the halves
                        // separately produced two replacement characters -- an
                        // emoji came out as garbage -- which is what comparing
                        // this writer's bytes against the String form caught.
                        out.putCodePoint(0x10000 + ((c - 0xd800) << 10)
                                + (value.charAt(iter + 1) - 0xdc00));
                        iter++;
                    } else if (c >= 0xd800 && c <= 0xdfff) {
                        // An unpaired surrogate has no UTF-8 form at all, so it
                        // cannot be written literally -- it has to be escaped or
                        // substituted. The escape is the only one of the two that
                        // loses nothing, and it is what the String form does too,
                        // so both writers stay byte for byte identical.
                        out.putAscii("\\u");
                        out.put(hexDigit((c >> 12) & 0xf));
                        out.put(hexDigit((c >> 8) & 0xf));
                        out.put(hexDigit((c >> 4) & 0xf));
                        out.put(hexDigit(c & 0xf));
                    } else {
                        out.putCodePoint(c);
                    }
                    break;
            }
        }
        out.put('"');
    }

    private static int hexDigit(int nibble) {
        return nibble < 10 ? '0' + nibble : 'a' + (nibble - 10);
    }

    private static void writeValue(StringBuilder out, Object value) {
        if (value == null) {
            out.append("null");
            return;
        }
        // Before the String branch, and for the same reason the sink writer checks
        // it first: a Writable is a DTO carrying its own generated writer. Without
        // this it reached the quoting branch below and was emitted as the JSON
        // STRING of its toString(), so one handler returned an object over HTTP/1.1
        // and unusable text over HTTP/2 -- the two writers disagreeing about a
        // value's type, exactly as they did over Short and Byte.
        if (value instanceof Writable) {
            ByteSink sink = new ByteSink(256);
            ((Writable) value).writeTo(sink);
            try {
                out.append(new String(sink.bytes(), 0, sink.length(), "UTF-8"));
            } catch (java.io.UnsupportedEncodingException never) {
                // UTF-8 is required of every VM.
                throw new IllegalStateException(never.toString(), never);
            }
            return;
        }
        if (value instanceof String) {
            writeString(out, (String) value);
            return;
        }
        // Short and Byte belong here with the other integral types. The ByteSink
        // writer already treats them as numbers, and leaving them out here sent
        // them to the quoting branch below, so Json.write(Short) produced "1"
        // where the sink produced 1 -- the same value with a different JSON type
        // depending on which writer the caller reached.
        if (value instanceof Boolean || value instanceof Integer || value instanceof Long
                || value instanceof Short || value instanceof Byte) {
            out.append(value.toString());
            return;
        }
        if (value instanceof Double || value instanceof Float) {
            double d = ((Number) value).doubleValue();
            // JSON has no Infinity or NaN; emitting them produces a document no
            // parser will read back.
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                out.append("null");
            } else {
                out.append(value.toString());
            }
            return;
        }
        if (value instanceof Map) {
            Map map = (Map) value;
            out.append('{');
            boolean first = true;
            Iterator it = map.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                if (!first) {
                    out.append(',');
                }
                first = false;
                writeString(out, String.valueOf(entry.getKey()));
                out.append(':');
                writeValue(out, entry.getValue());
            }
            out.append('}');
            return;
        }
        if (value instanceof List) {
            List list = (List) value;
            out.append('[');
            for (int iter = 0 ; iter < list.size() ; iter++) {
                if (iter > 0) {
                    out.append(',');
                }
                writeValue(out, list.get(iter));
            }
            out.append(']');
            return;
        }
        if (value instanceof byte[]) {
            // As base64url, which is what the byte-sink writer does. The two have to
            // agree: HTTP/1.1 writes through the sink and HTTP/2 through this one, so
            // a disagreement means a BLOB comes back readable over one protocol and
            // as "[B@1a2b3c" over the other, from the same handler.
            writeString(out, Base64Url.encode((byte[]) value));
            return;
        }
        if (value instanceof Collection) {
            // As above: the two writers have to agree on what a Set is.
            out.append('[');
            Iterator it = ((Collection) value).iterator();
            boolean first = true;
            while (it.hasNext()) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                writeValue(out, it.next());
            }
            out.append(']');
            return;
        }
        writeString(out, value.toString());
    }

    private static void writeString(StringBuilder out, String value) {
        out.append('"');
        for (int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            switch (c) {
                case '"':  out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\n': out.append("\\n");  break;
                case '\r': out.append("\\r");  break;
                case '\t': out.append("\\t");  break;
                case '\b': out.append("\\b");  break;
                case '\f': out.append("\\f");  break;
                default:
                    if (c < 0x20) {
                        String hex = Integer.toHexString(c);
                        out.append("\\u");
                        for (int pad = hex.length() ; pad < 4 ; pad++) {
                            out.append('0');
                        }
                        out.append(hex);
                    } else if (c >= 0xd800 && c <= 0xdbff && iter + 1 < value.length()
                            && value.charAt(iter + 1) >= 0xdc00
                            && value.charAt(iter + 1) <= 0xdfff) {
                        out.append(c);
                        iter++;
                        out.append(value.charAt(iter));
                    } else if (c >= 0xd800 && c <= 0xdfff) {
                        // Unpaired: appending it produces a String that no UTF-8
                        // encoder can represent, so it silently became '?' on the
                        // wire. The escape keeps the value intact and matches what
                        // the byte writer emits.
                        out.append("\\u");
                        out.append(Integer.toHexString(c));
                    } else {
                        out.append(c);
                    }
            }
        }
        out.append('"');
    }
}
