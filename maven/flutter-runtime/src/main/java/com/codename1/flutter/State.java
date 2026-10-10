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

/**
 * Mutable state for a {@link StatefulWidget}. Owned by a
 * {@link StatefulElement}: created on mount, retargeted at new widget
 * instances on update (with {@link #didUpdateWidget}), disposed on unmount.
 */
public abstract class State<T extends StatefulWidget> {

    StatefulElement element;
    private T widgetValue;

    /**
     * The current widget configuration for this state.
     */
    public T widget() {
        return widgetValue;
    }

    /**
     * The location of this state's widget in the element tree.
     */
    public BuildContext context() {
        return element;
    }

    /**
     * Runs {@code fn} (which mutates fields of this state) and schedules a
     * rebuild of this element for the next frame.
     */
    public void setState(Funcs.VoidFunc0 fn) {
        FlutterUI.assertEdt();
        if (fn != null) {
            fn.call();
        }
        if (element != null) {
            element.markNeedsBuild();
        }
    }

    /**
     * Called once when the element is first mounted, before the first build.
     */
    public void initState() {
    }

    /**
     * Called immediately after {@link #initState} and again whenever an
     * inherited widget this state depends on changes. No-op by default.
     */
    public void didChangeDependencies() {
    }

    /**
     * Whether this state is currently in the tree ({@code State.mounted}):
     * true between mount and {@link #dispose()}.
     */
    public boolean mounted() {
        return element != null;
    }

    /**
     * Called when the element absorbed a new widget configuration. The new
     * widget is already available via {@link #widget()}.
     */
    public void didUpdateWidget(T oldWidget) {
    }

    /**
     * Called when the element is removed from the tree permanently.
     */
    public void dispose() {
    }

    public abstract Widget build(BuildContext context);

    // ------------------------------------------------------------------
    // Framework plumbing (package private)
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    void attach(StatefulElement element, StatefulWidget widget) {
        this.element = element;
        this.widgetValue = (T) widget;
    }

    @SuppressWarnings("unchecked")
    void updateWidget(StatefulWidget widget) {
        this.widgetValue = (T) widget;
    }

    @SuppressWarnings("unchecked")
    void invokeDidUpdateWidget(StatefulWidget oldWidget) {
        didUpdateWidget((T) oldWidget);
    }

    void detach() {
        this.element = null;
    }
}
