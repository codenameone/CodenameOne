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
package com.codename1.telemetry;

import com.codename1.util.StringUtil;

import java.util.List;
import java.util.Map;

/// OTLP's `ExportTraceServiceRequest`, written from spans in either of its two
/// encodings.
///
/// Hand written because a protobuf runtime is far larger than the dozen fields a
/// trace export uses, and an app that turns telemetry on should not grow by a
/// library to do it. The field numbers are opentelemetry-proto's `trace.proto`,
/// `common.proto` and `resource.proto`; the tests decode both encodings with the
/// classes generated from those files.
final class OtlpEncoding {
    static final String SCOPE_NAME = "com.codename1.telemetry";

    private OtlpEncoding() {
    }

    // ------------------------------------------------------------------
    // JSON
    // ------------------------------------------------------------------

    static byte[] json(Map<String, Object> resource, List<TelemetrySpan> spans) {
        StringBuilder b = new StringBuilder(256 + spans.size() * 256);
        b.append("{\"resourceSpans\":[{\"resource\":{\"attributes\":");
        jsonAttributes(b, resource);
        b.append("},\"scopeSpans\":[{\"scope\":{\"name\":\"").append(SCOPE_NAME)
                .append("\"},\"spans\":[");
        boolean first = true;
        for (TelemetrySpan span : spans) {
            if (!first) {
                b.append(',');
            }
            first = false;
            jsonSpan(b, span);
        }
        b.append("]}]}]}");
        return utf8(b.toString());
    }

    private static void jsonSpan(StringBuilder b, TelemetrySpan span) {
        b.append("{\"traceId\":\"").append(span.traceId)
                .append("\",\"spanId\":\"").append(span.spanId).append('"');
        if (span.parentSpanId != null) {
            b.append(",\"parentSpanId\":\"").append(span.parentSpanId).append('"');
        }
        b.append(",\"flags\":").append(flags(span));
        b.append(",\"name\":");
        jsonString(b, span.name);
        b.append(",\"kind\":").append(span.kind);
        // Strings: proto3's JSON mapping writes 64-bit integers that way, since
        // a nanosecond timestamp is past what a JavaScript number holds exactly.
        b.append(",\"startTimeUnixNano\":\"").append(span.startEpochNanos).append('"');
        b.append(",\"endTimeUnixNano\":\"").append(span.endEpochNanos).append('"');
        b.append(",\"attributes\":");
        jsonAttributes(b, span.attributes);
        if (span.droppedAttributes > 0) {
            b.append(",\"droppedAttributesCount\":").append(span.droppedAttributes);
        }
        if (span.events != null && !span.events.isEmpty()) {
            b.append(",\"events\":[");
            boolean first = true;
            for (Object[] event : span.events) {
                if (!first) {
                    b.append(',');
                }
                first = false;
                b.append("{\"timeUnixNano\":\"").append(event[0]).append("\",\"name\":");
                jsonString(b, String.valueOf(event[1]));
                b.append(",\"attributes\":");
                jsonAttributes(b, asMap(event[2]));
                b.append('}');
            }
            b.append(']');
        }
        if (span.droppedEvents > 0) {
            b.append(",\"droppedEventsCount\":").append(span.droppedEvents);
        }
        if (span.statusCode != 0) {
            b.append(",\"status\":{");
            if (span.statusMessage != null) {
                b.append("\"message\":");
                jsonString(b, span.statusMessage);
                b.append(',');
            }
            b.append("\"code\":").append(span.statusCode).append('}');
        }
        b.append('}');
    }

    private static void jsonAttributes(StringBuilder b, Map<String, Object> attributes) {
        b.append('[');
        if (attributes != null) {
            boolean first = true;
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                if (!first) {
                    b.append(',');
                }
                first = false;
                b.append("{\"key\":");
                jsonString(b, entry.getKey());
                b.append(",\"value\":{");
                Object v = entry.getValue();
                if (v instanceof Boolean) {
                    b.append("\"boolValue\":").append(v);
                } else if (v instanceof Long || v instanceof Integer) {
                    b.append("\"intValue\":\"").append(v).append('"');
                } else if (v instanceof Double) {
                    b.append("\"doubleValue\":").append(v);
                } else {
                    b.append("\"stringValue\":");
                    jsonString(b, String.valueOf(v));
                }
                b.append("}}");
            }
        }
        b.append(']');
    }

    private static void jsonString(StringBuilder b, String value) {
        b.append('"');
        int n = value.length();
        for (int i = 0; i < n; i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    b.append("\\\"");
                    break;
                case '\\':
                    b.append("\\\\");
                    break;
                case '\n':
                    b.append("\\n");
                    break;
                case '\r':
                    b.append("\\r");
                    break;
                case '\t':
                    b.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        String hex = Integer.toHexString(c);
                        b.append("\\u");
                        for (int pad = hex.length(); pad < 4; pad++) {
                            b.append('0');
                        }
                        b.append(hex);
                    } else {
                        b.append(c);
                    }
                    break;
            }
        }
        b.append('"');
    }

    // ------------------------------------------------------------------
    // Protobuf
    // ------------------------------------------------------------------

    static byte[] protobuf(Map<String, Object> resource, List<TelemetrySpan> spans) {
        Sink scope = new Sink();
        Sink scopeName = new Sink();
        scopeName.string(1, SCOPE_NAME);
        scope.message(1, scopeName);                       // ScopeSpans.scope
        for (TelemetrySpan span : spans) {
            scope.message(2, protoSpan(span));             // ScopeSpans.spans
        }
        Sink resourceMessage = new Sink();
        protoAttributes(resourceMessage, 1, resource);     // Resource.attributes
        Sink resourceSpans = new Sink();
        resourceSpans.message(1, resourceMessage);         // ResourceSpans.resource
        resourceSpans.message(2, scope);                   // ResourceSpans.scope_spans
        Sink request = new Sink();
        request.message(1, resourceSpans);                 // ExportTraceServiceRequest.resource_spans
        return request.toByteArray();
    }

    private static Sink protoSpan(TelemetrySpan span) {
        Sink out = new Sink();
        out.bytes(1, hex(span.traceId));
        out.bytes(2, hex(span.spanId));
        if (span.parentSpanId != null) {
            out.bytes(4, hex(span.parentSpanId));
        }
        out.string(5, span.name);
        out.varint(6, span.kind);
        out.fixed64(7, span.startEpochNanos);
        out.fixed64(8, span.endEpochNanos);
        protoAttributes(out, 9, span.attributes);
        if (span.droppedAttributes > 0) {
            out.varint(10, span.droppedAttributes);
        }
        if (span.events != null) {
            for (Object[] event : span.events) {
                Sink e = new Sink();
                e.fixed64(1, ((Long) event[0]).longValue());
                e.string(2, String.valueOf(event[1]));
                protoAttributes(e, 3, asMap(event[2]));
                out.message(11, e);
            }
        }
        if (span.droppedEvents > 0) {
            out.varint(12, span.droppedEvents);
        }
        if (span.statusCode != 0) {
            Sink status = new Sink();
            if (span.statusMessage != null) {
                status.string(2, span.statusMessage);
            }
            status.varint(3, span.statusCode);
            out.message(15, status);
        }
        out.fixed32(16, flags(span));
        return out;
    }

    private static void protoAttributes(Sink out, int field, Map<String, Object> attributes) {
        if (attributes == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            Sink value = new Sink();
            Object v = entry.getValue();
            if (v instanceof Boolean) {
                value.varint(2, ((Boolean) v).booleanValue() ? 1 : 0);
            } else if (v instanceof Long || v instanceof Integer) {
                value.varint(3, ((Number) v).longValue());
            } else if (v instanceof Double) {
                value.fixed64(4, Double.doubleToLongBits(((Double) v).doubleValue()));
            } else {
                value.string(1, String.valueOf(v));
            }
            Sink kv = new Sink();
            kv.string(1, entry.getKey());
            kv.message(2, value);
            out.message(field, kv);
        }
    }

    /// Trace flags in the low byte, then HAS_IS_REMOTE: every span the app makes
    /// has a local parent or none.
    private static int flags(TelemetrySpan span) {
        return (span.sampled ? 1 : 0) | 0x100;
    }

    private static Map<String, Object> asMap(Object value) {
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            return map;
        }
        return null;
    }

    private static byte[] hex(String text) {
        byte[] out = new byte[text.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) ((digit(text.charAt(i * 2)) << 4) | digit(text.charAt(i * 2 + 1)));
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
        return c - 'A' + 10;
    }

    /// UTF-8 through `String.getBytes`, which on ParparVM is the VM's native
    /// vectorized encoder rather than a loop in Java.
    static byte[] utf8(String value) {
        return StringUtil.getBytes(value);
    }

    /// A protobuf message being written. Nested messages are written into a sink
    /// of their own first, because a length-delimited field needs its length
    /// before its bytes.
    static final class Sink {
        private byte[] data = new byte[64];
        private int length;

        private void ensure(int extra) {
            if (length + extra > data.length) {
                int size = data.length * 2;
                while (size < length + extra) {
                    size *= 2;
                }
                byte[] grown = new byte[size];
                System.arraycopy(data, 0, grown, 0, length);
                data = grown;
            }
        }

        void tag(int field, int wireType) {
            rawVarint(((long) field << 3) | wireType);
        }

        void rawVarint(long value) {
            long v = value;
            ensure(10);
            while ((v & ~0x7fL) != 0) {
                data[length++] = (byte) ((v & 0x7f) | 0x80);
                v >>>= 7;
            }
            data[length++] = (byte) v;
        }

        void varint(int field, long value) {
            tag(field, 0);
            rawVarint(value);
        }

        void fixed64(int field, long value) {
            tag(field, 1);
            ensure(8);
            for (int i = 0; i < 8; i++) {
                data[length++] = (byte) (value >> (8 * i));
            }
        }

        void fixed32(int field, int value) {
            tag(field, 5);
            ensure(4);
            for (int i = 0; i < 4; i++) {
                data[length++] = (byte) (value >> (8 * i));
            }
        }

        void bytes(int field, byte[] value) {
            bytes(field, value, value.length);
        }

        private void bytes(int field, byte[] value, int count) {
            tag(field, 2);
            rawVarint(count);
            ensure(count);
            System.arraycopy(value, 0, data, length, count);
            length += count;
        }

        void string(int field, String value) {
            bytes(field, utf8(value));
        }

        /// A nested message, copied straight out of the child's buffer: its
        /// length has to precede it, so it is written once the child is complete.
        void message(int field, Sink child) {
            bytes(field, child.data, child.length);
        }

        byte[] toByteArray() {
            byte[] out = new byte[length];
            System.arraycopy(data, 0, out, 0, length);
            return out;
        }
    }
}
