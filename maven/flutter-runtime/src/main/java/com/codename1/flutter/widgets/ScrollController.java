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

import com.codename1.flutter.animation.Curve;

import dart.async.Future;
import dart.core.Duration;
import dart.runtime.Funcs;

import java.util.ArrayList;
import java.util.List;

/**
 * Controls the offset of a scrollable ({@code ScrollController} in Flutter). The
 * new_gallery desktop carousel reads {@link #offset()} / {@link #position()},
 * calls {@link #animateTo}, and adds a listener to toggle its paging buttons.
 *
 * <p>The controller owns a {@link ScrollPosition}; the mounted scroll render
 * element keeps that position's extents in sync and forwards user scrolls, which
 * notify listeners.</p>
 */
public class ScrollController {

    private final ScrollPosition scrollPosition = new ScrollPosition();
    private final List<Funcs.VoidFunc0> listeners = new ArrayList<Funcs.VoidFunc0>();
    private double initialScrollOffset;
    private boolean keepScrollOffset = true;
    private String debugLabel;
    private boolean attached;

    public ScrollController() {
    }

    // Named-parameter setters.
    public void initialScrollOffset(double v) {
        this.initialScrollOffset = v;
        this.scrollPosition.setPixels(v);
    }

    public void keepScrollOffset(boolean v) {
        this.keepScrollOffset = v;
    }

    public void debugLabel(String v) {
        this.debugLabel = v;
    }

    public double offset() {
        return scrollPosition.pixels();
    }

    public ScrollPosition position() {
        return scrollPosition;
    }

    public boolean hasClients() {
        return attached;
    }

    public Future<Object> animateTo(double offset, Duration duration, Curve curve) {
        scrollPosition.jumpTo(offset);
        if (client != null) {
            client.scrollToOffset(offset);
        }
        notifyListeners();
        return Future.value(null);
    }

    public void jumpTo(double value) {
        scrollPosition.jumpTo(value);
        if (client != null) {
            client.scrollToOffset(value);
        }
        notifyListeners();
    }

    public void addListener(Funcs.VoidFunc0 listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(Funcs.VoidFunc0 listener) {
        listeners.remove(listener);
    }

    public void dispose() {
        listeners.clear();
        attached = false;
    }

    // ------------------------------------------------------------------
    // Framework plumbing
    // ------------------------------------------------------------------

    /**
     * The scrollable a controller drives: the mounted list moves itself to an offset.
     *
     * <p>A ListView used to store its controller and never attach it, so hasClients
     * stayed false, user scrolling never reached offset or the listeners, and
     * jumpTo/animateTo moved only this detached model while the list sat still.</p>
     */
    interface Client {
        /** Scrolls to {@code offset} logical pixels. */
        void scrollToOffset(double offset);
    }

    private Client client;

    void attach() {
        this.attached = true;
    }

    /** Attaches the scrollable this controller drives. */
    void attach(Client c) {
        this.client = c;
        this.attached = true;
        // The position this controller already holds -- its initialScrollOffset, or
        // where an earlier list left it -- is where the newly attached one starts.
        // Recording it only here left the list at zero while offset reported it.
        if (c != null && scrollPosition.pixels() != 0) {
            c.scrollToOffset(scrollPosition.pixels());
        }
    }

    void detach() {
        this.attached = false;
        this.client = null;
    }

    /** Detaches {@code c}, if it is still the one attached. */
    void detach(Client c) {
        if (client == c) {
            detach();
        }
    }

    /** The user scrolled the attached list to {@code offset} logical pixels. */
    void userScrolled(double offset, double maxExtent, double viewport) {
        scrollPosition.applyViewportDimension(viewport);
        scrollPosition.applyContentDimensions(0, maxExtent);
        if (scrollPosition.pixels() != offset) {
            scrollPosition.setPixels(offset);
            notifyListeners();
        }
    }

    void notifyListeners() {
        com.codename1.flutter.foundation.Listeners.notify(listeners);
    }
}
