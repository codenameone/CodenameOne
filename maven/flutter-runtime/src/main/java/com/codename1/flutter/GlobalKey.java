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

/**
 * A key that is unique across the entire app and provides access to the element,
 * state and context it is attached to ({@code GlobalKey<T>} in Flutter). The
 * gallery mostly uses global keys as stable identity tokens passed to widgets;
 * {@link #currentState()} / {@link #currentContext()} return the live targets
 * once the keyed widget is mounted (null until then).
 *
 * @param <T> the {@code State} (or other) type exposed via {@link #currentState()}
 */
public class GlobalKey<T> extends Key {

    private String debugLabel;
    private T state;
    private BuildContext context;
    private Widget widget;

    public GlobalKey() {
    }

    /** Named constructor parameter {@code debugLabel:}. */
    public void debugLabel(String label) {
        this.debugLabel = label;
    }

    public T currentState() {
        return state;
    }

    public BuildContext currentContext() {
        return context;
    }

    public Widget currentWidget() {
        return widget;
    }

    // ------------------------------------------------------------------
    // Framework plumbing
    // ------------------------------------------------------------------

    public void attach(T state, BuildContext context, Widget widget) {
        this.state = state;
        this.context = context;
        this.widget = widget;
    }

    public void detach() {
        this.state = null;
        this.context = null;
        this.widget = null;
    }
}
