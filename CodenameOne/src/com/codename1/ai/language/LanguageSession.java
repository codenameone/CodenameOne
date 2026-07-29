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
package com.codename1.ai.language;

import com.codename1.impl.LanguageImpl;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;

/// Shared lifecycle implementation for the public feature-specific language
/// sessions. This package-private type keeps native backends alive across
/// repeated operations and defers release while an operation is pending.
final class LanguageSession implements AutoCloseable {
    interface Operation<T> {
        AsyncResource<T> run(LanguageImpl implementation,
                             LanguageOptions options);
    }

    private final LanguageImpl implementation;
    private final LanguageOptions options;
    private boolean closed;
    private int activeOperations;
    private boolean closePending;

    private LanguageSession(LanguageImpl implementation,
                            LanguageOptions options) {
        this.implementation = implementation;
        this.options = options;
    }

    static boolean isSupported(String feature, LanguageOptions options) {
        LanguageImpl implementation =
                Display.getInstance().getLanguageBackend();
        if (implementation == null) {
            return false;
        }
        try {
            LanguageOptions actual = options == null
                    ? new LanguageOptions() : options;
            return implementation.isSupported(feature,
                    actual.getBackend().getId());
        } finally {
            implementation.close();
        }
    }

    static LanguageSession open(String feature, LanguageOptions options) {
        LanguageImpl implementation =
                Display.getInstance().getLanguageBackend();
        LanguageOptions actual = (options == null
                ? new LanguageOptions() : options).snapshot();
        if (implementation == null
                || !implementation.isSupported(feature,
                        actual.getBackend().getId())) {
            if (implementation != null) {
                implementation.close();
            }
            throw new UnsupportedOperationException(
                    feature + " is not supported");
        }
        return new LanguageSession(implementation, actual);
    }

    <T> AsyncResource<T> execute(Operation<T> operation) {
        return execute(operation, false);
    }

    <T> AsyncResource<T> execute(Operation<T> operation,
                                 final boolean closeWhenFinished) {
        final AsyncResource<T> backendResult;
        synchronized (this) {
            if (closed) {
                throw new IllegalStateException(
                        "Language session is closed");
            }
            activeOperations++;
            try {
                backendResult = operation.run(implementation, options);
                if (backendResult == null) {
                    throw new IllegalStateException(
                            "Language backend returned no asynchronous result");
                }
            } catch (RuntimeException error) {
                activeOperations--;
                throw error;
            } catch (Error error) {
                activeOperations--;
                throw error;
            }
        }
        final OperationResource<T> result =
                new OperationResource<T>(this, closeWhenFinished);
        final Completion completion = new Completion();
        backendResult.ready(new SuccessCallback<T>() {
            public void onSucess(T value) {
                if (completion.finish()) {
                    if (closeWhenFinished) {
                        close();
                    }
                    operationFinished();
                    result.publish(value);
                }
            }
        }).except(new SuccessCallback<Throwable>() {
            public void onSucess(Throwable error) {
                if (completion.finish()) {
                    if (closeWhenFinished) {
                        close();
                    }
                    operationFinished();
                    result.fail(error);
                }
            }
        });
        return result;
    }

    private void operationFinished() {
        LanguageImpl toClose = null;
        synchronized (this) {
            activeOperations--;
            if (activeOperations == 0 && closePending) {
                closePending = false;
                toClose = implementation;
            }
        }
        if (toClose != null) {
            toClose.close();
        }
    }

    public void close() {
        LanguageImpl toClose = null;
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
            if (activeOperations == 0) {
                toClose = implementation;
            } else {
                closePending = true;
            }
        }
        if (toClose != null) {
            toClose.close();
        }
    }

    private static final class OperationResource<T>
            extends AsyncResource<T> {
        private final LanguageSession owner;
        private final boolean closeWhenFinished;

        OperationResource(LanguageSession owner,
                          boolean closeWhenFinished) {
            this.owner = owner;
            this.closeWhenFinished = closeWhenFinished;
        }

        public synchronized boolean cancel(boolean mayInterruptIfRunning) {
            boolean cancelled = super.cancel(mayInterruptIfRunning);
            if (cancelled && closeWhenFinished) {
                owner.close();
            }
            return cancelled;
        }

        synchronized void publish(T value) {
            if (!isCancelled()) {
                complete(value);
            }
        }

        synchronized void fail(Throwable error) {
            if (!isCancelled()) {
                error(error);
            }
        }
    }

    private static final class Completion {
        private boolean finished;

        synchronized boolean finish() {
            if (finished) {
                return false;
            }
            finished = true;
            return true;
        }
    }
}
