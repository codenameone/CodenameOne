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
}
