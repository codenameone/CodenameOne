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
 * Flutter's {@code DropdownButton<T>}: the selected item's child and an arrow; tapping
 * opens the items as a menu and reports the chosen value through {@code onChanged}.
 * A null {@code onChanged} (or no items) disables it and shows {@code disabledHint}.
 */
public class DropdownButton<T> extends StatelessWidget {
    protected DartList<?> items;
    protected T value;
    protected Funcs.VoidFunc1<Object> onChanged;
    protected Widget hint;
    protected Widget disabledHint;
    protected Widget icon;
    protected Widget underline;
    protected boolean isExpanded;
    protected com.codename1.flutter.TextStyle style;
    protected Color dropdownColor;

    public void items(DartList<?> v) { this.items = v; }
    public void value(T v) { this.value = v; }
    public void onChanged(Funcs.VoidFunc1<Object> v) { this.onChanged = v; }
    public void hint(Widget v) { this.hint = v; }
    public void disabledHint(Widget v) { this.disabledHint = v; }
    public void icon(Widget v) { this.icon = v; }
    public void underline(Widget v) { this.underline = v; }
    public void isExpanded(boolean v) { this.isExpanded = v; }
    public void style(com.codename1.flutter.TextStyle v) { this.style = v; }
    public void dropdownColor(Color v) { this.dropdownColor = v; }
    public void isDense(boolean v) { }
    public void elevation(long v) { }
    public void iconSize(double v) { }
    public void borderRadius(Object v) { }
    public void menuMaxHeight(Double v) { }
    public void alignment(Object v) { }
    public void padding(Object v) { }
    public void focusNode(Object v) { }
    public void autofocus(boolean v) { }
    public void selectedItemBuilder(Object v) { }

    /** The shown value and the arrow, opening a menu of the items when enabled. */
    protected Widget field(BuildContext context, boolean drawUnderline) {
        ColorScheme cs = Theme.of(context).colorScheme();
        boolean enabled = onChanged != null && items != null && !items.isEmpty();
        Widget shown = enabled ? hint : (disabledHint != null ? disabledHint : hint);
        if (items != null) {
            for (Object o : items) {
                if (o instanceof DropdownMenuItem && value != null
                        && value.equals(((DropdownMenuItem<?>) o).getValue())) {
                    shown = ((DropdownMenuItem<?>) o).getChild();
                }
            }
        }
        if (shown == null) {
            shown = Kit.text("", null);
        }
        if (style != null) {
            com.codename1.flutter.widgets.DefaultTextStyle dts = new com.codename1.flutter.widgets.DefaultTextStyle();
            dts.style(style);
            dts.child(shown);
            shown = dts;
        }
        com.codename1.flutter.widgets.Icon arrow = new com.codename1.flutter.widgets.Icon(
                com.codename1.flutter.Icons.arrow_drop_down());
        arrow.color(enabled ? cs.onSurfaceVariant() : cs.onSurface().withOpacity(0.38));
        Widget body = Kit.row(isExpanded ? com.codename1.flutter.MainAxisAlignment.spaceBetween : null,
                isExpanded ? Kit.expanded(1, shown) : shown, icon != null ? icon : arrow);
        if (drawUnderline) {
            Widget line = underline != null ? underline
                    : Kit.box(cs.outline(), 0, null, 0, Kit.sized(null, 1.0, null));
            body = Kit.col(com.codename1.flutter.CrossAxisAlignment.stretch, body, line);
        }
        if (!enabled) {
            return body;
        }
        PopupMenuButton<Object> menu = new PopupMenuButton<Object>();
        final DartList<?> all = items;
        menu.itemBuilder(new Funcs.Func1<BuildContext, Object>() {
            @Override
            public Object call(BuildContext c) {
                DartList<Object> out = DartList.of();
                for (Object o : all) {
                    if (o instanceof DropdownMenuItem) {
                        DropdownMenuItem<?> it = (DropdownMenuItem<?>) o;
                        PopupMenuItem<Object> pm = new PopupMenuItem<Object>();
                        pm.value(it.getValue());
                        pm.child(it.getChild());
                        pm.enabled(it.isEnabled());
                        if (it.getOnTap() != null) {
                            pm.onTap(it.getOnTap());
                        }
                        out.add(pm);
                    }
                }
                return out;
            }
        });
        final Funcs.VoidFunc1<Object> changed = onChanged;
        menu.onSelected(new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object v) {
                changed.call(v);
            }
        });
        if (dropdownColor != null) {
            menu.color(dropdownColor);
        }
        menu.child(body);
        return menu;
    }

    @Override
    public Widget build(BuildContext context) {
        return field(context, !DropdownButtonHideUnderline.at(context));
    }
}
