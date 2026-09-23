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
package com.codename1.backend.otel;

import com.codename1.backend.ByteSink;
import com.codename1.backend.Json;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * The OTLP trace export request, as a table, and the protobuf encoder driven by
 * it.
 *
 * <p>ONE MODEL, TWO ENCODINGS. An export is built as the tree OTLP/JSON
 * describes -- Maps keyed by the lowerCamelCase field names, Lists for repeated
 * fields -- and that tree is either written with {@link Json} or walked against
 * this table to produce the binary protobuf form. The relay uses the same walk to
 * re-encode what a client sent, which is also what validates it: a field this
 * table does not name is dropped rather than forwarded, and a trace id that is
 * not 32 hex digits is refused.
 *
 * <p>Protobuf because some collectors accept nothing else -- Dynatrace's OTLP
 * endpoint is one -- and JSON because every collector accepts it and a person can
 * read it. The field numbers are opentelemetry-proto's
 * {@code opentelemetry/proto/trace/v1/trace.proto} and its common and resource
 * messages; the round trip is checked against the generated protobuf classes in
 * the module's tests.
 */
final class OtlpSchema {
    // Field kinds.
    private static final int STRING = 1;
    /** A hex string in JSON, bytes on the wire. */
    private static final int HEX_BYTES = 2;
    /** Base64 in JSON, bytes on the wire (AnyValue.bytesValue only). */
    private static final int BASE64_BYTES = 3;
    private static final int FIXED64 = 4;
    private static final int FIXED32 = 5;
    /** uint32 counts and enums: a varint. */
    private static final int VARINT = 6;
    private static final int INT64 = 7;
    private static final int BOOL = 8;
    private static final int DOUBLE = 9;
    private static final int MESSAGE = 10;

    /** One field: its JSON name, its number, its kind, and for a message its type. */
    private static final class Field {
        final String name;
        final int number;
        final int kind;
        final boolean repeated;
        Message type;
        /** For HEX_BYTES: the exact length in bytes, or 0 for any. */
        final int bytes;
        /**
         * For HEX_BYTES: whether an all-zero value is refused. OTLP requires trace
         * and span ids to be nonzero; a parent id is simply absent for a root.
         */
        boolean nonZero;

        Field(String name, int number, int kind, boolean repeated, int bytes) {
            this.name = name;
            this.number = number;
            this.kind = kind;
            this.repeated = repeated;
            this.bytes = bytes;
        }
    }

    private static final class Message {
        final Field[] fields;

        Message(Field[] fields) {
            this.fields = fields;
        }

        Field field(String name) {
            for(int iter = 0 ; iter < fields.length ; iter++) {
                if(fields[iter].name.equals(name)) {
                    return fields[iter];
                }
            }
            return null;
        }
    }

    private static Field f(String name, int number, int kind) {
        return new Field(name, number, kind, false, 0);
    }

    private static Field hex(String name, int number, int length) {
        return new Field(name, number, HEX_BYTES, false, length);
    }

    /** A required id: a relay that forwarded a zero one would have answered 200 for a
     * span the collector then rejects, with the app long past being able to retry. */
    private static Field id(String name, int number, int length) {
        Field out = hex(name, number, length);
        out.nonZero = true;
        return out;
    }

    private static Field rep(String name, int number) {
        return new Field(name, number, MESSAGE, true, 0);
    }

    private static final Message ANY_VALUE;
    private static final Message KEY_VALUE;
    private static final Message ARRAY_VALUE;
    private static final Message KEY_VALUE_LIST;
    private static final Message RESOURCE;
    private static final Message SCOPE;
    private static final Message STATUS;
    private static final Message EVENT;
    private static final Message LINK;
    private static final Message SPAN;
    private static final Message SCOPE_SPANS;
    private static final Message RESOURCE_SPANS;
    /** ExportTraceServiceRequest, the body of POST /v1/traces. */
    private static final Message EXPORT;

    static {
        Field arrayValue = f("arrayValue", 5, MESSAGE);
        Field kvlistValue = f("kvlistValue", 6, MESSAGE);
        ANY_VALUE = new Message(new Field[] {
            f("stringValue", 1, STRING),
            f("boolValue", 2, BOOL),
            f("intValue", 3, INT64),
            f("doubleValue", 4, DOUBLE),
            arrayValue,
            kvlistValue,
            f("bytesValue", 7, BASE64_BYTES)
        });
        Field kvValue = f("value", 2, MESSAGE);
        kvValue.type = ANY_VALUE;
        KEY_VALUE = new Message(new Field[] {f("key", 1, STRING), kvValue});
        Field arrayValues = rep("values", 1);
        arrayValues.type = ANY_VALUE;
        ARRAY_VALUE = new Message(new Field[] {arrayValues});
        Field listValues = rep("values", 1);
        listValues.type = KEY_VALUE;
        KEY_VALUE_LIST = new Message(new Field[] {listValues});
        arrayValue.type = ARRAY_VALUE;
        kvlistValue.type = KEY_VALUE_LIST;

        RESOURCE = new Message(new Field[] {
            attributes(1), f("droppedAttributesCount", 2, VARINT)
        });
        SCOPE = new Message(new Field[] {
            f("name", 1, STRING), f("version", 2, STRING), attributes(3),
            f("droppedAttributesCount", 4, VARINT)
        });
        STATUS = new Message(new Field[] {f("message", 2, STRING), f("code", 3, VARINT)});
        EVENT = new Message(new Field[] {
            f("timeUnixNano", 1, FIXED64), f("name", 2, STRING), attributes(3),
            f("droppedAttributesCount", 4, VARINT)
        });
        LINK = new Message(new Field[] {
            id("traceId", 1, 16), id("spanId", 2, 8), f("traceState", 3, STRING),
            attributes(4), f("droppedAttributesCount", 5, VARINT), f("flags", 6, FIXED32)
        });
        Field events = rep("events", 11);
        events.type = EVENT;
        Field links = rep("links", 13);
        links.type = LINK;
        Field status = f("status", 15, MESSAGE);
        status.type = STATUS;
        SPAN = new Message(new Field[] {
            id("traceId", 1, 16), id("spanId", 2, 8), f("traceState", 3, STRING),
            hex("parentSpanId", 4, 8), f("flags", 16, FIXED32), f("name", 5, STRING),
            f("kind", 6, VARINT), f("startTimeUnixNano", 7, FIXED64),
            f("endTimeUnixNano", 8, FIXED64), attributes(9),
            f("droppedAttributesCount", 10, VARINT), events,
            f("droppedEventsCount", 12, VARINT), links,
            f("droppedLinksCount", 14, VARINT), status
        });
        Field scope = f("scope", 1, MESSAGE);
        scope.type = SCOPE;
        Field spans = rep("spans", 2);
        spans.type = SPAN;
        SCOPE_SPANS = new Message(new Field[] {scope, spans, f("schemaUrl", 3, STRING)});
        Field resource = f("resource", 1, MESSAGE);
        resource.type = RESOURCE;
        Field scopeSpans = rep("scopeSpans", 2);
        scopeSpans.type = SCOPE_SPANS;
        RESOURCE_SPANS = new Message(new Field[] {resource, scopeSpans, f("schemaUrl", 3, STRING)});
        Field resourceSpans = rep("resourceSpans", 1);
        resourceSpans.type = RESOURCE_SPANS;
        EXPORT = new Message(new Field[] {resourceSpans});
    }

    private static Field attributes(int number) {
        Field out = rep("attributes", number);
        out.type = KEY_VALUE;
        return out;
    }

    private OtlpSchema() {
    }

    /** The export request as OTLP/JSON. */
    static byte[] json(Map request) {
        ByteSink out = new ByteSink(1024);
        Json.write(request, out);
        return copy(out);
    }

    /**
     * The export request as protobuf.
     *
     * @throws IOException when a value does not fit its field: a trace id of the
     *         wrong length, a count that is not a number. Only the relay can hit
     *         that, since it is the one caller whose tree came from outside.
     */
    static byte[] protobuf(Map request) throws IOException {
        ByteSink out = new ByteSink(1024);
        writeMessage(EXPORT, request, out, 0);
        return copy(out);
    }

    /**
     * Re-builds a tree, keeping only what this table names. The relay forwards the
     * result instead of the client's own tree, so unknown fields -- whatever a
     * client decided to attach -- never reach the collector.
     */
    static Map sanitize(Map request) throws IOException {
        return sanitizeMessage(EXPORT, request, 0);
    }

    /** How many spans a request carries, for the relay's cap. */
    static int countSpans(Map request) {
        int total = 0;
        Object rs = request.get("resourceSpans");
        if(!(rs instanceof List)) {
            return 0;
        }
        List resources = (List)rs;
        for(int r = 0 ; r < resources.size() ; r++) {
            Object resource = resources.get(r);
            if(!(resource instanceof Map)) {
                continue;
            }
            Object ss = ((Map)resource).get("scopeSpans");
            if(!(ss instanceof List)) {
                continue;
            }
            List scopes = (List)ss;
            for(int s = 0 ; s < scopes.size() ; s++) {
                Object scope = scopes.get(s);
                if(scope instanceof Map) {
                    Object spans = ((Map)scope).get("spans");
                    if(spans instanceof List) {
                        total += ((List)spans).size();
                    }
                }
            }
        }
        return total;
    }

    /** AnyValue nests; nothing legitimate nests this deep. */
    private static final int MAX_DEPTH = 16;

    private static Map sanitizeMessage(Message type, Map value, int depth) throws IOException {
        if(depth > MAX_DEPTH) {
            throw new IOException("the export nests deeper than " + MAX_DEPTH + " levels");
        }
        Map out = new java.util.LinkedHashMap();
        for(int iter = 0 ; iter < type.fields.length ; iter++) {
            Field field = type.fields[iter];
            Object v = value.get(field.name);
            if(v == null) {
                if(field.nonZero) {
                    // A span or link with no id at all is as unusable as a zero one.
                    throw new IOException(field.name + " is required");
                }
                continue;
            }
            if(field.kind != MESSAGE) {
                // Checked by encoding it: the same rules, one place.
                checkScalar(field, v);
                out.put(field.name, v);
                continue;
            }
            if(field.repeated) {
                if(!(v instanceof List)) {
                    throw new IOException(field.name + " must be a list");
                }
                List items = (List)v;
                List kept = new java.util.ArrayList(items.size());
                for(int i = 0 ; i < items.size() ; i++) {
                    Object item = items.get(i);
                    if(!(item instanceof Map)) {
                        throw new IOException(field.name + " must hold objects");
                    }
                    kept.add(sanitizeMessage(field.type, (Map)item, depth + 1));
                }
                out.put(field.name, kept);
            } else {
                if(!(v instanceof Map)) {
                    throw new IOException(field.name + " must be an object");
                }
                out.put(field.name, sanitizeMessage(field.type, (Map)v, depth + 1));
            }
        }
        return out;
    }

    private static void checkScalar(Field field, Object value) throws IOException {
        ByteSink scratch = new ByteSink(32);
        writeScalar(field, value, scratch);
    }

    private static void writeMessage(Message type, Map value, ByteSink out, int depth)
            throws IOException {
        if(depth > MAX_DEPTH) {
            throw new IOException("the export nests deeper than " + MAX_DEPTH + " levels");
        }
        for(int iter = 0 ; iter < type.fields.length ; iter++) {
            Field field = type.fields[iter];
            Object v = value.get(field.name);
            if(v == null) {
                continue;
            }
            if(field.kind == MESSAGE) {
                if(field.repeated) {
                    if(!(v instanceof List)) {
                        throw new IOException(field.name + " must be a list");
                    }
                    List items = (List)v;
                    for(int i = 0 ; i < items.size() ; i++) {
                        Object item = items.get(i);
                        if(!(item instanceof Map)) {
                            throw new IOException(field.name + " must hold objects");
                        }
                        writeNested(field, (Map)item, out, depth);
                    }
                } else {
                    if(!(v instanceof Map)) {
                        throw new IOException(field.name + " must be an object");
                    }
                    writeNested(field, (Map)v, out, depth);
                }
                continue;
            }
            writeScalar(field, v, out);
        }
    }

    /** Length-delimited: the child is encoded first so its length is known. */
    private static void writeNested(Field field, Map value, ByteSink out, int depth)
            throws IOException {
        ByteSink child = new ByteSink(64);
        writeMessage(field.type, value, child, depth + 1);
        tag(out, field.number, 2);
        varint(out, child.length());
        out.put(child);
    }

    private static void writeScalar(Field field, Object value, ByteSink out) throws IOException {
        switch(field.kind) {
            case STRING: {
                if(!(value instanceof String)) {
                    throw new IOException(field.name + " must be a string");
                }
                // The VM's native encoder, not ByteSink's loop: on the packaged
                // runtime String.getBytes is the vectorized path.
                byte[] text = ((String)value).getBytes("UTF-8");
                tag(out, field.number, 2);
                varint(out, text.length);
                out.put(text, 0, text.length);
                return;
            }
            case HEX_BYTES: {
                byte[] bytes = fromHex(field, value);
                if(bytes.length == 0) {
                    // proto3 omits an empty bytes field; a root span's
                    // parentSpanId is written as "" in JSON and absent here.
                    return;
                }
                tag(out, field.number, 2);
                varint(out, bytes.length);
                out.put(bytes, 0, bytes.length);
                return;
            }
            case BASE64_BYTES: {
                if(!(value instanceof String)) {
                    throw new IOException(field.name + " must be a base64 string");
                }
                byte[] bytes = fromBase64((String)value);
                tag(out, field.number, 2);
                varint(out, bytes.length);
                out.put(bytes, 0, bytes.length);
                return;
            }
            case FIXED64: {
                long v = number(field, value);
                tag(out, field.number, 1);
                fixed64(out, v);
                return;
            }
            case FIXED32: {
                long v = number(field, value);
                if(v < 0 || v > 0xffffffffL) {
                    throw new IOException(field.name + " does not fit 32 bits");
                }
                tag(out, field.number, 5);
                for(int i = 0 ; i < 4 ; i++) {
                    out.put((int)((v >>> (8 * i)) & 0xff));
                }
                return;
            }
            case VARINT: {
                long v = number(field, value);
                if(v < 0 || v > 0xffffffffL) {
                    throw new IOException(field.name + " does not fit 32 bits");
                }
                tag(out, field.number, 0);
                varint(out, v);
                return;
            }
            case INT64: {
                long v = number(field, value);
                tag(out, field.number, 0);
                varint(out, v);
                return;
            }
            case BOOL: {
                if(!(value instanceof Boolean)) {
                    throw new IOException(field.name + " must be true or false");
                }
                tag(out, field.number, 0);
                out.put(((Boolean)value).booleanValue() ? 1 : 0);
                return;
            }
            case DOUBLE: {
                double d;
                if(value instanceof Number) {
                    d = ((Number)value).doubleValue();
                } else if(value instanceof String) {
                    try {
                        d = Double.parseDouble((String)value);
                    } catch (NumberFormatException err) {
                        throw new IOException(field.name + " must be a number");
                    }
                } else {
                    throw new IOException(field.name + " must be a number");
                }
                tag(out, field.number, 1);
                fixed64(out, Double.doubleToLongBits(d));
                return;
            }
            default:
                throw new IOException("unknown field kind for " + field.name);
        }
    }

    /**
     * A 64-bit number from JSON. The proto3 JSON mapping writes 64-bit integers as
     * STRINGS, because a JavaScript number loses precision past 2^53 and a
     * nanosecond timestamp is well past it; a number is accepted too, as
     * collectors do.
     */
    private static long number(Field field, Object value) throws IOException {
        if(value instanceof Long || value instanceof Integer || value instanceof Short
                || value instanceof Byte) {
            return ((Number)value).longValue();
        }
        if(value instanceof Number) {
            double d = ((Number)value).doubleValue();
            if(d != Math.floor(d) || Double.isInfinite(d) || Double.isNaN(d)) {
                throw new IOException(field.name + " must be a whole number");
            }
            return (long)d;
        }
        if(value instanceof String) {
            String text = (String)value;
            if(text.length() == 0 || text.length() > 20) {
                throw new IOException(field.name + " must be a whole number");
            }
            // Unsigned for the fixed64 timestamps, which are uint64 in the proto:
            // parsed digit by digit with wrap-around, so a value above
            // Long.MAX_VALUE keeps its bit pattern the way protobuf does.
            boolean negative = text.charAt(0) == '-';
            long v = 0;
            for(int iter = negative ? 1 : 0 ; iter < text.length() ; iter++) {
                char c = text.charAt(iter);
                if(c < '0' || c > '9') {
                    throw new IOException(field.name + " must be a whole number");
                }
                v = v * 10 + (c - '0');
            }
            if(negative && text.length() == 1) {
                throw new IOException(field.name + " must be a whole number");
            }
            return negative ? -v : v;
        }
        throw new IOException(field.name + " must be a number");
    }

    private static byte[] fromHex(Field field, Object value) throws IOException {
        if(!(value instanceof String)) {
            throw new IOException(field.name + " must be a hex string");
        }
        String text = (String)value;
        if(text.length() == 0) {
            if(field.nonZero) {
                throw new IOException(field.name + " is required");
            }
            return new byte[0];
        }
        if(field.bytes > 0 && text.length() != field.bytes * 2) {
            throw new IOException(field.name + " must be " + (field.bytes * 2) + " hex digits");
        }
        byte[] out = new byte[text.length() / 2];
        if(out.length * 2 != text.length()) {
            throw new IOException(field.name + " must be an even number of hex digits");
        }
        for(int iter = 0 ; iter < out.length ; iter++) {
            int hi = hexDigit(text.charAt(iter * 2));
            int lo = hexDigit(text.charAt(iter * 2 + 1));
            if(hi < 0 || lo < 0) {
                throw new IOException(field.name + " must be hex digits");
            }
            out[iter] = (byte)((hi << 4) | lo);
        }
        if(field.nonZero) {
            boolean zero = true;
            for(int iter = 0 ; iter < out.length && zero ; iter++) {
                zero = out[iter] == 0;
            }
            if(zero) {
                throw new IOException(field.name + " must not be all zeros");
            }
        }
        return out;
    }

    static int hexDigit(char c) {
        if(c >= '0' && c <= '9') {
            return c - '0';
        }
        if(c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if(c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return -1;
    }

    private static byte[] fromBase64(String text) throws IOException {
        ByteSink out = new ByteSink(text.length());
        int buffer = 0;
        int bits = 0;
        for(int iter = 0 ; iter < text.length() ; iter++) {
            char c = text.charAt(iter);
            int v;
            if(c >= 'A' && c <= 'Z') {
                v = c - 'A';
            } else if(c >= 'a' && c <= 'z') {
                v = c - 'a' + 26;
            } else if(c >= '0' && c <= '9') {
                v = c - '0' + 52;
            } else if(c == '+' || c == '-') {
                v = 62;
            } else if(c == '/' || c == '_') {
                v = 63;
            } else if(c == '=') {
                break;
            } else {
                throw new IOException("bytesValue must be base64");
            }
            buffer = (buffer << 6) | v;
            bits += 6;
            if(bits >= 8) {
                bits -= 8;
                out.put((buffer >> bits) & 0xff);
            }
        }
        return copy(out);
    }

    private static void tag(ByteSink out, int number, int wireType) {
        varint(out, ((long)number << 3) | wireType);
    }

    /** Base-128, low group first. Negative int64 takes the full ten bytes, as protobuf does. */
    static void varint(ByteSink out, long value) {
        while((value & ~0x7fL) != 0) {
            out.put((int)((value & 0x7f) | 0x80));
            value >>>= 7;
        }
        out.put((int)value);
    }

    private static void fixed64(ByteSink out, long value) {
        for(int i = 0 ; i < 8 ; i++) {
            out.put((int)((value >>> (8 * i)) & 0xff));
        }
    }

    private static byte[] copy(ByteSink sink) {
        byte[] out = new byte[sink.length()];
        System.arraycopy(sink.bytes(), 0, out, 0, out.length);
        return out;
    }

    /**
     * ExportTraceServiceResponse's partial_success, as OTLP/JSON writes it:
     * {@code {"partialSuccess":{"rejectedSpans":"3","errorMessage":"..."}}}.
     */
    static void jsonPartialSuccess(String body, long[] rejected, String[] message) throws IOException {
        Object parsed = Json.parse(body);
        if(!(parsed instanceof Map)) {
            return;
        }
        Object partial = ((Map)parsed).get("partialSuccess");
        if(!(partial instanceof Map)) {
            return;
        }
        Object count = ((Map)partial).get("rejectedSpans");
        if(count != null) {
            rejected[0] = number(f("rejectedSpans", 1, INT64), count);
        }
        Object text = ((Map)partial).get("errorMessage");
        if(text instanceof String) {
            message[0] = (String)text;
        }
    }

    /**
     * The same from the binary form: field 1 of the response is the
     * ExportTracePartialSuccess message, whose field 1 is rejected_spans (int64)
     * and field 2 error_message (string). Unknown fields are skipped, as protobuf
     * requires, so a newer collector's response still reads.
     */
    static void protobufPartialSuccess(byte[] body, long[] rejected, String[] message)
            throws IOException {
        int[] at = new int[1];
        while(at[0] < body.length) {
            long key = readVarint(body, at);
            int field = (int)(key >>> 3);
            int wire = (int)(key & 7);
            if(field == 1 && wire == 2) {
                int length = (int)readVarint(body, at);
                int end = at[0] + length;
                if(length < 0 || end > body.length) {
                    throw new IOException("truncated response");
                }
                while(at[0] < end) {
                    long inner = readVarint(body, at);
                    int innerField = (int)(inner >>> 3);
                    int innerWire = (int)(inner & 7);
                    if(innerField == 1 && innerWire == 0) {
                        rejected[0] = readVarint(body, at);
                    } else if(innerField == 2 && innerWire == 2) {
                        int n = (int)readVarint(body, at);
                        if(n < 0 || at[0] + n > end) {
                            throw new IOException("truncated response");
                        }
                        message[0] = new String(body, at[0], n, "UTF-8");
                        at[0] += n;
                    } else {
                        skip(body, at, innerWire);
                    }
                }
            } else {
                skip(body, at, wire);
            }
        }
    }

    private static long readVarint(byte[] data, int[] at) throws IOException {
        long value = 0;
        for(int shift = 0 ; shift < 64 ; shift += 7) {
            if(at[0] >= data.length) {
                throw new IOException("truncated varint");
            }
            int b = data[at[0]++] & 0xff;
            value |= (long)(b & 0x7f) << shift;
            if((b & 0x80) == 0) {
                return value;
            }
        }
        throw new IOException("varint too long");
    }

    private static void skip(byte[] data, int[] at, int wire) throws IOException {
        switch(wire) {
            case 0:
                readVarint(data, at);
                return;
            case 1:
                at[0] += 8;
                break;
            case 2:
                int n = (int)readVarint(data, at);
                if(n < 0) {
                    throw new IOException("negative length");
                }
                at[0] += n;
                break;
            case 5:
                at[0] += 4;
                break;
            default:
                throw new IOException("unsupported wire type " + wire);
        }
        if(at[0] > data.length) {
            throw new IOException("truncated field");
        }
    }
}
