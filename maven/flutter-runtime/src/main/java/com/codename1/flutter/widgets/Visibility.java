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

/**
 * Whether (and how) to include its {@code child} in the tree — Flutter's {@code Visibility}.
 *
 * <p>{@code visible: false} shows the {@code replacement} instead — nothing, by
 * default. It used to draw the child regardless, which is the loudest possible
 * reading of "do not show this".</p>
 *
 * <p>{@code maintainState} is not modelled: a hidden child is rebuilt when it
 * comes back rather than kept alive. {@code maintainSize} is honoured only in
 * that a replacement can hold space if one is given.</p>
 */
public class Visibility extends Widget implements HasChild {

    private boolean visible = true;
    private Widget replacement;
    private boolean maintainState;
    private boolean maintainAnimation;
    private boolean maintainSize;
    private boolean maintainSemantics;
    private boolean maintainInteractivity;
    private Widget child;

    public void visible(boolean v) { this.visible = v; }
    public void replacement(Widget v) { this.replacement = v; }
    public void maintainState(boolean v) { this.maintainState = v; }
    public void maintainAnimation(boolean v) { this.maintainAnimation = v; }
    public void maintainSize(boolean v) { this.maintainSize = v; }
    public void maintainSemantics(boolean v) { this.maintainSemantics = v; }
    public void maintainInteractivity(boolean v) { this.maintainInteractivity = v; }
    public boolean getVisible() { return visible; }

    public void child(Widget v) {
        this.child = v;
    }

    @Override
    public Widget getChild() {
        if (visible) {
            return child;
        }
        if (replacement != null) {
            return replacement;
        }
        // Flutter's default replacement is SizedBox.shrink() — an empty box,
        // not the child it was just told to hide.
        SizedBox empty = new SizedBox();
        empty.width(0);
        empty.height(0);
        return empty;
    }

    @Override
    public Element createElement() {
        return new PassThroughRenderElement(this);
    }
}
