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
import com.codename1.flutter.Element;
import com.codename1.flutter.FocusNode;
import com.codename1.flutter.Widget;
import com.codename1.flutter.services.KeyEvent;
import com.codename1.flutter.services.KeyEventResult;

import dart.runtime.Funcs;

/**
 * Manages a {@link FocusNode} for its subtree — Flutter's {@code Focus}.
 * Structural pass-through for this milestone: the {@code child} renders
 * unchanged and the focus/key callbacks are captured. {@code onKeyEvent} is
 * typed as the real Flutter {@code FocusOnKeyEventCallback} so key handlers can
 * inspect the {@link KeyEvent} and return a {@link KeyEventResult}.
 */
public class Focus extends Widget implements HasChild {

    private FocusNode focusNode;
    private Boolean autofocus;
    private Funcs.VoidFunc1<Boolean> onFocusChange;
    private Object onKey;
    private Funcs.Func2<FocusNode, KeyEvent, KeyEventResult> onKeyEvent;
    private Boolean canRequestFocus;
    private Boolean skipTraversal;
    private Boolean descendantsAreFocusable;
    private Boolean includeSemantics;
    private String debugLabel;
    private Widget child;

    public void focusNode(FocusNode v) { this.focusNode = v; }
    public void autofocus(Boolean v) { this.autofocus = v; }
    public void onFocusChange(Funcs.VoidFunc1<Boolean> v) { this.onFocusChange = v; }
    public void onKey(Object v) { this.onKey = v; }
    public void onKeyEvent(Funcs.Func2<FocusNode, KeyEvent, KeyEventResult> v) { this.onKeyEvent = v; }
    public void canRequestFocus(Boolean v) { this.canRequestFocus = v; }
    public void skipTraversal(Boolean v) { this.skipTraversal = v; }
    public void descendantsAreFocusable(Boolean v) { this.descendantsAreFocusable = v; }
    public void includeSemantics(Boolean v) { this.includeSemantics = v; }
    public void debugLabel(String v) { this.debugLabel = v; }

    public void child(Widget v) {
        this.child = v;
    }

    @Override
    public Widget getChild() {
        return child;
    }

    /** Flutter's {@code Focus.of} — the enclosing node (none tracked at this pass). */
    public static FocusNode of(BuildContext context, boolean scopeOk) {
        return null;
    }

    @Override
    public Element createElement() {
        return new PassThroughRenderElement(this);
    }
}
