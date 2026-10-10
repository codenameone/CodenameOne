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

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * One entry painted into an {@link Overlay} — Flutter's {@code OverlayEntry}.
 * Feature-discovery builds it from a {@code builder}, rebuilds it via
 * {@link #markNeedsBuild()} and tears it down with {@link #remove()}.
 */
public class OverlayEntry {

    private Funcs.Func1<BuildContext, Widget> builder;
    private Boolean opaque;
    private Boolean maintainState;
    private boolean mounted = true;

    public OverlayEntry() {
    }

    public void builder(Funcs.Func1<BuildContext, Widget> v) {
        this.builder = v;
    }

    public void opaque(Boolean v) {
        this.opaque = v;
    }

    public void maintainState(Boolean v) {
        this.maintainState = v;
    }

    public Funcs.Func1<BuildContext, Widget> getBuilder() {
        return builder;
    }

    private OverlayState owner;

    void attach(OverlayState state) {
        this.owner = state;
    }

    /** Marks the entry as needing to rebuild its content on the next frame. */
    public void markNeedsBuild() {
        if (owner != null) {
            owner.rebuild();
        }
    }

    /** Removes this entry from its overlay. */
    public void remove() {
        mounted = false;
        if (owner != null) {
            owner.forget(this);
            owner = null;
        }
    }

    public boolean mounted() {
        return mounted;
    }
}
