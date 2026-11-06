/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package dart.core;

/// Dart's {@code StackTrace}. A trace is either text ({@link #fromString}) or the Java
/// frames of a throwable, rendered lazily one Dart-style {@code #n} line per frame.
/// An async error's trace travels with its error: {@link #attach} records the trace
/// given to {@code completeError}/{@code addError}, and {@link #ofThrown} answers it
/// to the handler or catch clause that receives the error.
public class StackTrace {
    /// Dart's {@code StackTrace.empty}.
    public static final StackTrace empty = new StackTrace("");

    /// Traces given with an error, keyed by the Throwable that carries the error.
    /// Weak, so an error nobody holds takes its trace with it.
    private static final java.util.WeakHashMap<Throwable, StackTrace> ATTACHED =
            new java.util.WeakHashMap<Throwable, StackTrace>();

    private String text;
    private Throwable frames;

    public StackTrace(String text) {
        this.text = text == null ? "" : text;
    }

    private StackTrace(Throwable frames) {
        this.frames = frames;
    }

    /// Dart's {@code StackTrace.current}: the caller's frames. A getter in Dart; the
    /// transpiler emits it as this call.
    public static StackTrace current() {
        return new StackTrace(new Throwable());
    }

    /// Dart's {@code StackTrace.fromString(stackTraceString)}.
    public static StackTrace fromString(String stackTraceString) {
        return new StackTrace(stackTraceString);
    }

    /// The trace for a caught value: one passed along with it, otherwise {@link #empty}.
    public static StackTrace of(Object trace) {
        return trace instanceof StackTrace ? (StackTrace) trace : empty;
    }

    /// Records {@code trace} as the trace of the error {@code t} carries. A null or
    /// non-StackTrace trace records nothing, so the throwable's own frames apply.
    public static void attach(Throwable t, Object trace) {
        if (t != null && trace instanceof StackTrace) {
            ATTACHED.put(t, (StackTrace) trace);
        }
    }

    /// The Throwable an async error travels as, with {@code trace} attached to it.
    public static Throwable carry(Object error, Object trace) {
        Throwable t = error instanceof Throwable ? (Throwable) error : dart.runtime.DartRuntime.asError(error);
        attach(t, trace);
        return t;
    }

    /// The trace an error handler receives for {@code error} as delivered.
    public static StackTrace forError(Object error) {
        return error instanceof Throwable ? ofThrown((Throwable) error) : empty;
    }

    /// What {@code catch (e, st)} and an error handler bind for {@code t}: the trace
    /// attached to it, else its own Java frames.
    public static StackTrace ofThrown(Throwable t) {
        if (t == null) {
            return empty;
        }
        StackTrace st = ATTACHED.get(t);
        return st != null ? st : new StackTrace(t);
    }

    @Override
    public String toString() {
        if (text == null) {
            StringBuilder sb = new StringBuilder();
            StackTraceElement[] els = frames.getStackTrace();
            int n = 0;
            if (els != null) {
                for (StackTraceElement e : els) {
                    sb.append('#').append(n++).append("      ").append(e.getClassName()).append('.')
                            .append(e.getMethodName()).append(" (").append(e.getFileName()).append(':')
                            .append(e.getLineNumber()).append(")\n");
                }
            }
            if (n == 0) {
                // A VM that records no frames still answers a trace, never an empty one.
                sb.append("#0      <asynchronous suspension>\n");
            }
            text = sb.toString();
            frames = null;
        }
        return text;
    }
}
