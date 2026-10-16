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

import com.codename1.flutter.ComposedElement;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

import dart.async.Future;
import dart.runtime.Funcs;

/**
 * Element for {@link FutureBuilder}: holds the snapshot and subscribes to the future,
 * mirroring Flutter's {@code _FutureBuilderState}.
 *
 * <p>FutureBuilder used to be a plain stateless widget that never read its future, so it
 * built its initial snapshot once and stayed there: every result and every error was
 * dropped, and a screen fed by a future sat on its loading state for good.</p>
 *
 * <p>As in Flutter: the snapshot starts from {@code initialData} (state none), turns
 * {@code waiting} while a future is pending and {@code done} with its value or error; a
 * NEW future (by identity) resubscribes from {@code none}, and a callback from a future
 * this element no longer tracks -- replaced, or unmounted -- is ignored.</p>
 */
public class FutureBuilderElement extends ComposedElement {

    private AsyncSnapshot snapshot;
    private Object subscribedTo;
    /** Identifies the live subscription; a callback carrying another one is stale. */
    private Object activeCallback;
    private boolean subscribing;

    public FutureBuilderElement(FutureBuilder<?> widget) {
        super(widget);
    }

    @Override
    public void mount(Element parent, int slot) {
        FutureBuilder<?> w = (FutureBuilder<?>) widget();
        snapshot = w.getInitialData() != null
                ? new AsyncSnapshot(ConnectionState.none, w.getInitialData(), null, null)
                : new AsyncSnapshot();
        subscribe(w.getFuture());
        super.mount(parent, slot);
    }

    @Override
    public void update(Widget newWidget) {
        Object next = ((FutureBuilder<?>) newWidget).getFuture();
        if (next != subscribedTo) {
            activeCallback = null;
            snapshot = new AsyncSnapshot(ConnectionState.none, snapshot.data(), snapshot.error(),
                    snapshot.stackTrace());
            subscribe(next);
        }
        super.update(newWidget);
    }

    @Override
    public void unmount() {
        activeCallback = null;
        subscribedTo = null;
        super.unmount();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void subscribe(Object future) {
        subscribedTo = future;
        if (!(future instanceof Future)) {
            return;
        }
        final Object token = new Object();
        activeCallback = token;
        subscribing = true;
        try {
            ((Future) future).then(new Funcs.VoidFunc1<Object>() {
                @Override
                public void call(Object value) {
                    settle(token, new AsyncSnapshot(ConnectionState.done, value, null, null));
                }
            }).catchError(new Funcs.VoidFunc1<Object>() {
                @Override
                public void call(Object error) {
                    settle(token, new AsyncSnapshot(ConnectionState.done, null, error, null));
                }
            });
        } finally {
            subscribing = false;
        }
        if (activeCallback == token && snapshot.connectionState() != ConnectionState.done) {
            snapshot = new AsyncSnapshot(ConnectionState.waiting, snapshot.data(), snapshot.error(),
                    snapshot.stackTrace());
        }
    }

    private void settle(final Object token, final AsyncSnapshot result) {
        if (subscribing) {
            // An already-completed future answers during subscribe(), before this
            // element has built: take the result as the snapshot to build with.
            if (activeCallback == token) {
                snapshot = result;
            }
            return;
        }
        Runnable apply = new Runnable() {
            @Override
            public void run() {
                if (activeCallback != token) {
                    return;   // a replaced future, or an unmounted element
                }
                snapshot = result;
                markNeedsBuild();
            }
        };
        // Completion can arrive off the EDT (a headless timer, a network callback);
        // element state is only ever touched on it.
        if (com.codename1.ui.Display.isInitialized() && !com.codename1.ui.CN.isEdt()) {
            com.codename1.ui.CN.callSerially(apply);
        } else {
            apply.run();
        }
    }

    @Override
    protected Widget build() {
        return ((FutureBuilder<?>) widget()).buildWith(this, snapshot);
    }
}
