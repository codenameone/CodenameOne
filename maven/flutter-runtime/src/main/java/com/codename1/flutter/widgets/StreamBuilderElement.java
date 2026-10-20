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

import dart.async.Stream;
import dart.async.StreamSubscription;
import dart.runtime.Funcs;

/**
 * Holds the subscription behind a {@link StreamBuilder}. A new stream on rebuild
 * cancels the old subscription and keeps the last data, as Flutter's does; unmount
 * cancels it.
 */
public class StreamBuilderElement extends ComposedElement {

    private AsyncSnapshot snapshot;
    private Object subscribedTo;
    private StreamSubscription<?> subscription;
    /** Identifies the live subscription; an event carrying another one is stale. */
    private Object activeToken;
    private boolean subscribing;

    public StreamBuilderElement(StreamBuilder<?> widget) {
        super(widget);
    }

    @Override
    public void mount(Element parent, int slot) {
        StreamBuilder<?> w = (StreamBuilder<?>) widget();
        snapshot = new AsyncSnapshot(ConnectionState.none, w.getInitialData(), null, null);
        subscribe(w.getStream());
        super.mount(parent, slot);
    }

    @Override
    public void update(Widget newWidget) {
        Object next = ((StreamBuilder<?>) newWidget).getStream();
        if (next != subscribedTo) {
            cancel();
            // The error is kept on purpose: Flutter's StreamBuilderBase moves the snapshot
            // with AsyncSnapshot.inState(none) and then inState(waiting), and inState
            // carries data, error and stackTrace alike. Clearing it here would make a
            // builder that tests hasError render differently than it does on Flutter.
            snapshot = new AsyncSnapshot(ConnectionState.none, snapshot.data(), snapshot.error(),
                    snapshot.stackTrace());
            subscribe(next);
        }
        super.update(newWidget);
    }

    @Override
    public void unmount() {
        cancel();
        subscribedTo = null;
        super.unmount();
    }

    private void cancel() {
        activeToken = null;
        if (subscription != null) {
            subscription.cancel();
            subscription = null;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void subscribe(Object stream) {
        subscribedTo = stream;
        if (!(stream instanceof Stream)) {
            return;
        }
        final Object token = new Object();
        activeToken = token;
        snapshot = new AsyncSnapshot(ConnectionState.waiting, snapshot.data(), snapshot.error(),
                snapshot.stackTrace());
        subscribing = true;
        try {
            subscription = ((Stream) stream).listen(new Funcs.VoidFunc1<Object>() {
                @Override
                public void call(Object value) {
                    settle(token, new AsyncSnapshot(ConnectionState.active, value, null, null));
                }
            }, new Funcs.VoidFunc1<Object>() {
                @Override
                public void call(Object error) {
                    settle(token, new AsyncSnapshot(ConnectionState.active, null, error, null));
                }
            }, new Funcs.VoidFunc0() {
                @Override
                public void call() {
                    AsyncSnapshot last = snapshot;
                    settle(token, new AsyncSnapshot(ConnectionState.done, last.data(), last.error(),
                            last.stackTrace()));
                }
            });
        } finally {
            subscribing = false;
        }
    }

    private void settle(final Object token, final AsyncSnapshot result) {
        if (subscribing) {
            // a stream that emits as it is listened to (authStateChanges does) answers
            // before this element has built: build with that event
            if (activeToken == token) {
                snapshot = result;
            }
            return;
        }
        Runnable apply = new Runnable() {
            @Override
            public void run() {
                if (activeToken != token) {
                    return;   // a replaced stream, or an unmounted element
                }
                snapshot = result;
                markNeedsBuild();
            }
        };
        if (com.codename1.ui.Display.isInitialized() && !com.codename1.ui.CN.isEdt()) {
            com.codename1.ui.CN.callSerially(apply);
        } else {
            apply.run();
        }
    }

    @Override
    protected Widget build() {
        return ((StreamBuilder<?>) widget()).buildWith(this, snapshot);
    }
}
