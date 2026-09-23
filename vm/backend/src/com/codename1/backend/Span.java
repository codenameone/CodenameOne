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

/**
 * One timed operation inside a distributed trace.
 *
 * <p>The server creates these itself -- one per request, one per outbound
 * {@link Web} call and one per {@link Database} statement -- so an application
 * normally only touches the current one, through {@link Tracing#current()}, to
 * add an attribute. {@link Tracing#inSpan} wraps a block of its own work.
 *
 * <p>When tracing is not installed every method here is a no-op on a shared
 * instance, so code that decorates the current span costs nothing in a server
 * that does not export traces.
 *
 * <p>The kinds and status codes are the numbers the OTLP wire format uses, so a
 * tracer can write them without translating.
 */
public abstract class Span {
    /** An operation inside the process. */
    public static final int KIND_INTERNAL = 1;
    /** Answering a request that arrived from outside. */
    public static final int KIND_SERVER = 2;
    /** A request this process makes to something else. */
    public static final int KIND_CLIENT = 3;

    /**
     * What was current when this span became current, restored when it ends.
     * Owned by {@link Tracing}; a tracer never reads it.
     */
    Span previous;
    /** Whether {@link Tracing} made this span current and must restore on end. */
    boolean entered;

    /** For tracer implementations. */
    protected Span() {
    }

    /** Adds or replaces a string attribute. A null value is ignored. */
    public abstract Span setAttribute(String key, String value);

    /** Adds or replaces an integer attribute. */
    public abstract Span setAttribute(String key, long value);

    /** Adds or replaces a floating point attribute. */
    public abstract Span setAttribute(String key, double value);

    /** Adds or replaces a flag attribute. */
    public abstract Span setAttribute(String key, boolean value);

    /**
     * Records a failure as an "exception" event and marks the span as an error.
     * The message is kept; the stack trace is not, because it can carry values a
     * handler had in scope and it is by far the largest part of a span.
     */
    public abstract Span recordException(Throwable error);

    /** Marks the span as failed, with a short description. */
    public abstract Span setError(String description);

    /** Renames the span. The router calls this once it knows the route template. */
    public abstract Span updateName(String name);

    /** The current name. */
    public abstract String getName();

    /** One of the KIND constants. */
    public abstract int getKind();

    /**
     * Whether this span is being recorded. A span the sampler declined still
     * carries a trace context and propagates it, so the NEXT service agrees not to
     * record either -- but attributes set on it go nowhere, and a caller that
     * would compute an expensive one can ask first.
     */
    public abstract boolean isRecording();

    /**
     * This span as a W3C {@code traceparent} value, which is what an outbound
     * request carries so the service it reaches joins the same trace. Null for
     * the no-op span.
     */
    public abstract String traceparent();

    /** The W3C {@code tracestate} this trace carries, or null for none. */
    public abstract String tracestate();

    /**
     * Drops the span: it ends normally but is never exported. For traffic that is
     * about tracing itself, which would otherwise report on every export.
     */
    public abstract void discard();

    /** Ends the span. Only the first call counts. */
    public abstract void end();
}
