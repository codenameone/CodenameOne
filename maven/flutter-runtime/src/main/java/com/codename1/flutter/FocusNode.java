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
 * {@code focusNode:} parameter, and disposes them. This implementation tracks
 * focus state and listeners; wiring to the actual CN1 component focus is left to
 * the field render elements.
 */
public class FocusNode {

    private String debugLabel;
    private boolean skipTraversal;
    private boolean canRequestFocus = true;
    private boolean hasFocus;
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

    public void requestFocus(FocusNode node) {
        if (canRequestFocus) {
            setHasFocus(true);
        }
    }

    public void unfocus() {
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
    }

    // ------------------------------------------------------------------
    // Framework plumbing
    // ------------------------------------------------------------------

    void setHasFocus(boolean focus) {
        if (this.hasFocus != focus) {
            this.hasFocus = focus;
            com.codename1.flutter.foundation.Listeners.notify(listeners);
        }
    }
}
