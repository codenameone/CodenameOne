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
 * Element for a {@link StatefulWidget}. Owns the {@link State} instance:
 * created (and {@code initState} run) on mount, retargeted with
 * {@code didUpdateWidget} when a new widget of the same type/key arrives,
 * disposed on unmount.
 */
public class StatefulElement extends ComposedElement {

    private final State<? extends StatefulWidget> state;

    public StatefulElement(StatefulWidget widget) {
        super(widget);
        this.state = widget.createState();
        this.state.attach(this, widget);
    }

    public State<? extends StatefulWidget> state() {
        return state;
    }

    @Override
    protected Object globalKeyState() {
        return state;
    }

    @Override
    protected void firstBuild() {
        state.initState();
        // Flutter runs didChangeDependencies right after initState and before the first
        // build; widgets that create controllers there (e.g. a PageController sized from
        // MediaQuery) rely on it having run before build reads them.
        state.didChangeDependencies();
        super.firstBuild();
    }

    @Override
    public void update(Widget newWidget) {
        StatefulWidget oldWidget = (StatefulWidget) widget;
        widget = newWidget;
        state.updateWidget((StatefulWidget) newWidget);
        state.invokeDidUpdateWidget(oldWidget);
        dirty = true;
        performRebuild();
    }

    @Override
    public void unmount() {
        super.unmount();
        state.dispose();
        state.detach();
    }

    /** Set when a dependency changed; State.didChangeDependencies runs before the next build. */
    private boolean dependenciesChanged;

    /**
     * Flutter runs State.didChangeDependencies whenever an inherited widget the state read
     * changes, before rebuilding it. It only ever ran after initState here, so a state that
     * refreshed locale-derived data or re-created a controller from MediaQuery there kept
     * stale fields after a Theme, MediaQuery or localization change.
     */
    @Override
    public void didChangeDependencies() {
        dependenciesChanged = true;
        super.didChangeDependencies();
    }

    @Override
    protected Widget build() {
        if (dependenciesChanged) {
            dependenciesChanged = false;
            state.didChangeDependencies();
        }
        return state.build(this);
    }
}
