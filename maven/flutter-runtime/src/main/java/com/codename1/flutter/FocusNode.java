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
package com.codename1.flutter;

import dart.runtime.Funcs;

import java.util.ArrayList;
import java.util.List;

/**
 * An object that can request keyboard focus ({@code FocusNode} in Flutter). The
 * gallery creates these in {@code initState}, hands them to text fields via the
 * {@code focusNode:} parameter, and disposes them.
 *
 * <p>A node is bridged to the component it is given to through a {@link Host}:
 * requestFocus and unfocus move the real Codename One focus (and so the caret
 * and the keyboard), and the component reports focus it gains or loses by
 * itself back through {@link #hostFocusChanged}. Tracking only a private flag
 * reported hasFocus true while nothing on screen was focused, so "focus the
 * first invalid field" did nothing. As in Flutter, one node holds the primary
 * focus at a time.</p>
 */
public class FocusNode {

    /**
     * The component a node is attached to. Framework plumbing, public only
     * because the field render elements live in another package.
     */
    public interface Host {
        /** Gives the component the focus and starts editing it. */
        void focusHost();

        /** Takes the focus away from the component and stops editing it. */
        void blurHost();
    }

    /** The node holding the primary focus, if any. */
    private static FocusNode primary;

    private String debugLabel;
    private boolean skipTraversal;
    private boolean canRequestFocus = true;
    private boolean hasFocus;
    private Host host;
    private final List<Funcs.VoidFunc0> listeners = new ArrayList<Funcs.VoidFunc0>();

    public FocusNode() {
    }

    // Named constructor parameter setters.

    public void debugLabel(String v) {
        this.debugLabel = v;
    }

    public void skipTraversal(boolean v) {
        this.skipTraversal = v;
    }

    public void canRequestFocus(boolean v) {
        this.canRequestFocus = v;
    }

    public boolean hasFocus() {
        return hasFocus;
    }

    public boolean hasPrimaryFocus() {
        return hasFocus;
    }

    public void requestFocus() {
        requestFocus(null);
    }

    public void requestFocus(FocusNode node) {
        if (node != null && node != this) {
            node.requestFocus();
            return;
        }
        if (!canRequestFocus) {
            return;
        }
        setHasFocus(true);
        if (host != null) {
            host.focusHost();
        }
    }

    public void unfocus() {
        if (hasFocus && host != null) {
            host.blurHost();
        }
        setHasFocus(false);
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
        if (primary == this) {
            primary = null;
        }
        host = null;
    }

    // ------------------------------------------------------------------
    // Framework plumbing
    // ------------------------------------------------------------------

    /** The node holding the primary focus, or null. */
    public static FocusNode primaryFocus() {
        return primary;
    }

    /**
     * Attaches the component this node focuses. A focus requested before the
     * component existed -- in initState, say -- is handed to it now.
     */
    public void attachHost(Host h) {
        this.host = h;
        if (h != null && hasFocus) {
            h.focusHost();
        }
    }

    /** Detaches {@code h}, if it is still the attached component. */
    public void detachHost(Host h) {
        if (host == h) {
            host = null;
        }
    }

    /** The attached component gained or lost the focus by itself (a tap, the keyboard closing). */
    public void hostFocusChanged(boolean focused) {
        setHasFocus(focused);
    }

    void setHasFocus(boolean focus) {
        if (this.hasFocus == focus) {
            return;
        }
        if (focus) {
            FocusNode previous = primary;
            primary = this;
            if (previous != null && previous != this) {
                // The node that had the focus loses it; its component is left to
                // Codename One, which moves the focus off it when this one takes it.
                previous.setHasFocus(false);
            }
        } else if (primary == this) {
            primary = null;
        }
        this.hasFocus = focus;
        com.codename1.flutter.foundation.Listeners.notify(listeners);
    }
}
