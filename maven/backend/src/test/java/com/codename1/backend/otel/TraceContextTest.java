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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** W3C Trace Context parsing, which is input from the internet. */
class TraceContextTest {
    private static final String VALID =
            "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";

    @Test
    @DisplayName("the specification's own example parses, and formats back to itself")
    void roundTrip() {
        TraceContext parsed = TraceContext.parse(VALID);
        assertNotNull(parsed);
        assertTrue(parsed.sampled());
        assertEquals(VALID, TraceContext.format(parsed.traceHi, parsed.traceLo,
                parsed.spanId, parsed.sampled()));
    }

    @Test
    @DisplayName("an unsampled parent is read as unsampled")
    void unsampled() {
        TraceContext parsed = TraceContext.parse(
                "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-00");
        assertNotNull(parsed);
        assertFalse(parsed.sampled());
    }

    @Test
    @DisplayName("everything the specification says to ignore is ignored")
    void malformedIsRefused() {
        // Upper case hex: the specification requires lower case.
        assertNull(TraceContext.parse("00-4BF92F3577B34DA6A3CE929D0E0E4736-00f067aa0ba902b7-01"));
        // The forbidden version.
        assertNull(TraceContext.parse("ff-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"));
        // All-zero ids.
        assertNull(TraceContext.parse("00-00000000000000000000000000000000-00f067aa0ba902b7-01"));
        assertNull(TraceContext.parse("00-4bf92f3577b34da6a3ce929d0e0e4736-0000000000000000-01"));
        // Version 00 has exactly four fields.
        assertNull(TraceContext.parse(VALID + "-extra"));
        // Truncated, and wrong separators.
        assertNull(TraceContext.parse("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7"));
        assertNull(TraceContext.parse("00_4bf92f3577b34da6a3ce929d0e0e4736_00f067aa0ba902b7_01"));
        assertNull(TraceContext.parse("00-4bf92f3577b34da6a3ce929d0e0e473g-00f067aa0ba902b7-01"));
        assertNull(TraceContext.parse(null));
        assertNull(TraceContext.parse(""));
    }

    @Test
    @DisplayName("a later version is read for the fields this one knows")
    void futureVersion() {
        TraceContext parsed = TraceContext.parse(
                "01-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01-whatever");
        assertNotNull(parsed);
        // ...but only when the flags end where they should.
        assertNull(TraceContext.parse(
                "01-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01x"));
    }

    @Test
    @DisplayName("tracestate passes through when sound and is dropped when not")
    void tracestate() {
        assertEquals("rojo=00f067aa0ba902b7,congo=t61rcWkgMzE",
                TraceContext.vetTracestate(" rojo=00f067aa0ba902b7,congo=t61rcWkgMzE "));
        assertNull(TraceContext.vetTracestate("bad\nvalue"));
        StringBuilder huge = new StringBuilder();
        while(huge.length() <= 512) {
            huge.append("k=v,");
        }
        assertNull(TraceContext.vetTracestate(huge.toString()));
        assertNull(TraceContext.vetTracestate(""));
    }

    @Test
    @DisplayName("a tracestate that is not a valid list is discarded whole")
    void tracestateGrammar() {
        assertNull(TraceContext.vetTracestate("bad"), "a member with no key=value");
        assertNull(TraceContext.vetTracestate("a=1,a=2"), "a key twice");
        assertNull(TraceContext.vetTracestate("Upper=1"), "keys are lower case");
        assertNull(TraceContext.vetTracestate("a=x,b=has=equals"));
        StringBuilder many = new StringBuilder();
        for(int i = 0 ; i < 33 ; i++) {
            many.append(i == 0 ? "" : ",").append("k").append(i).append("=v");
        }
        assertNull(TraceContext.vetTracestate(many.toString()), "33 members");
        assertEquals("tenant@sys=1,k/_-*=v v", TraceContext.vetTracestate("tenant@sys=1,k/_-*=v v"));
        assertEquals("a=1,b=2", TraceContext.vetTracestate(" a=1, ,,b=2, "),
                "empty members are accepted, as the specification requires, and not sent on");
    }

    @Test
    @DisplayName("a sampler that takes no argument ignores one it was given")
    void samplerArgumentOnlyForRatio() throws Exception {
        // A shared template sets OTEL_TRACES_SAMPLER_ARG for whatever sampler it
        // expects; a service that chose always_on must still start.
        assertTrue(Sampler.parse("always_on", "not-a-number").sample(false, false, 1));
        assertFalse(Sampler.parse("parentbased_always_off", "x").sample(false, false, 1));
        assertRefused("parentbased_traceidratio", "x");
    }

    @Test
    @DisplayName("a logged endpoint loses its userinfo and its query")
    void endpointRedaction() {
        assertEquals("https://<redacted>@collector.example/v1/traces?<redacted>",
                BatchExporter.redact("https://user:secret@collector.example/v1/traces?token=t"));
        assertEquals("http://collector.example:4318/v1/traces",
                BatchExporter.redact("http://collector.example:4318/v1/traces"));
        // A fragment carries a token as readily as a query does.
        assertEquals("https://collector.example/v1/traces#<redacted>",
                BatchExporter.redact("https://collector.example/v1/traces#access_token=secret"));
        assertEquals("https://c.example/v1#<redacted>",
                BatchExporter.redact("https://c.example/v1#frag?token=secret"));
    }

    @Test
    @DisplayName("once shutdown begins the exporter takes nothing more")
    void aStoppingExporterRefusesLateWork() throws Exception {
        BatchExporter exporter = new BatchExporter("http://127.0.0.1:9/v1/traces",
                new java.util.ArrayList(), true, new java.util.LinkedHashMap(), 16, 4, 60000, 1024);
        exporter.start();
        exporter.shutdown(0);
        // A span from a request still in flight when its tracer was replaced, and
        // a relayed payload arriving just as late: both are dropped and counted,
        // never handed to a worker that is supposed to be finishing.
        exporter.add(null);
        assertFalse(exporter.addRelayed(new byte[] {1}, "application/json"),
                "a stopping exporter accepted a relayed payload");
        java.util.Map metrics = new java.util.LinkedHashMap();
        exporter.metrics(metrics);
        assertEquals(Long.valueOf(1), metrics.get("spansDropped"));
        assertEquals(Integer.valueOf(0), metrics.get("spansQueued"));
    }

    @Test
    @DisplayName("the traces path goes on the endpoint's path, not after its query")
    void tracesPathBeforeQuery() {
        assertEquals("https://c.example/otlp/v1/traces?api-key=s3cret",
                OtlpTracer.appendTracesPath("https://c.example/otlp?api-key=s3cret"));
        assertEquals("http://localhost:4318/v1/traces",
                OtlpTracer.appendTracesPath("http://localhost:4318/"));
        assertEquals("http://localhost:4318/v1/traces",
                OtlpTracer.appendTracesPath("http://localhost:4318"));
    }

    @Test
    @DisplayName("relay input: whole base64 only, and integers in their range")
    void relayValueValidation() throws Exception {
        assertRelayRefuses("{\"bytesValue\":\"A\"}", "a lone base64 character is no byte");
        assertRelayRefuses("{\"bytesValue\":\"AA=garbage\"}", "data after padding");
        assertRelayRefuses("{\"bytesValue\":\"A=AA\"}", "padding in the middle");
        assertRelayRefuses("{\"intValue\":1e100}", "a number past 64 bits");
        assertRelayRefuses("{\"intValue\":\"9223372036854775808\"}", "one past Long.MAX_VALUE");
        relay("{\"bytesValue\":\"AAE=\"}");
        relay("{\"bytesValue\":\"AAE\"}");
        relay("{\"bytesValue\":\"-_8\"}");
        relay("{\"intValue\":\"-9223372036854775808\"}");
    }

    private static void relay(String anyValue) throws Exception {
        OtlpSchema.protobuf(OtlpSchema.sanitize(com.codename1.backend.Json.parseObject(
                "{\"resourceSpans\":[{\"scopeSpans\":[{\"spans\":[{\"traceId\":\""
                + "4bf92f3577b34da6a3ce929d0e0e4736\",\"spanId\":\"00f067aa0ba902b7\","
                + "\"attributes\":[{\"key\":\"k\",\"value\":" + anyValue + "}]}]}]}]}")));
    }

    private static void assertRelayRefuses(String anyValue, String why) {
        try {
            relay(anyValue);
        } catch (Exception expected) {
            return;
        }
        throw new AssertionError("accepted " + anyValue + ": " + why);
    }

    @Test
    @DisplayName("a partial success is read from either encoding")
    void partialSuccessDecoding() throws Exception {
        long[] rejected = new long[1];
        String[] message = new String[1];
        byte[] proto = io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse
                .newBuilder()
                .setPartialSuccess(io.opentelemetry.proto.collector.trace.v1
                        .ExportTracePartialSuccess.newBuilder()
                        .setRejectedSpans(3).setErrorMessage("too old").build())
                .build().toByteArray();
        OtlpSchema.protobufPartialSuccess(proto, rejected, message);
        assertEquals(3, rejected[0]);
        assertEquals("too old", message[0]);
        rejected[0] = 0;
        message[0] = null;
        OtlpSchema.jsonPartialSuccess(
                "{\"partialSuccess\":{\"rejectedSpans\":\"5\",\"errorMessage\":\"bad\"}}",
                rejected, message);
        assertEquals(5, rejected[0]);
        assertEquals("bad", message[0]);
    }

    @Test
    @DisplayName("the ratio sampler agrees with itself for one trace id")
    void samplerIsDeterministicPerTrace() throws Exception {
        Sampler half = Sampler.parse("traceidratio", "0.5");
        int sampled = 0;
        for(long id = 1 ; id <= 10000 ; id++) {
            long lo = id * 0x9E3779B97F4A7C15L;
            boolean first = half.sample(false, false, lo);
            assertEquals(first, half.sample(false, false, lo));
            if(first) {
                sampled++;
            }
        }
        assertTrue(sampled > 4500 && sampled < 5500, "sampled " + sampled + " of 10000");
    }

    @Test
    @DisplayName("parent-based sampling follows the caller and ignores its own ratio")
    void parentBased() throws Exception {
        Sampler never = Sampler.parse("parentbased_always_off", null);
        assertTrue(never.sample(true, true, 1));
        assertFalse(never.sample(true, false, 1));
        assertFalse(never.sample(false, false, 1));
        Sampler always = Sampler.parse(null, null);
        assertFalse(always.sample(true, false, 1));
        assertTrue(always.sample(false, false, 1));
    }

    @Test
    @DisplayName("an unknown sampler or a ratio out of range is refused, not defaulted")
    void badSamplerConfiguration() {
        assertRefused("jaeger_remote", null);
        assertRefused("traceidratio", "1.5");
        assertRefused("traceidratio", "half");
    }

    private static void assertRefused(String name, String arg) {
        try {
            Sampler.parse(name, arg);
        } catch (java.io.IOException expected) {
            return;
        }
        throw new AssertionError("accepted sampler " + name + " / " + arg);
    }

    @Test
    @DisplayName("an X-Ray trace header becomes the equivalent traceparent")
    void xray() throws Exception {
        java.lang.reflect.Method convert = com.codename1.backend.Tracing.class
                .getDeclaredMethod("fromXRay", String.class);
        convert.setAccessible(true);
        assertEquals("00-5759e988bd862e3fe1be46a994272793-53995c3f42cd8ad8-01",
                convert.invoke(null,
                        "Root=1-5759e988-bd862e3fe1be46a994272793;Parent=53995c3f42cd8ad8;Sampled=1"));
        assertEquals("00-5759e988bd862e3fe1be46a994272793-53995c3f42cd8ad8-00",
                convert.invoke(null,
                        "Root=1-5759e988-bd862e3fe1be46a994272793;Parent=53995c3f42cd8ad8;Sampled=0"));
        assertNull(convert.invoke(null, "Root=1-5759e988-bd862e3fe1be46a994272793"));
        assertNull(convert.invoke(null, (Object)null));
    }

    @Test
    @DisplayName("a trace endpoint must name a host, and a valid port if it names one")
    void endpointAuthority() {
        assertTrue(OtlpTracer.hasHttpAuthority("https://collector.example/v1/traces"));
        assertTrue(OtlpTracer.hasHttpAuthority("http://user:pw@127.0.0.1:4318"));
        assertTrue(OtlpTracer.hasHttpAuthority("http://[::1]:4318/v1/traces"));
        assertTrue(OtlpTracer.hasHttpAuthority("HTTP://c.example:/v1/traces"));
        assertFalse(OtlpTracer.hasHttpAuthority("https://"));
        assertFalse(OtlpTracer.hasHttpAuthority("https:///v1/traces"));
        assertFalse(OtlpTracer.hasHttpAuthority("http://user@:4318"));
        assertFalse(OtlpTracer.hasHttpAuthority("http://c.example:70000"));
        assertFalse(OtlpTracer.hasHttpAuthority("http://c.example:43x8"));
        assertFalse(OtlpTracer.hasHttpAuthority("http://[::1"));
        assertFalse(OtlpTracer.hasHttpAuthority("ftp://c.example"));
        // A host no resolver or libcurl can use, though it is not empty.
        assertFalse(OtlpTracer.hasHttpAuthority("https://collector example/v1/traces"));
        assertFalse(OtlpTracer.hasHttpAuthority("https://collector\\example"));
        assertFalse(OtlpTracer.hasHttpAuthority("https://collector\texample"));
        assertFalse(OtlpTracer.hasHttpAuthority("https://[not-an-address]:4318"));
        assertFalse(OtlpTracer.hasHttpAuthority("https://[1234]"));
        assertFalse(OtlpTracer.hasHttpAuthority("https://host]:4318"));
        assertTrue(OtlpTracer.hasHttpAuthority("https://10.0.0.7:4318"));
        assertTrue(OtlpTracer.hasHttpAuthority("https://[::ffff:10.0.0.7]:4318"));
        assertTrue(OtlpTracer.hasHttpAuthority("https://otel-collector.svc_1.local"));
    }

    @Test
    @DisplayName("the relay ceiling counts UTF-8 bytes, not characters")
    void relayCountsBytes() {
        assertEquals(3, OtlpRelay.utf8Length("abc", 100));
        assertEquals(6, OtlpRelay.utf8Length("\u0800\u0800", 100),
                "a three-byte character counted as one");
        assertEquals(4, OtlpRelay.utf8Length("\ud83d\ude00", 100), "a surrogate pair is 4 bytes");
        assertEquals(3, OtlpRelay.utf8Length("\ud83d", 100), "a lone surrogate encodes as U+FFFD");
        assertEquals(2, OtlpRelay.utf8Length("\u00e9", 100));
        assertTrue(OtlpRelay.utf8Length("\u0800\u0800\u0800\u0800", 5) > 5);
    }

    @Test
    @DisplayName("the relay path is refused unless matchable, and normalized as requests are")
    void relayPath() {
        assertEquals("/otel/v1/traces", OtlpTracer.canonicalPath("/otel/v1/traces"));
        assertEquals("/t%C3%A9l%C3%A9metry", OtlpTracer.canonicalPath("/t%c3%a9l%c3%a9metry"),
                "a kept escape gets upper-case hex, as the server spells it");
        assertEquals("/otel/traces", OtlpTracer.canonicalPath("/otel/%74races"),
                "an escaped unreserved character is decoded, as the server decodes it");
        assertEquals("/a%2Fb", OtlpTracer.canonicalPath("/a%2fb"));
        assertNull(OtlpTracer.canonicalPath("/t\u00e9l\u00e9metry"), "non-ASCII");
        assertNull(OtlpTracer.canonicalPath("/otel?x=1"), "a query never matches");
        assertNull(OtlpTracer.canonicalPath("/otel#f"));
        assertNull(OtlpTracer.canonicalPath("/otel traces"));
        assertNull(OtlpTracer.canonicalPath("/otel%2"), "a truncated escape");
        assertNull(OtlpTracer.canonicalPath("/otel%zz"));
        assertNull(OtlpTracer.canonicalPath("otel"));
        assertNull(OtlpTracer.canonicalPath(""));
    }

    @Test
    @DisplayName("a supplementary character survives percent-decoding beside an escape")
    void percentDecodingKeepsSurrogatePairs() throws Exception {
        java.util.Map out = new java.util.LinkedHashMap();
        OtlpTracer.parsePairs("label=\ud83d\ude00%20ok", out, "test");
        assertEquals("\ud83d\ude00 ok", out.get("label"));
    }

    @Test
    @DisplayName("a parent span id may be empty for a root, never all zeros")
    void zeroParentIsRefused() throws Exception {
        String span = "{\"resourceSpans\":[{\"scopeSpans\":[{\"spans\":[{\"traceId\":"
                + "\"4bf92f3577b34da6a3ce929d0e0e4736\",\"spanId\":\"00f067aa0ba902b7\","
                + "\"parentSpanId\":\"PARENT\",\"name\":\"x\"}]}]}]}";
        OtlpSchema.sanitize((java.util.Map)com.codename1.backend.Json.parse(span.replace("PARENT", "")));
        OtlpSchema.sanitize((java.util.Map)com.codename1.backend.Json.parse(
                span.replace("PARENT", "b7ad6b7169203331")));
        java.io.IOException refused = org.junit.jupiter.api.Assertions.assertThrows(
                java.io.IOException.class, () -> OtlpSchema.sanitize((java.util.Map)
                        com.codename1.backend.Json.parse(span.replace("PARENT", "0000000000000000"))));
        assertTrue(refused.getMessage().contains("parentSpanId"), refused.getMessage());
    }

    @Test
    @DisplayName("a truncated value never ends in half a character")
    void truncationKeepsPairsWhole() {
        StringBuilder text = new StringBuilder();
        for(int i = 0 ; i < OtelSpan.MAX_VALUE_LENGTH - 1 ; i++) {
            text.append('a');
        }
        text.append("\ud83d\ude00tail");
        String bounded = OtelSpan.bound(text.toString());
        assertEquals(OtelSpan.MAX_VALUE_LENGTH - 1, bounded.length());
        assertFalse(Character.isHighSurrogate(bounded.charAt(bounded.length() - 1)));
    }

    @Test
    @DisplayName("the relay's CORS origin is * or one serialized origin")
    void corsOrigin() throws Exception {
        assertEquals("https://app.example.com", cors("https://app.example.com"));
        assertEquals("http://localhost:8080", cors("http://localhost:8080"));
        assertEquals("*", cors("*"));
        assertNull(cors(""));
        for(String bad : new String[] {"https://app.example.com/", "https://app.example.com/app",
                "https://a.example, https://b.example", "app.example.com", "https://u@app.example",
                "https://app.example.com?x=1"}) {
            try {
                cors(bad);
                throw new AssertionError("accepted " + bad);
            } catch (java.io.IOException expected) {
                assertTrue(expected.getMessage().contains(OtlpTracer.RELAY_CORS_ORIGIN));
            }
        }
    }

    private static String cors(String value) throws java.io.IOException {
        java.util.Properties settings = new java.util.Properties();
        settings.setProperty(OtlpTracer.RELAY_CORS_ORIGIN, value);
        return OtlpTracer.corsOrigin(com.codename1.backend.Config.of(settings, "test"));
    }
}
