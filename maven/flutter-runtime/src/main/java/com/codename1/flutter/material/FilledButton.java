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

import com.codename1.flutter.Key;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * Material 3's {@code FilledButton}: the elevated button's shape and layout with the
 * primary colour as its face and no shadow. {@code FilledButton.tonal} uses the
 * secondary container colours instead. See ButtonRenderElement's elevated branch.
 */
public class FilledButton extends ElevatedButton {
    private boolean tonal;

    public FilledButton() {
    }

    /** Whether this is the tonal variant. */
    public boolean isTonal() {
        return tonal;
    }

    public static FilledButton tonal(Key key, Funcs.VoidFunc0 onPressed, ButtonStyle style, Widget child) {
        FilledButton b = new FilledButton();
        b.tonal = true;
        b.key(key);
        b.onPressed(onPressed);
        b.style(style);
        b.child(child);
        return b;
    }

    public static FilledButton icon(Key key, Funcs.VoidFunc0 onPressed, ButtonStyle style,
            Widget icon, Widget label) {
        FilledButton b = new FilledButton();
        b.key(key);
        b.onPressed(onPressed);
        b.style(style);
        b.child(label != null ? label : icon);
        if (label != null) {
            b.leadingIcon(icon);
        }
        return b;
    }
}
