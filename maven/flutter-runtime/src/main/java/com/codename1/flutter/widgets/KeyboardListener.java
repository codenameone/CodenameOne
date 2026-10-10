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
import com.codename1.flutter.FocusNode;
import com.codename1.flutter.Widget;
import com.codename1.flutter.services.KeyEvent;

import dart.runtime.Funcs;

/**
 * A raw keyboard listener — Flutter's {@code KeyboardListener}. Structural
 * pass-through: the single {@code child} renders unchanged; the focus node and
 * key-event callback are held for a later input pass.
 */
public class KeyboardListener extends Widget implements HasChild {

    private FocusNode focusNode;
    private Boolean autofocus;
    private Boolean includeSemantics;
    private Funcs.VoidFunc1<KeyEvent> onKeyEvent;
    private Widget child;

    public void focusNode(FocusNode v) { this.focusNode = v; }
    public void autofocus(boolean v) { this.autofocus = v; }
    public void includeSemantics(boolean v) { this.includeSemantics = v; }
    public void onKeyEvent(Funcs.VoidFunc1<KeyEvent> v) { this.onKeyEvent = v; }

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
