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

import com.codename1.backend.Span;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A span as {@link OtlpTracer} records it.
 *
 * <p>Written by one thread -- the one doing the work it times -- and handed to
 * the exporter only by {@link #end}, through a synchronized queue, so nothing
 * here needs a lock of its own.
 */
final class OtelSpan extends Span {
    /** OpenTelemetry's default attribute and event limits. */
    static final int MAX_ATTRIBUTES = 128;
    static final int MAX_EVENTS = 128;
    /** Past this a string attribute is truncated. A request body in an attribute is a bug. */
    static final int MAX_VALUE_LENGTH = 4096;

    private final OtlpTracer tracer;
    final long traceHi;
    final long traceLo;
    final long spanId;
    /** 0 for a root span. */
    final long parentId;
    final boolean parentRemote;
    final boolean sampled;
    final String tracestate;
    final int kind;
    String name;
    final long startEpochNanos;
    /**
     * The clock this span reads: an epoch time and the monotonic reading taken at
     * the same moment, shared by every span of one trace in this process.
     */
    final long anchorEpochNanos;
    final long anchorNano;
    long endEpochNanos;
    /** Key to value (String, Long, Double or Boolean), in the order first set. */
    final Map attributes;
    int droppedAttributes;
    /** Each an Object[] {Long time, String name, Map attributes}. */
    final List events;
    int droppedEvents;
    /** 0 unset, 2 error: OTLP's StatusCode. OK is never set by instrumentation. */
    int statusCode;
    String statusMessage;
    private boolean ended;
    private boolean discarded;

    OtelSpan(OtlpTracer tracer, String name, int kind, long traceHi, long traceLo, long spanId,
             long parentId, boolean parentRemote, boolean sampled, String tracestate,
             OtelSpan localParent) {
        this.tracer = tracer;
        this.name = name == null ? "" : name;
        this.kind = kind;
        this.traceHi = traceHi;
        this.traceLo = traceLo;
        this.spanId = spanId;
        this.parentId = parentId;
        this.parentRemote = parentRemote;
        this.sampled = sampled;
        this.tracestate = tracestate;
        // ONE CLOCK PER TRACE. A root span anchors the wall clock to the monotonic
        // one, and its descendants inherit that anchor and measure from it. Each
        // span reading the wall clock for itself put spans a millisecond apart
        // (the wall clock's resolution) on timelines that disagreed, and a child
        // was reported ending after the parent that contains it. The monotonic
        // reading also keeps a clock step from producing a span that ends before
        // it starts.
        if(localParent != null) {
            this.anchorEpochNanos = localParent.anchorEpochNanos;
            this.anchorNano = localParent.anchorNano;
        } else {
            this.anchorEpochNanos = System.currentTimeMillis() * 1000000L;
            this.anchorNano = System.nanoTime();
        }
        this.startEpochNanos = nowEpochNanos();
        this.attributes = sampled ? new LinkedHashMap() : null;
        this.events = sampled ? new ArrayList(0) : null;
    }

    public Span setAttribute(String key, String value) {
        if(value != null && value.length() > MAX_VALUE_LENGTH) {
            value = value.substring(0, MAX_VALUE_LENGTH);
        }
        return put(key, value);
    }

    public Span setAttribute(String key, long value) {
        return put(key, Long.valueOf(value));
    }

    public Span setAttribute(String key, double value) {
        if(Double.isNaN(value) || Double.isInfinite(value)) {
            // JSON has no spelling for either, so the whole export would be
            // refused by a collector over one attribute. Kept as text instead.
            return put(key, String.valueOf(value));
        }
        return put(key, Double.valueOf(value));
    }

    public Span setAttribute(String key, boolean value) {
        return put(key, value ? Boolean.TRUE : Boolean.FALSE);
    }

    private Span put(String key, Object value) {
        if(!sampled || ended || key == null || value == null || tracer.excluded(key)) {
            return this;
        }
        if(!attributes.containsKey(key) && attributes.size() >= MAX_ATTRIBUTES) {
            droppedAttributes++;
            return this;
        }
        attributes.put(key, value);
        return this;
    }

    public Span recordException(Throwable error) {
        if(!sampled || ended || error == null) {
            return this;
        }
        String type = error.getClass().getName();
        String message = error.getMessage();
        statusCode = 2;
        statusMessage = message == null ? type : message;
        if(events.size() >= MAX_EVENTS) {
            droppedEvents++;
            return this;
        }
        Map attrs = new LinkedHashMap();
        attrs.put("exception.type", type);
        if(message != null) {
            attrs.put("exception.message", message.length() > MAX_VALUE_LENGTH
                    ? message.substring(0, MAX_VALUE_LENGTH) : message);
        }
        events.add(new Object[] {Long.valueOf(nowEpochNanos()), "exception", attrs});
        return this;
    }

    public Span setError(String description) {
        if(!sampled || ended) {
            return this;
        }
        statusCode = 2;
        statusMessage = description;
        return this;
    }

    public Span updateName(String newName) {
        if(!ended && newName != null) {
            name = newName;
        }
        return this;
    }

    public String getName() {
        return name;
    }

    public int getKind() {
        return kind;
    }

    public boolean isRecording() {
        return sampled && !ended && !discarded;
    }

    public String traceparent() {
        return TraceContext.format(traceHi, traceLo, spanId, sampled);
    }

    public String tracestate() {
        return tracestate;
    }

    public void discard() {
        discarded = true;
    }

    public void end() {
        if(ended) {
            return;
        }
        ended = true;
        endEpochNanos = nowEpochNanos();
        if(sampled && !discarded) {
            tracer.ended(this);
        }
    }

    private long nowEpochNanos() {
        long elapsed = System.nanoTime() - anchorNano;
        return anchorEpochNanos + (elapsed < 0 ? 0 : elapsed);
    }
}
