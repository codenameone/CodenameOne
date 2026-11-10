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
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Kit;
import dart.core.DartList;
import dart.runtime.Funcs;
import com.codename1.flutter.Color;
/**
 * Flutter's {@code InputDecorator}: a child framed the way a text field is -- the label
 * above, an underline (or the decoration's border) around, the helper or error text
 * below. The decoration's fill and the theme's {@code inputDecorationTheme} apply.
 */
public class InputDecorator extends StatelessWidget {
    private InputDecoration decoration;
    private Widget child;
    private boolean isEmpty;
    private boolean isFocused;
    private String errorOverride;

    public void decoration(InputDecoration v) { this.decoration = v; }
    public void child(Widget v) { this.child = v; }
    public void isEmpty(boolean v) { this.isEmpty = v; }
    public void isFocused(boolean v) { this.isFocused = v; }
    public void isHovering(boolean v) { }
    public void expands(boolean v) { }
    public void baseStyle(Object v) { }
    public void textAlign(Object v) { }
    void errorOverride(String v) { this.errorOverride = v; }

    @Override
    public Widget build(BuildContext context) {
        ColorScheme cs = Theme.of(context).colorScheme();
        InputDecoration d = decoration;
        String label = d == null ? null : d.getLabelText();
        String hint = d == null ? null : d.getHintText();
        String helper = d == null ? null : d.getHelperText();
        String error = errorOverride != null ? errorOverride : (d == null ? null : d.getErrorText());
        Color accent = error != null ? cs.error() : (isFocused ? cs.primary() : cs.onSurfaceVariant());
        com.codename1.flutter.TextStyle small = new com.codename1.flutter.TextStyle();
        small.fontSize(12);
        small.color(accent);
        Widget body = child;
        if (isEmpty && hint != null) {
            com.codename1.flutter.TextStyle hs = new com.codename1.flutter.TextStyle();
            hs.color(cs.onSurfaceVariant());
            body = Kit.text(hint, hs);
        }
        boolean outlined = d != null && d.getBorder() instanceof com.codename1.flutter.OutlineInputBorder;
        InputDecorationThemeData theme = Theme.of(context).inputDecorationTheme();
        boolean filled = (d != null && d.isFilled()) || (theme != null && Boolean.TRUE.equals(Boolean.valueOf(theme.isFilled())));
        Color fill = d != null && d.getFillColor() != null ? d.getFillColor()
                : (theme != null && theme.getFillColor() != null ? theme.getFillColor() : cs.surfaceContainerHighest());
        Widget framed = Kit.pad(outlined || filled ? 12 : 0, 8,
                Kit.col(com.codename1.flutter.CrossAxisAlignment.stretch,
                        label == null ? null : Kit.text(label, small), body));
        if (outlined) {
            framed = Kit.box(filled ? fill : null, 4, error != null ? cs.error() : cs.outline(), 1, framed);
        } else {
            framed = Kit.col(com.codename1.flutter.CrossAxisAlignment.stretch,
                    filled ? Kit.box(fill, 0, null, 0, framed) : framed,
                    Kit.box(accent, 0, null, 0, Kit.sized(null, isFocused || error != null ? 2.0 : 1.0, null)));
        }
        String below = error != null ? error : helper;
        if (below == null) {
            return framed;
        }
        return Kit.col(com.codename1.flutter.CrossAxisAlignment.stretch, framed,
                Kit.pad(outlined ? 12 : 0, 4, Kit.text(below, small)));
    }
}
