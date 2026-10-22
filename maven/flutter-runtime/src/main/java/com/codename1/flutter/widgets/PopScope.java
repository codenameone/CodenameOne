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
 * Flutter's PopScope: {@code canPop: false} vetoes a back gesture or {@code maybePop}
 * on the route it sits in, and {@code onPopInvokedWithResult} (or the older
 * {@code onPopInvoked}) hears every attempt, vetoed or not. {@code Navigator.pop} itself
 * is never vetoed, as in Flutter.
 */
public class PopScope<T> extends Widget implements HasChild {

    private Widget child;
    private boolean canPop = true;
    private Funcs.VoidFunc1<Boolean> onPopInvoked;
    private Funcs.VoidFunc2<Boolean, Object> onPopInvokedWithResult;

    public void child(Widget v) {
        this.child = v;
    }

    public void canPop(boolean v) {
        this.canPop = v;
    }

    public void onPopInvoked(Funcs.VoidFunc1<Boolean> v) {
        this.onPopInvoked = v;
    }

    public void onPopInvokedWithResult(Funcs.VoidFunc2<Boolean, Object> v) {
        this.onPopInvokedWithResult = v;
    }

    public boolean getCanPop() {
        return canPop;
    }

    /** Tells the app a pop was attempted; {@code didPop} is false when this scope vetoed it. */
    public void invoked(boolean didPop, Object result) {
        if (onPopInvokedWithResult != null) {
            onPopInvokedWithResult.call(Boolean.valueOf(didPop), result);
        } else if (onPopInvoked != null) {
            onPopInvoked.call(Boolean.valueOf(didPop));
        }
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
