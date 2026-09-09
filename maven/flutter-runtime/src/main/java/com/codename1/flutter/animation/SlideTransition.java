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
import com.codename1.flutter.Offset;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.FractionalTranslationRenderElement;

/**
 * Slides its child by an animated offset given as a FRACTION of the child's own size —
 * Flutter's {@code SlideTransition}.
 *
 * <p>It is its own render element rather than a wrapper around FractionalTranslation
 * because the fraction has to be read at PAINT time: the animation moves every frame, and
 * rebuilding a wrapper widget per frame to carry the new value would relayout the subtree
 * for what is only a change of where it is drawn.</p>
 */
public class SlideTransition extends AnimatedChildWidget
        implements FractionalTranslationRenderElement.FractionSource {

    private Animation<?> position;

    public void position(Animation<?> v) {
        this.position = v;
        listenable(v);
    }

    public Animation<?> getPosition() {
        return position;
    }

    @Override
    public Offset fraction() {
        if (position == null) {
            return null;
        }
        Object v = position.value();
        return v instanceof Offset ? (Offset) v : null;
    }

    @Override
    public Widget child() {
        return getChild();
    }

    @Override
    public com.codename1.flutter.foundation.Listenable driver() {
        return position;
    }

    @Override
    public Element createElement() {
        return new FractionalTranslationRenderElement(this);
    }
}
