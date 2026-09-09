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

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * A low-level pointer-event listener — Flutter's {@code Listener}. Structural
 * pass-through: the single {@code child} renders unchanged; the pointer
 * callbacks are held for a later input pass.
 */
public class Listener extends Widget implements HasChild {

    private Funcs.VoidFunc1<Object> onPointerDown;
    private Funcs.VoidFunc1<Object> onPointerMove;
    private Funcs.VoidFunc1<Object> onPointerUp;
    private Funcs.VoidFunc1<Object> onPointerCancel;
    private Funcs.VoidFunc1<Object> onPointerHover;
    private Funcs.VoidFunc1<Object> onPointerSignal;
    private Object behavior;
    private Widget child;

    public void onPointerDown(Funcs.VoidFunc1<Object> v) { this.onPointerDown = v; }
    public void onPointerMove(Funcs.VoidFunc1<Object> v) { this.onPointerMove = v; }
    public void onPointerUp(Funcs.VoidFunc1<Object> v) { this.onPointerUp = v; }
    public void onPointerCancel(Funcs.VoidFunc1<Object> v) { this.onPointerCancel = v; }
    public void onPointerHover(Funcs.VoidFunc1<Object> v) { this.onPointerHover = v; }
    public void onPointerSignal(Funcs.VoidFunc1<Object> v) { this.onPointerSignal = v; }
    public void behavior(Object v) { this.behavior = v; }

    public void child(Widget v) {
        this.child = v;
    }

    @Override
    public Widget getChild() {
        return child;
    }

    @Override
    public Element createElement() {
        return new PassThroughRenderElement(this);
    }
}
