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
 *
 * <p>Size follows the same source, {@code kCupertinoButtonPadding}/{@code
 * kCupertinoButtonSizeBorderRadius}/{@code kCupertinoButtonMinSize} (all keyed
 * by {@code sizeStyle}, packages/flutter/lib/src/cupertino/constants.dart) --
 * this runtime never states a {@code sizeStyle}, matching Flutter's own
 * un-stated default of {@code CupertinoButtonSize.large} -- and the label's
 * font is {@code CupertinoTheme.of(context).textTheme.actionTextStyle} (17
 * logical pixels) -- composed onto the Material button through {@link
 * ButtonStyle}'s padding/textStyle/minimumSize, which a plain Material
 * button never states and so never sees.</p>
 */
public class CupertinoButton extends StatelessWidget {

    /**
     * {@code kCupertinoButtonSizeBorderRadius[CupertinoButtonSize.large]}:
     * {@code BorderRadius.circular(12)}. Flutter 3.35 replaced the single
     * {@code borderRadius: 8} default with this per-size map; large is the
     * un-stated {@code sizeStyle} default both constructors fall back to.
     */
    private static final double DEFAULT_RADIUS_LP = 12;

    /**
     * {@code kCupertinoButtonPadding[CupertinoButtonSize.large]}:
     * {@code EdgeInsets.symmetric(vertical: 16, horizontal: 20)}. Flutter 3.35
     * replaced the separate {@code _kButtonPadding} (all 16, unfilled) /
     * {@code _kBackgroundButtonPadding} (64 horizontal, 14 vertical, filled)
     * constants with this ONE per-size map, read the same way regardless of
     * {@code _style} (plain/tinted/filled) -- so the filled and unfilled forms
     * now share the exact same default padding.
     */
    private static final double PADDING_H_LP = 20;
    private static final double PADDING_V_LP = 16;
    /**
     * {@code kCupertinoButtonMinSize[CupertinoButtonSize.large]}: 44, the same
     * value {@code kMinInteractiveDimensionCupertino} used to supply directly
     * (that constant is now only the map's own fallback for a null entry, per
     * {@code _CupertinoButtonState.build}'s {@code ConstrainedBox}).
     */
    private static final double MIN_INTERACTIVE_LP = 44;

    private Funcs.VoidFunc0 onPressed;
    private Widget child;
    private boolean filled;
    private Color color;
    private Color disabledColor;
    private com.codename1.flutter.EdgeInsetsGeometry padding;
    private Double minSize;

    public void onPressed(Funcs.VoidFunc0 v) {
        this.onPressed = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public void padding(Object v) {
        this.padding = v instanceof com.codename1.flutter.EdgeInsetsGeometry
                ? (com.codename1.flutter.EdgeInsetsGeometry) v : null;
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
        this.minSize = v;
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
        // CupertinoButton.build: Text(..., style: CupertinoTheme.of(context)
        // .textTheme.actionTextStyle). Composed through ButtonStyle.textStyle, which
        // ButtonRenderElement.applyLabelFont reads -- every OTHER button's style
        // states none, so this is the only path that ever sees it.
        com.codename1.flutter.TextStyle actionStyle = theme.textTheme().actionTextStyle();
        ButtonBase b;
        ButtonStyle style;
        if (filled) {
            // CupertinoButton.filled forces its OWN `color` to null in Dart (the
            // factory sets `color = null, _filled = true`), so only disabledColor
            // is ever a caller override here.
            Color bg = enabled ? theme.primaryColor()
                    : (disabledColor != null ? disabledColor : CupertinoColors.quaternarySystemFill);
            b = new ElevatedButton();
            style = ElevatedButton.styleFrom(theme.primaryContrastingColor(), bg, null, 0.0,
                    actionStyle, filledPadding(), null, filledShape(), null, null, null);
        } else {
            // An explicit `color` on the UNFILLED constructor would, in real
            // Flutter, also paint a background (backgroundColor resolves from
            // `color ?? (_filled ? primaryColor : null)` for either constructor) --
            // unexercised by this runtime's demos, so only the tint role is
            // modelled here.
            Color fg = enabled ? (color != null ? color : theme.primaryColor())
                    : CupertinoColors.placeholderText;
            b = new TextButton();
            style = TextButton.styleFrom(fg, null, null, 0.0, actionStyle, plainPadding(),
                    null, null, null, null, null);
        }
        // kMinInteractiveDimensionCupertino: not a styleFrom() parameter (Flutter's
        // minimumSize is per-axis and nothing else in this runtime needs one), so it
        // goes directly on the built style -- ButtonRenderElement.performLayout reads it.
        style.minimumSize(minSize != null ? minSize : MIN_INTERACTIVE_LP);
        b.style(style);
        b.onPressed(onPressed);
        b.child(child);
        return b;
    }

    /**
     * The caller's own padding when it is a plain (non-directional) one, or
     * {@code kCupertinoButtonPadding[CupertinoButtonSize.large]}. Shared with
     * {@link #filledPadding()} -- Flutter's own map does not vary by {@code
     * _style} -- kept as two methods only because the unfilled/filled
     * branches in {@link #build} already read two differently-named ones.
     */
    private com.codename1.flutter.EdgeInsets plainPadding() {
        return padding instanceof com.codename1.flutter.EdgeInsets
                ? (com.codename1.flutter.EdgeInsets) padding
                : com.codename1.flutter.EdgeInsets.symmetric(PADDING_H_LP, PADDING_V_LP);
    }

    /** The caller's own padding when stated, or {@code kCupertinoButtonPadding[CupertinoButtonSize.large]}. */
    private com.codename1.flutter.EdgeInsets filledPadding() {
        return padding instanceof com.codename1.flutter.EdgeInsets
                ? (com.codename1.flutter.EdgeInsets) padding
                : com.codename1.flutter.EdgeInsets.symmetric(PADDING_H_LP, PADDING_V_LP);
    }

    /**
     * {@code BorderRadius.circular(12)}, matching the un-stated {@code borderRadius}
     * ({@code kCupertinoButtonSizeBorderRadius[CupertinoButtonSize.large]}).
     */
    private RoundedRectangleBorder filledShape() {
        RoundedRectangleBorder shape = new RoundedRectangleBorder();
        shape.borderRadius(BorderRadius.circular(DEFAULT_RADIUS_LP));
        return shape;
    }
}
