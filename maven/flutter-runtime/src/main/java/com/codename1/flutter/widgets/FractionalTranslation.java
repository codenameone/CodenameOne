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
import com.codename1.flutter.Offset;
import com.codename1.flutter.Widget;

/**
 * Translates its {@code child} by an {@link Offset} expressed as a fraction of the child's
 * own size before painting — Flutter's {@code FractionalTranslation}.
 */
public class FractionalTranslation extends Widget
        implements FractionalTranslationRenderElement.FractionSource {

    private Offset translation;
    private boolean transformHitTests = true;
    private Widget child;

    public void translation(Offset v) {
        this.translation = v;
    }

    public void transformHitTests(boolean v) {
        this.transformHitTests = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Offset getTranslation() {
        return translation;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Offset fraction() {
        return translation;
    }

    @Override
    public Widget child() {
        return child;
    }

    @Override
    public com.codename1.flutter.foundation.Listenable driver() {
        return null;   // a static translation: nothing to follow
    }

    @Override
    public Element createElement() {
        return new FractionalTranslationRenderElement(this);
    }
}
