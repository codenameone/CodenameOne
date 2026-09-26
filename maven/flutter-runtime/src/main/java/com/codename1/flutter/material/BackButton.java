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

import com.codename1.flutter.Color;
import com.codename1.flutter.Icons;
import com.codename1.flutter.widgets.Icon;

import dart.runtime.Funcs;

/**
 * A material back button: an {@link IconButton} showing the platform back
 * chevron that, when pressed, pops the current route (Flutter's
 * {@code BackButton}). {@code onPressed} overrides the default pop.
 */
public class BackButton extends IconButton {

    public BackButton() {
        icon(new Icon(Icons.arrow_back));
    }

    public BackButton(com.codename1.flutter.Key key, Color color, Funcs.VoidFunc0 onPressed) {
        this();
        if (color != null) {
            color(color);
        }
        if (onPressed != null) {
            onPressed(onPressed);
        }
    }
}
