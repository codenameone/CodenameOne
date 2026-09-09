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
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * Makes its {@code child} partially transparent — Flutter's {@code Opacity}.
 * The opacity value (0.0 fully transparent .. 1.0 fully opaque) is captured;
 * this pass renders the child at full opacity, with alpha compositing deferred
 * to the paint layer.
 */
public class Opacity extends Widget {

    private double opacity = 1.0;
    private Widget child;

    public void opacity(double v) {
        this.opacity = v;
    }

    public void alwaysIncludeSemantics(boolean v) {
    }

    public void child(Widget v) {
        this.child = v;
    }

    public double getOpacity() {
        return opacity;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public com.codename1.flutter.Element createElement() {
        return new OpacityRenderElement(this);
    }
}
