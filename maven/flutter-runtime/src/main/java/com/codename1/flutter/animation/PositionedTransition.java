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
package com.codename1.flutter.animation;

import com.codename1.flutter.Element;
import com.codename1.flutter.RelativeRect;
import com.codename1.flutter.widgets.Positioned;
import com.codename1.flutter.widgets.PositionedRenderElement;

/**
 * Animates the position/size (a {@code RelativeRect}) of a child within a
 * Stack — Flutter's {@code PositionedTransition}. It resolves the animation's
 * current {@code RelativeRect} and hosts the child as a {@link Positioned}
 * (LTRB insets from the stack edges) so the Stack lays it out in place. The
 * gallery's Backdrop drives two of these to slide the home/settings panels.
 *
 * <p>{@link PositionedTransitionElement} follows the animation frame by frame; the
 * widget only carries the configuration.</p>
 */
public class PositionedTransition extends AnimatedChildWidget {

    private Animation<?> rect;

    public void rect(Animation<?> v) {
        this.rect = v;
    }

    public Animation<?> getRect() {
        return rect;
    }

    @Override
    public Element createElement() {
        Positioned p = new Positioned();
        Object v = rect != null ? rect.value() : null;
        RelativeRect r = v instanceof RelativeRect ? (RelativeRect) v : RelativeRect.fill;
        p.left(r.left());
        p.top(r.top());
        p.right(r.right());
        p.bottom(r.bottom());
        p.child(getChild());
        PositionedTransitionElement e = new PositionedTransitionElement(p);
        e.transition(this);
        return e;
    }
}
