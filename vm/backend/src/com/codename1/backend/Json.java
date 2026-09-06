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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A self-contained JSON reader/writer for server-side binaries.
 *
 * Deliberately not com.codename1.io.JSONParser: that class reaches
 * com.codename1.processing.Result, which reaches com.codename1.xml.Element, so
 * reusing it would link an XML DOM into every server binary to parse a request
 * body. The mapping package has the same problem (Mapper imports Element).
 *
 * Values map as: object to LinkedHashMap (insertion-ordered so a round trip is
 * stable), array to ArrayList, string to String, number to Double or Long,
 * true/false to Boolean, null to null.
 */
public final class Json {
    private final String src;
    private int pos;

    private Json(String src) {
        this.src = src;
    }

    /** Parses a JSON object. Throws IOException on anything malformed. */
    public static Map parseObject(String json) throws IOException {
        Object value = parse(json);
        if(!(value instanceof Map)) {
            throw new IOException("Expected a JSON object");
        }
        return (Map)value;
    }

    public static Object parse(String json) throws IOException {
        if(json == null) {
            throw new IOException("No JSON to parse");
        }
        Json p = new Json(json);
        p.skipWhitespace();
        Object value = p.readValue();
        p.skipWhitespace();
        if(p.pos < p.src.length()) {
            throw new IOException("Trailing content at offset " + p.pos);
        }
        return value;
    }

    private Object readValue() throws IOException {
        if(pos >= src.length()) {
            throw new IOException("Unexpected end of JSON");
        }
        char c = src.charAt(pos);
        switch(c) {
            case '{': return readObject();
            case '[': return readArray();
            case '"': return readString();
            case 't': return readLiteral("true", Boolean.TRUE);
            case 'f': return readLiteral("false", Boolean.FALSE);
            case 'n': return readLiteral("null", null);
            default: return readNumber();
        }
    }

    private Map readObject() throws IOException {
        Map out = new LinkedHashMap();
        pos++; // {
        skipWhitespace();
        if(peek() == '}') {
            pos++;
            return out;
        }
        while(true) {
            skipWhitespace();
            if(peek() != '"') {
                throw new IOException("Expected a key at offset " + pos);
            }
            String key = readString();
            skipWhitespace();
            if(peek() != ':') {
                throw new IOException("Expected ':' at offset " + pos);
            }
            pos++;
            skipWhitespace();
            out.put(key, readValue());
            skipWhitespace();
            char c = peek();
            pos++;
            if(c == '}') {
                return out;
            }
            if(c != ',') {
                throw new IOException("Expected ',' or '}' at offset " + (pos - 1));
            }
        }
    }

    private List readArray() throws IOException {
        List out = new ArrayList();
        pos++; // [
        skipWhitespace();
        if(peek() == ']') {
            pos++;
            return out;
        }
        while(true) {
            skipWhitespace();
            out.add(readValue());
            skipWhitespace();
            char c = peek();
            pos++;
            if(c == ']') {
                return out;
            }
            if(c != ',') {
                throw new IOException("Expected ',' or ']' at offset " + (pos - 1));
            }
        }
    }

    private String readString() throws IOException {
        pos++; // opening quote
        StringBuilder out = new StringBuilder();
        while(true) {
            if(pos >= src.length()) {
                throw new IOException("Unterminated string");
            }
            char c = src.charAt(pos++);
            if(c == '"') {
                return out.toString();
            }
            if(c != '\\') {
                out.append(c);
                continue;
            }
            if(pos >= src.length()) {
                throw new IOException("Unterminated escape");
            }
            char esc = src.charAt(pos++);
            switch(esc) {
                case '"':  out.append('"');  break;
                case '\\': out.append('\\'); break;
                case '/':  out.append('/');  break;
                case 'b':  out.append('\b'); break;
                case 'f':  out.append('\f'); break;
                case 'n':  out.append('\n'); break;
                case 'r':  out.append('\r'); break;
                case 't':  out.append('\t'); break;
                case 'u':
                    if(pos + 4 > src.length()) {
                        throw new IOException("Truncated \\u escape");
                    }
                    try {
                        out.append((char)Integer.parseInt(src.substring(pos, pos + 4), 16));
                    } catch (NumberFormatException err) {
                        throw new IOException("Malformed \\u escape at offset " + pos);
                    }
                    pos += 4;
                    break;
                default:
                    throw new IOException("Unknown escape \\" + esc);
            }
        }
    }

    private Object readLiteral(String literal, Object value) throws IOException {
        if(!src.startsWith(literal, pos)) {
            throw new IOException("Expected " + literal + " at offset " + pos);
        }
        pos += literal.length();
        return value;
    }

    private Object readNumber() throws IOException {
        int start = pos;
        boolean floating = false;
        while(pos < src.length()) {
            char c = src.charAt(pos);
            if(c == '-' || c == '+' || (c >= '0' && c <= '9')) {
                pos++;
            } else if(c == '.' || c == 'e' || c == 'E') {
                floating = true;
                pos++;
            } else {
                break;
            }
        }
        if(start == pos) {
            throw new IOException("Expected a value at offset " + start);
        }
        String text = src.substring(start, pos);
        try {
            // Integers stay integers: a long round-tripped through double loses
            // precision above 2^53, and ids are exactly the values that get large.
            return floating ? (Object)Double.valueOf(Double.parseDouble(text))
                            : (Object)Long.valueOf(Long.parseLong(text));
        } catch (NumberFormatException err) {
            throw new IOException("Malformed number '" + text + "'");
        }
    }

    private char peek() throws IOException {
        if(pos >= src.length()) {
            throw new IOException("Unexpected end of JSON");
        }
        return src.charAt(pos);
    }

    private void skipWhitespace() {
        while(pos < src.length()) {
            char c = src.charAt(pos);
            if(c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                pos++;
            } else {
                break;
            }
        }
    }

    // ------------------------------------------------------------------
    // Writing
    // ------------------------------------------------------------------

    public static String write(Object value) {
        StringBuilder out = new StringBuilder();
        writeValue(out, value);
        return out.toString();
    }

    /**
     * Writes JSON as UTF-8 bytes straight into a reusable buffer.
     *
     * The String-returning form above builds a StringBuilder, grows its char[]
     * several times, copies it into a String and then encodes that to bytes --
     * four allocations for a document the server is about to write to a socket
     * and discard. Measured on a small JSON response, that chain was most of the
     * per-request allocation once the response head had been dealt with, and it
     * kept the collector's run-ahead cap firing.
     *
     * Same output as {@link #write(Object)}, byte for byte.
     */
    public static void write(Object value, ByteSink out) {
        writeValue(out, value);
    }

    /**
     * A value that knows how to write itself as JSON.
     *
     * The point of this interface is to let generated code skip the Map. A codec
     * emitted by the annotation processor knows its field names and types at build
     * time, so it can write straight into the sink; without somewhere to hang that,
     * a handler's return value has to become a LinkedHashMap first and be walked
     * back with instanceof dispatch per value. Measured on the benchmark's
     * one-field object, just removing the per-request map was worth 22%.
     */
    public interface Writable {
        void writeTo(ByteSink out);
    }

    /**
     * One JSON string, escaped, straight into {@code out}.
     *
     * Public because generated code calls it. A codec that knows its field types
     * at build time emits a direct call here instead of putting the value in a Map
     * and letting {@link #write} rediscover its type at run time.
     */
    public static void writeString(String value, ByteSink out) {
        if(value == null) {
            out.putAscii("null");
            return;
        }
        writeString(out, value);
    }

    /**
     * One JSON value of unknown type, for the cases a generated codec cannot
     * resolve statically (an unmodelled java.* type, a heterogeneous collection).
     * The generated code uses the typed calls wherever it can and falls back here
     * only where it must.
     */
    public static void writeValue(Object value, ByteSink out) {
        writeValue(out, value);
    }

    private static void writeValue(ByteSink out, Object value) {
        if(value instanceof Writable) {
            // Checked first: a Writable is a DTO with a generated writer, and the
            // clauses below would otherwise fall through to its toString().
            ((Writable)value).writeTo(out);
            return;
        }
        if(value == null) {
            out.putAscii("null");
            return;
        }
        if(value instanceof String) {
            writeString(out, (String)value);
            return;
        }
        if(value instanceof Integer || value instanceof Long
                || value instanceof Short || value instanceof Byte) {
            out.putNumber(((Number)value).longValue());
            return;
        }
        if(value instanceof Boolean) {
            out.putAscii(((Boolean)value).booleanValue() ? "true" : "false");
            return;
        }
        if(value instanceof Double || value instanceof Float) {
            double d = ((Number)value).doubleValue();
            // JSON has no Infinity or NaN; emitting them produces a document no
            // parser will read back. Same rule as the String form.
            if(Double.isNaN(d) || Double.isInfinite(d)) {
                out.putAscii("null");
                return;
            }
            out.putAscii(String.valueOf(d));
            return;
        }
        if(value instanceof Map) {
            out.put('{');
            Map map = (Map)value;
            java.util.Iterator it = map.keySet().iterator();
            boolean first = true;
            while(it.hasNext()) {
                Object key = it.next();
                if(!first) {
                    out.put(',');
                }
                first = false;
                writeString(out, key == null ? "null" : String.valueOf(key));
                out.put(':');
                writeValue(out, map.get(key));
            }
            out.put('}');
            return;
        }
        if(value instanceof List) {
            out.put('[');
            List list = (List)value;
            for(int iter = 0 ; iter < list.size() ; iter++) {
                if(iter > 0) {
                    out.put(',');
                }
                writeValue(out, list.get(iter));
            }
            out.put(']');
            return;
        }
        if(value instanceof byte[]) {
            writeString(out, Base64Url.encode((byte[])value));
            return;
        }
        writeString(out, String.valueOf(value));
    }

    private static void writeString(ByteSink out, String value) {
        out.put('"');
        int n = value.length();
        for(int iter = 0 ; iter < n ; iter++) {
            char c = value.charAt(iter);
            switch(c) {
                case '"':  out.putAscii("\\\""); break;
                case '\\': out.putAscii("\\\\"); break;
                case '\n': out.putAscii("\\n"); break;
                case '\r': out.putAscii("\\r"); break;
                case '\t': out.putAscii("\\t"); break;
                case '\b': out.putAscii("\\b"); break;
                case '\f': out.putAscii("\\f"); break;
                default:
                    if(c < 0x20) {
                        // Control characters must be escaped, and the six-character
                        // form is the only one JSON allows for those without a
                        // short escape. (Spelling it out rather than writing the
                        // escape prefix: Java expands that sequence inside
                        // COMMENTS too, and the file stops compiling.)
                        out.putAscii("\\u00");
                        out.put(hexDigit((c >> 4) & 0xf));
                        out.put(hexDigit(c & 0xf));
                    } else if(c < 0x80) {
                        out.put(c);
                    } else if(c >= 0xd800 && c <= 0xdbff && iter + 1 < n
                            && value.charAt(iter + 1) >= 0xdc00
                            && value.charAt(iter + 1) <= 0xdfff) {
                        // A surrogate PAIR is one code point. Encoding the halves
                        // separately produced two replacement characters -- an
                        // emoji came out as garbage -- which is what comparing
                        // this writer's bytes against the String form caught.
                        out.putCodePoint(0x10000 + ((c - 0xd800) << 10)
                                + (value.charAt(iter + 1) - 0xdc00));
                        iter++;
                    } else if(c >= 0xd800 && c <= 0xdfff) {
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
        if(value == null) {
            out.append("null");
            return;
        }
        if(value instanceof String) {
            writeString(out, (String)value);
            return;
        }
        if(value instanceof Boolean || value instanceof Integer || value instanceof Long) {
            out.append(value.toString());
            return;
        }
        if(value instanceof Double || value instanceof Float) {
            double d = ((Number)value).doubleValue();
            // JSON has no Infinity or NaN; emitting them produces a document no
            // parser will read back.
            if(Double.isNaN(d) || Double.isInfinite(d)) {
                out.append("null");
            } else {
                out.append(value.toString());
            }
            return;
        }
        if(value instanceof Map) {
            Map map = (Map)value;
            out.append('{');
            boolean first = true;
            java.util.Iterator it = map.keySet().iterator();
            while(it.hasNext()) {
                Object key = it.next();
                if(!first) {
                    out.append(',');
                }
                first = false;
                writeString(out, String.valueOf(key));
                out.append(':');
                writeValue(out, map.get(key));
            }
            out.append('}');
            return;
        }
        if(value instanceof List) {
            List list = (List)value;
            out.append('[');
            for(int iter = 0 ; iter < list.size() ; iter++) {
                if(iter > 0) {
                    out.append(',');
                }
                writeValue(out, list.get(iter));
            }
            out.append(']');
            return;
        }
        writeString(out, value.toString());
    }

    private static void writeString(StringBuilder out, String value) {
        out.append('"');
        for(int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            switch(c) {
                case '"':  out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\n': out.append("\\n");  break;
                case '\r': out.append("\\r");  break;
                case '\t': out.append("\\t");  break;
                case '\b': out.append("\\b");  break;
                case '\f': out.append("\\f");  break;
                default:
                    if(c < 0x20) {
                        String hex = Integer.toHexString(c);
                        out.append("\\u");
                        for(int pad = hex.length() ; pad < 4 ; pad++) {
                            out.append('0');
                        }
                        out.append(hex);
                    } else if(c >= 0xd800 && c <= 0xdbff && iter + 1 < value.length()
                            && value.charAt(iter + 1) >= 0xdc00
                            && value.charAt(iter + 1) <= 0xdfff) {
                        out.append(c);
                        iter++;
                        out.append(value.charAt(iter));
                    } else if(c >= 0xd800 && c <= 0xdfff) {
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
