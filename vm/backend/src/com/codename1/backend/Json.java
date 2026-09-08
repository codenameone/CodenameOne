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
import java.util.Collection;
import java.util.Iterator;
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

    /**
     * How deep a document may nest before it is refused.
     *
     * The parser is recursive, so nesting depth is stack depth, and a body is
     * whatever the client sent. Without a bound a few kilobytes of "[[[[..." reach
     * StackOverflowError -- which is an Error, so neither the handler's catch nor
     * the server's catch of Exception sees it, and the thread dies rather than the
     * request failing. 512 is far past any real document and far short of the
     * stack.
     */
    private static final int MAX_DEPTH = 512;

    private int depth;

    private Object readValue() throws IOException {
        if(pos >= src.length()) {
            throw new IOException("Unexpected end of JSON");
        }
        char c = src.charAt(pos);
        switch(c) {
            case '{': return readNested(true);
            case '[': return readNested(false);
            case '"': return readString();
            case 't': return readLiteral("true", Boolean.TRUE);
            case 'f': return readLiteral("false", Boolean.FALSE);
            case 'n': return readLiteral("null", null);
            default: return readNumber();
        }
    }

    /** Depth is counted here so both containers share one bound and one release. */
    private Object readNested(boolean object) throws IOException {
        if(depth >= MAX_DEPTH) {
            throw new IOException("JSON nested deeper than " + MAX_DEPTH);
        }
        depth++;
        try {
            return object ? (Object)readObject() : (Object)readArray();
        } finally {
            depth--;
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
                // RFC 8259: everything below U+0020 has to arrive escaped. Taking
                // it literally accepted documents that a conforming parser -- or
                // whatever validates upstream of this one -- rejects, which is
                // how the two disagree about where a string ends.
                if(c < 0x20) {
                    throw new IOException("A control character must be escaped in a "
                            + "JSON string, at offset " + (pos - 1));
                }
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

    /**
     * A JSON number, by the grammar rather than by what Java happens to parse.
     *
     * Scanning a run of "-+0-9.eE" and handing it to Long.parseLong accepted "+1",
     * "01", ".5" and "1." -- none of which is a JSON number, and all of which a
     * conforming client or an upstream validator rejects. A parser that takes
     * documents its own clients cannot produce is worse than a strict one.
     *
     * RFC 8259: [ '-' ] ( '0' | [1-9][0-9]* ) [ '.' [0-9]+ ] [ ('e'|'E') [+-] [0-9]+ ]
     */
    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private Object readNumber() throws IOException {
        int start = pos;
        boolean floating = false;
        if(pos < src.length() && src.charAt(pos) == '-') {
            pos++;                          // '+' is not a JSON sign
        }
        int intStart = pos;
        if(pos < src.length() && src.charAt(pos) == '0') {
            pos++;
            if(pos < src.length() && isDigit(src.charAt(pos))) {
                throw new IOException("A leading zero is not a JSON number, at offset "
                        + start);
            }
        } else {
            while(pos < src.length() && isDigit(src.charAt(pos))) {
                pos++;
            }
        }
        if(pos == intStart) {
            throw new IOException("Expected a digit at offset " + intStart);
        }
        if(pos < src.length() && src.charAt(pos) == '.') {
            floating = true;
            pos++;
            int fracStart = pos;
            while(pos < src.length() && isDigit(src.charAt(pos))) {
                pos++;
            }
            if(pos == fracStart) {
                throw new IOException("Expected a digit after '.' at offset " + fracStart);
            }
        }
        if(pos < src.length() && (src.charAt(pos) == 'e' || src.charAt(pos) == 'E')) {
            floating = true;
            pos++;
            if(pos < src.length() && (src.charAt(pos) == '-' || src.charAt(pos) == '+')) {
                pos++;
            }
            int expStart = pos;
            while(pos < src.length() && isDigit(src.charAt(pos))) {
                pos++;
            }
            if(pos == expStart) {
                throw new IOException("Expected a digit in the exponent at offset "
                        + expStart);
            }
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
        if(value instanceof Collection) {
            // A Set is a JSON array too. Falling through to the String branch below
            // wrote its toString() as a quoted "[a, b]", which parses as a string and
            // is silently the wrong shape rather than an error. Indexed above because
            // a List answers get(i) without building an iterator.
            out.put('[');
            Iterator it = ((Collection)value).iterator();
            boolean first = true;
            while(it.hasNext()) {
                if(!first) {
                    out.put(',');
                }
                first = false;
                writeValue(out, it.next());
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
        // Before the String branch, and for the same reason the sink writer checks
        // it first: a Writable is a DTO carrying its own generated writer. Without
        // this it reached the quoting branch below and was emitted as the JSON
        // STRING of its toString(), so one handler returned an object over HTTP/1.1
        // and unusable text over HTTP/2 -- the two writers disagreeing about a
        // value's type, exactly as they did over Short and Byte.
        if(value instanceof Writable) {
            ByteSink sink = new ByteSink(256);
            ((Writable)value).writeTo(sink);
            try {
                out.append(new String(sink.bytes(), 0, sink.length(), "UTF-8"));
            } catch (java.io.UnsupportedEncodingException never) {
                // UTF-8 is required of every VM.
                throw new IllegalStateException(never.toString());
            }
            return;
        }
        if(value instanceof String) {
            writeString(out, (String)value);
            return;
        }
        // Short and Byte belong here with the other integral types. The ByteSink
        // writer already treats them as numbers, and leaving them out here sent
        // them to the quoting branch below, so Json.write(Short) produced "1"
        // where the sink produced 1 -- the same value with a different JSON type
        // depending on which writer the caller reached.
        if(value instanceof Boolean || value instanceof Integer || value instanceof Long
                || value instanceof Short || value instanceof Byte) {
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
        if(value instanceof byte[]) {
            // As base64url, which is what the byte-sink writer does. The two have to
            // agree: HTTP/1.1 writes through the sink and HTTP/2 through this one, so
            // a disagreement means a BLOB comes back readable over one protocol and
            // as "[B@1a2b3c" over the other, from the same handler.
            writeString(out, Base64Url.encode((byte[])value));
            return;
        }
        if(value instanceof Collection) {
            // As above: the two writers have to agree on what a Set is.
            out.append('[');
            Iterator it = ((Collection)value).iterator();
            boolean first = true;
            while(it.hasNext()) {
                if(!first) {
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
