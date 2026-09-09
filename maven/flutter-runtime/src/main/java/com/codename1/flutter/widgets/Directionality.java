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
import com.codename1.flutter.TextDirection;
import com.codename1.flutter.Widget;

/**
 * Establishes the reading direction for its subtree, mirroring Flutter's
 * {@code Directionality}. Layout-transparent in this runtime: it simply wraps
 * its child; the recorded {@link TextDirection} is available for later
 * bidi-aware rendering.
 */
public class Directionality extends Widget {

    private TextDirection textDirection;
    private Widget child;

    public void textDirection(TextDirection v) {
        this.textDirection = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public TextDirection getTextDirection() {
        return textDirection;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Element createElement() {
        return new DirectionalityRenderElement(this);
    }

    /**
     * Dart's {@code Directionality.of(context)}: the ambient text direction.
     * This runtime does not scope directionality through the element tree, so
     * the default LTR reading direction is reported.
     */
    public static TextDirection of(BuildContext context) {
        return TextDirection.ltr;
    }
}
