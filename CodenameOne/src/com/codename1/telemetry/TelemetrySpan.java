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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// One timed operation in a distributed trace.
///
/// Network requests become spans by themselves once [Telemetry] is installed; an
/// app creates its own with [Telemetry#startSpan(String)] or
/// [Telemetry#run(String, Runnable)] to time something the user did -- a tap, a
/// screen load -- so the requests it caused are grouped under it.
///
/// A span the sampler declined is not recorded, but it still carries its trace
/// context, and propagates the decision: the backend it calls agrees not to record
/// either, so a trace is whole or absent rather than missing its middle.
///
/// Not thread safe. A span belongs to the thread doing the work it times, and is
/// handed to the exporter only when it ends.
public final class TelemetrySpan {
    /// An operation inside the app.
    public static final int KIND_INTERNAL = 1;
    /// A request the app makes to something else.
    public static final int KIND_CLIENT = 3;

    /// OpenTelemetry's default limits. Past them an attribute or event is counted,
    /// not stored.
    static final int MAX_ATTRIBUTES = 128;
    static final int MAX_EVENTS = 32;
    static final int MAX_VALUE_LENGTH = 4096;

    final String traceId;
    final String spanId;
    final String parentSpanId;
    final boolean sampled;
    final int kind;
    String name;
    final long startEpochNanos;
    long endEpochNanos;
    /// The clock this span reads: an epoch time and a monotonic reading taken at the
    /// same moment, inherited from a local parent so every span of one trace sits on
    /// one timeline. A device's wall clock is corrected while the app runs --
    /// network time, the user, a time zone change -- and a child that read it for
    /// itself could start before its parent, or long after the parent had ended.
    final long anchorEpochNanos;
    final long anchorNano;
    final Map<String, Object> attributes;
    int droppedAttributes;
    /// Each an Object[] {Long time, String name, Map attributes}.
    final List<Object[]> events;
    int droppedEvents;
    /// 0 unset, 2 error: OTLP's own status codes.
    int statusCode;
    String statusMessage;
    private boolean ended;
    private final Telemetry.State owner;

    TelemetrySpan(Telemetry.State owner, String name, int kind, String traceId, String spanId,
                  String parentSpanId, boolean sampled) {
        this(owner, name, kind, traceId, spanId, parentSpanId, sampled, null);
    }

    TelemetrySpan(Telemetry.State owner, String name, int kind, String traceId, String spanId,
                  String parentSpanId, boolean sampled, TelemetrySpan localParent) {
        this.owner = owner;
        this.name = name == null ? "" : name;
        this.kind = kind;
        this.traceId = traceId;
        this.spanId = spanId;
        this.parentSpanId = parentSpanId;
        this.sampled = sampled;
        if (localParent != null) {
            this.anchorEpochNanos = localParent.anchorEpochNanos;
            this.anchorNano = localParent.anchorNano;
        } else {
            this.anchorEpochNanos = System.currentTimeMillis() * 1000000L;
            this.anchorNano = System.nanoTime();
        }
        this.startEpochNanos = now();
        this.attributes = sampled ? new LinkedHashMap<String, Object>() : null;
        this.events = sampled ? new ArrayList<Object[]>() : null;
    }

    /// Adds or replaces a string attribute. Null values are ignored.
    ///
    /// #### Returns
    ///
    /// this span
    public TelemetrySpan setAttribute(String key, String value) {
        if (value != null && value.length() > MAX_VALUE_LENGTH) {
            return put(key, bound(value));
        }
        return put(key, value);
    }

    /// Adds or replaces an integer attribute.
    ///
    /// #### Returns
    ///
    /// this span
    public TelemetrySpan setAttribute(String key, long value) {
        return put(key, Long.valueOf(value));
    }

    /// Adds or replaces a floating point attribute. NaN and the infinities, which
    /// JSON cannot spell, are recorded as text.
    ///
    /// #### Returns
    ///
    /// this span
    public TelemetrySpan setAttribute(String key, double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return put(key, String.valueOf(value));
        }
        return put(key, Double.valueOf(value));
    }

    /// Adds or replaces a flag attribute.
    ///
    /// #### Returns
    ///
    /// this span
    public TelemetrySpan setAttribute(String key, boolean value) {
        return put(key, value ? Boolean.TRUE : Boolean.FALSE);
    }

    private TelemetrySpan put(String key, Object value) {
        if (!sampled || ended || key == null || value == null) {
            return this;
        }
        if (!attributes.containsKey(key) && attributes.size() >= MAX_ATTRIBUTES) {
            droppedAttributes++;
            return this;
        }
        attributes.put(key, value);
        return this;
    }

    /// Records a failure as an "exception" event and marks the span failed. The
    /// message is kept; the stack trace is not.
    ///
    /// #### Returns
    ///
    /// this span
    public TelemetrySpan recordException(Throwable error) {
        if (!sampled || ended || error == null) {
            return this;
        }
        String type = error.getClass().getName();
        String message = error.getMessage();
        statusCode = 2;
        // Bounded like the event attribute below: an exception message can be any
        // size, and the export queue is bounded by span count, not bytes.
        statusMessage = bound(message == null ? type : message);
        if (events.size() >= MAX_EVENTS) {
            droppedEvents++;
            return this;
        }
        Map<String, Object> attrs = new LinkedHashMap<String, Object>();
        attrs.put("exception.type", type);
        if (message != null) {
            attrs.put("exception.message", message.length() > MAX_VALUE_LENGTH
                    ? bound(message) : message);
        }
        Object[] event = {Long.valueOf(now()), "exception", attrs};
        events.add(event);
        return this;
    }

    /// Marks the span failed, with a short description.
    ///
    /// #### Returns
    ///
    /// this span
    public TelemetrySpan setError(String description) {
        if (sampled && !ended) {
            statusCode = 2;
            statusMessage = description == null ? null : bound(description);
        }
        return this;
    }

    /// Renames the span.
    ///
    /// #### Returns
    ///
    /// this span
    public TelemetrySpan updateName(String newName) {
        if (!ended && newName != null) {
            name = newName;
        }
        return this;
    }

    /// The span's name.
    public String getName() {
        return name;
    }

    /// Whether this span is recorded. Attributes set on one that is not go nowhere,
    /// so an attribute that is expensive to compute can be skipped.
    public boolean isRecording() {
        return sampled && !ended;
    }

    /// The 32 hex digit trace id.
    public String getTraceId() {
        return traceId;
    }

    /// The 16 hex digit span id.
    public String getSpanId() {
        return spanId;
    }

    /// This span as a W3C `traceparent` header value, for a transport the
    /// framework does not instrument itself -- a WebSocket message, a push token
    /// registration.
    ///
    /// Null for the span [Telemetry#startSpan(String)] hands out when there is
    /// no trace to join: telemetry is not installed, consent is required and not
    /// given, or the platform cannot make ids. Its ids are all zeros, which W3C
    /// Trace Context defines as invalid, so a header built from them would be
    /// refused or misread downstream. An unsampled span is different: it is a real
    /// trace whose decision must travel, and it answers with its `-00` flags.
    ///
    /// #### Returns
    ///
    /// the header value, or null when there is no trace
    public String getTraceparent() {
        if (owner == null) {
            return null;
        }
        return "00-" + traceId + "-" + spanId + (sampled ? "-01" : "-00");
    }

    /// Whether `state` recorded this span.
    boolean isOwnedBy(Telemetry.State state) {
        return owner == state; //NOPMD CompareObjectsWithEquals
    }

    /// Ends the span. Only the first call counts.
    public void end() {
        if (ended) {
            return;
        }
        ended = true;
        endEpochNanos = now();
        if (sampled && owner != null) {
            owner.ended(this);
        }
    }

    /// Epoch nanoseconds on this trace's clock: the anchor plus the monotonic time
    /// since it. Never earlier than the anchor, whatever the platform's clock does.
    private long now() {
        long elapsed = System.nanoTime() - anchorNano;
        return anchorEpochNanos + (elapsed < 0 ? 0 : elapsed);
    }

    /// At most MAX_VALUE_LENGTH chars, cut on a code point boundary. A cut through
    /// a surrogate pair kept a lone high surrogate, which UTF-8 encoding then
    /// replaced, so the exported value was not the one the app recorded.
    static String bound(String value) {
        if (value.length() <= MAX_VALUE_LENGTH) {
            return value;
        }
        int end = MAX_VALUE_LENGTH;
        if (Character.isHighSurrogate(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(0, end);
    }
}
