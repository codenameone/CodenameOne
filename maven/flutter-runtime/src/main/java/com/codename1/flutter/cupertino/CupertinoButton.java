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

import com.codename1.flutter.BorderRadius;
import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.Key;
import com.codename1.flutter.RoundedRectangleBorder;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.material.ButtonBase;
import com.codename1.flutter.material.ButtonStyle;
import com.codename1.flutter.material.ElevatedButton;
import com.codename1.flutter.material.TextButton;

import dart.runtime.Funcs;

/**
 * An iOS-style button — Flutter's {@code CupertinoButton}. The default variant
 * is a borderless tinted-text button; {@code CupertinoButton.filled} has a
 * solid background. A null {@code onPressed} disables it. Composed onto the
 * material {@link TextButton} (default) / {@link ElevatedButton} (filled).
 *
 * <p>Color follows Flutter's {@code CupertinoButton.build}
 * (packages/flutter/lib/src/cupertino/button.dart): the unfilled button's
 * text is {@code CupertinoTheme.of(context).primaryColor} (the gallery's
 * cupertino-buttons demo sets no ancestor {@code CupertinoTheme}, so this
 * resolves to the plain default, {@code CupertinoColors.activeBlue} --
 * measured (0,122,255) against the reference, not the app's Material
 * primary). {@code .filled}'s background is that SAME primaryColor and its
 * foreground is {@code primaryContrastingColor} (white) -- in BOTH states,
 * enabled or disabled, since only the fill switches to {@code disabledColor}
 * once {@code onPressed} is null. Disabled and unfilled falls back to
 * {@code CupertinoColors.placeholderText} instead. Elevation is pinned to 0:
 * Flutter's CupertinoButton never casts a shadow, unlike the Material
 * {@link ElevatedButton} it is composed onto here.</p>
 */
public class CupertinoButton extends StatelessWidget {

    /** Flutter's default {@code CupertinoButton.borderRadius}: 8 logical pixels. */
    private static final double DEFAULT_RADIUS_LP = 8;

    private Funcs.VoidFunc0 onPressed;
    private Widget child;
    private boolean filled;
    private Color color;
    private Color disabledColor;

    public void onPressed(Funcs.VoidFunc0 v) {
        this.onPressed = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public void padding(Object v) {
    }

    /** An explicit fill (filled) / tint (unfilled) overriding the theme's primaryColor. */
    public void color(Color v) {
        this.color = v;
    }

    /** {@code CupertinoColors.quaternarySystemFill} unless the caller states otherwise. */
    public void disabledColor(Color v) {
        this.disabledColor = v;
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
        CupertinoThemeData theme = CupertinoTheme.of(context);
        boolean enabled = onPressed != null;
        ButtonBase b;
        ButtonStyle style;
        if (filled) {
            // CupertinoButton.filled forces its OWN `color` to null in Dart (the
            // factory sets `color = null, _filled = true`), so only disabledColor
            // is ever a caller override here.
            Color bg = enabled ? theme.primaryColor()
                    : (disabledColor != null ? disabledColor : CupertinoColors.quaternarySystemFill);
            b = new ElevatedButton();
            style = ElevatedButton.styleFrom(theme.primaryContrastingColor(), bg, null, 0.0, null,
                    null, null, filledShape(), null, null, null);
        } else {
            // An explicit `color` on the UNFILLED constructor would, in real
            // Flutter, also paint a background (backgroundColor resolves from
            // `color ?? (_filled ? primaryColor : null)` for either constructor) --
            // unexercised by this runtime's demos, so only the tint role is
            // modelled here.
            Color fg = enabled ? (color != null ? color : theme.primaryColor())
                    : CupertinoColors.placeholderText;
            b = new TextButton();
            style = TextButton.styleFrom(fg, null, null, 0.0, null, null, null, null, null, null, null);
        }
        b.style(style);
        b.onPressed(onPressed);
        b.child(child);
        return b;
    }

    /** {@code BorderRadius.circular(8)}, matching the un-stated {@code borderRadius}. */
    private RoundedRectangleBorder filledShape() {
        RoundedRectangleBorder shape = new RoundedRectangleBorder();
        shape.borderRadius(BorderRadius.circular(DEFAULT_RADIUS_LP));
        return shape;
    }
}
