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
package com.codename1.flutter.widgets;

/**
 * An immutable snapshot of interaction with an asynchronous computation, handed
 * to a {@code FutureBuilder} / {@code StreamBuilder} builder, mirroring
 * Flutter's {@code AsyncSnapshot<T>}. The about page reads {@link #hasData()}
 * and {@link #data()}.
 *
 * @param <T> the type of the async value
 */
public class AsyncSnapshot<T> {

    private final ConnectionState connectionState;
    private final T data;
    private final Object error;
    private final Object stackTrace;

    public AsyncSnapshot() {
        this(ConnectionState.none, null, null, null);
    }

    public AsyncSnapshot(ConnectionState connectionState, T data, Object error, Object stackTrace) {
        this.connectionState = connectionState;
        this.data = data;
        this.error = error;
        this.stackTrace = stackTrace;
    }

    public ConnectionState connectionState() {
        return connectionState;
    }

    public T data() {
        return data;
    }

    public Object error() {
        return error;
    }

    public Object stackTrace() {
        return stackTrace;
    }

    public boolean hasData() {
        return data != null;
    }

    public boolean hasError() {
        return error != null;
    }

    /**
     * Flutter's {@code requireData}: the data, or the stored error rethrown, or a
     * StateError when there is neither. Returning null let a builder carry on and
     * fail later at an unrelated dereference, with the original asynchronous error
     * lost.
     */
    public T requireData() {
        if (data != null) {
            return data;
        }
        if (error != null) {
            throw dart.runtime.DartRuntime.asError(error);
        }
        throw new dart.core.StateError("Snapshot has neither data nor error");
    }
}
