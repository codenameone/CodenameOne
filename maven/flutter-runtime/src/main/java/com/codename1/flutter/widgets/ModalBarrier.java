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
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * A full-screen barrier that optionally dismisses a route when tapped —
 * Flutter's {@code ModalBarrier}. new_gallery's backdrop wraps one in a
 * {@code Listener} to intercept taps while the settings page is open. This
 * milestone renders an inert filled box; dismissal is wired by the enclosing
 * gesture handler.
 */
public class ModalBarrier extends StatelessWidget {

    private Color color;
    private boolean dismissible = true;

    public void color(Color v) {
        this.color = v;
    }

    public void dismissible(boolean v) {
        this.dismissible = v;
    }

    public void semanticsLabel(String v) {
    }

    public void barrierSemanticsDismissible(boolean v) {
    }

    public void onDismiss(Object v) {
    }

    public Color getColor() {
        return color;
    }

    public boolean isDismissible() {
        return dismissible;
    }

    @Override
    public Widget build(BuildContext context) {
        return new Container();
    }
}
