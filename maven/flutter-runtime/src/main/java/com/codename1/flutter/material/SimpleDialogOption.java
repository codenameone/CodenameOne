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
package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * A single tappable option inside a {@link SimpleDialog} — Flutter's
 * {@code SimpleDialogOption}. Tapping fires {@code onPressed} (conventionally to pop the
 * dialog with a value).
 *
 * <p>The tap used to go nowhere, which made a SimpleDialog a list you could read and not
 * answer. Padding follows Material's option metrics (16lp horizontal, 8lp vertical).
 */
public class SimpleDialogOption extends StatelessWidget {

    private Object onPressed;
    private EdgeInsets padding;
    private Widget child;

    public void onPressed(dart.runtime.Funcs.VoidFunc0 v) {
        this.onPressed = v;
    }

    public void padding(EdgeInsets v) {
        this.padding = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Object getOnPressed() {
        return onPressed;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Widget build(BuildContext context) {
        com.codename1.flutter.widgets.Padding pad = new com.codename1.flutter.widgets.Padding();
        pad.padding(padding != null ? padding : EdgeInsets.symmetric(8, 16));
        pad.child(child);
        if (!(onPressed instanceof dart.runtime.Funcs.VoidFunc0)) {
            return pad;
        }
        InkWell tap = new InkWell();
        tap.child(pad);
        tap.onTap((dart.runtime.Funcs.VoidFunc0) onPressed);
        return tap;
    }
}
