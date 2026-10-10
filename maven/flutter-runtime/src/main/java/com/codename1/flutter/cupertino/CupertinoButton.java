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
package com.codename1.flutter.cupertino;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.Key;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.material.ButtonBase;
import com.codename1.flutter.material.ElevatedButton;
import com.codename1.flutter.material.TextButton;

import dart.runtime.Funcs;

/**
 * An iOS-style button — Flutter's {@code CupertinoButton}. The default variant
 * is a borderless tinted-text button; {@code CupertinoButton.filled} has a
 * solid background. A null {@code onPressed} disables it. Composed onto the
 * material {@link TextButton} (default) / {@link ElevatedButton} (filled).
 */
public class CupertinoButton extends StatelessWidget {

    private Funcs.VoidFunc0 onPressed;
    private Widget child;
    private boolean filled;

    public void onPressed(Funcs.VoidFunc0 v) {
        this.onPressed = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public void padding(Object v) {
    }

    public void color(Color v) {
    }

    public void disabledColor(Color v) {
    }

    public void minSize(double v) {
    }

    public void pressedOpacity(double v) {
    }

    public void borderRadius(Object v) {
    }

    public void alignment(Object v) {
    }

    /**
     * Dart's {@code CupertinoButton.filled} named constructor in canonical
     * positional form.
     */
    public static CupertinoButton filled(Key key, Funcs.VoidFunc0 onPressed, Widget child) {
        CupertinoButton b = new CupertinoButton();
        b.key(key);
        b.onPressed = onPressed;
        b.child = child;
        b.filled = true;
        return b;
    }

    @Override
    public Widget build(BuildContext context) {
        ButtonBase b = filled ? new ElevatedButton() : new TextButton();
        b.onPressed(onPressed);
        b.child(child);
        return b;
    }
}
