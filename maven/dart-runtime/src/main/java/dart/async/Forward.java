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
package dart.async;

import dart.runtime.Funcs;

/// A stream derived from another by listening to it: Dart's forwarding streams
/// ({@code take}, {@code skip}, {@code expand}, {@code asyncMap}...) and the
/// chunked converters ({@code utf8.decoder}, {@code LineSplitter}).
///
/// Each listen of the derived stream subscribes to the source once and makes one
/// {@code Forward} through the factory, whose {@link #data}, {@link #error} and
/// {@link #done} see the source's events as they arrive and pass on what they
/// produce, synchronously, as Dart's forwarding subscriptions do. Pausing,
/// resuming and cancelling the derived subscription pause, resume and cancel
/// the source. Nothing here blocks or starts a thread.
///
/// What {@link #data} throws is an error event, after which the derived stream
/// ends and the source is cancelled; a transform that should go on after a
/// failure catches it and calls {@link #addError} itself.
public abstract class Forward<T, R> {
    private StreamController<R> out;
    private StreamSubscription<T> sub;
    private boolean ended;
    private boolean cancelWanted;
    private int sourcePauses;

    /** One source event. */
    protected abstract void data(T value);

    /** One source error; forwarded unchanged unless overridden. */
    protected void error(Object e) {
        addError(e);
    }

    /** The source is done; the derived stream closes unless overridden. */
    protected void done() {
        close();
    }

    protected final void add(R value) {
        if (!ended) {
            out.add(value);
        }
    }

    protected final void addError(Object e) {
        if (!ended) {
            out.addError(e instanceof Throwable ? dart.runtime.DartRuntime.caught((Throwable) e) : e);
        }
    }

    /** Closes the derived stream (the source has ended). */
    protected final void close() {
        if (!ended) {
            ended = true;
            out.close();
        }
    }

    /** Ends the derived stream before the source has: cancels the source, then closes. */
    protected final void finish() {
        if (ended) {
            return;
        }
        ended = true;
        cancelSource();
        out.close();
    }

    protected final boolean isEnded() {
        return ended;
    }

    /// Holds the source back, as {@code asyncMap} does while a future is pending;
    /// every call is matched by one {@link #resumeSource}.
    protected final void pauseSource() {
        sourcePauses++;
        if (sub != null) {
            sub.pause();
        }
    }

    protected final void resumeSource() {
        if (sourcePauses > 0) {
            sourcePauses--;
            if (sub != null) {
                sub.resume();
            }
        }
    }

    private void cancelSource() {
        if (sub != null) {
            sub.cancel();
        } else {
            // Still inside source.listen(): cancelled once it returns.
            cancelWanted = true;
        }
    }

    private void safeData(T v) {
        if (ended) {
            return;
        }
        try {
            data(v);
        } catch (Throwable t) {
            addError(t);
            finish();
        }
    }

    private void safeError(Object e) {
        if (ended) {
            return;
        }
        try {
            error(e);
        } catch (Throwable t) {
            addError(t);
            finish();
        }
    }

    private void safeDone() {
        if (ended) {
            return;
        }
        try {
            done();
        } catch (Throwable t) {
            addError(t);
        }
        close();
    }

    /// The derived stream: every listen subscribes to {@code source} and makes a new
    /// {@code Forward} with {@code factory}.
    public static <T, R> Stream<R> bind(final Stream<T> source, final Funcs.Func0<? extends Forward<T, R>> factory) {
        final StreamController<R> out = new StreamController<R>();
        // Events are forwarded as they arrive from the source, not one microtask later.
        out.sync(true);
        final Forward<?, ?>[] cur = new Forward<?, ?>[1];
        out.onListen(new Funcs.VoidFunc0() {
            @Override
            public void call() {
                final Forward<T, R> f = factory.call();
                cur[0] = f;
                f.out = out;
                StreamSubscription<T> s = source.listen(new Funcs.VoidFunc1<T>() {
                    @Override
                    public void call(T v) {
                        f.safeData(v);
                    }
                }, new Funcs.VoidFunc1<Object>() {
                    @Override
                    public void call(Object e) {
                        f.safeError(e);
                    }
                }, new Funcs.VoidFunc0() {
                    @Override
                    public void call() {
                        f.safeDone();
                    }
                });
                f.sub = s;
                if (f.cancelWanted) {
                    s.cancel();
                    return;
                }
                for (int i = 0; i < f.sourcePauses; i++) {
                    s.pause();
                }
                f.started();
            }
        });
        out.onPause(new Funcs.VoidFunc0() {
            @Override
            public void call() {
                Forward<?, ?> f = cur[0];
                if (f != null && f.sub != null && !f.ended) {
                    f.sub.pause();
                }
            }
        });
        out.onResume(new Funcs.VoidFunc0() {
            @Override
            public void call() {
                Forward<?, ?> f = cur[0];
                if (f != null && f.sub != null && !f.ended) {
                    f.sub.resume();
                }
            }
        });
        out.onCancel(new Funcs.VoidFunc0() {
            @Override
            public void call() {
                Forward<?, ?> f = cur[0];
                if (f != null && !f.ended) {
                    f.ended = true;
                    f.cancelSource();
                }
            }
        });
        return out.stream();
    }

    /** Called once the source subscription exists; {@code take(0)} ends here. */
    protected void started() {
    }
}
