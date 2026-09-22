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

/**
 * The material text button: borderless primary-colored text, backed by a
 * CN1 Button (UIID "FlutterTextButton").
 */
public class TextButton extends ButtonBase {

    /**
     * Builds a {@link ButtonStyle} to hand to a TextButton's {@code style:}
     * parameter. Parameter order matches the Dart stub.
     */
    public static ButtonStyle styleFrom(com.codename1.flutter.Color foregroundColor,
            com.codename1.flutter.Color backgroundColor, com.codename1.flutter.Color shadowColor,
            Double elevation, com.codename1.flutter.TextStyle textStyle,
            com.codename1.flutter.EdgeInsets padding, Object side, Object shape, Object alignment,
            Object tapTargetSize, Object visualDensity) {
        return ButtonStyle.styleFrom(foregroundColor, backgroundColor, shadowColor, elevation, textStyle,
                padding, side, shape, alignment, tapTargetSize, visualDensity);
    }

    /**
     * {@code TextButton.icon}: a button whose content is an icon followed by a
     * label. This milestone consumes the label as the button content (the
     * leading icon is used when no label is supplied); a later pass composes
     * both into a Row.
     */
    public static TextButton icon(com.codename1.flutter.Key key,
            dart.runtime.Funcs.VoidFunc0 onPressed, ButtonStyle style,
            com.codename1.flutter.Widget icon, com.codename1.flutter.Widget label) {
        TextButton b = new TextButton();
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
