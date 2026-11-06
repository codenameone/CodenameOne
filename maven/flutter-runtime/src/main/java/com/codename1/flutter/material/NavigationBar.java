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
 * Flutter's Material 3 {@code NavigationBar}: destinations in a row, the selected one's
 * icon on a pill-shaped indicator, labels under the icons. Colours, height and label
 * behaviour come from the widget, else {@code ThemeData.navigationBarTheme}, else the
 * M3 defaults (surfaceContainer, secondaryContainer, 80).
 */
public class NavigationBar extends StatelessWidget {
    private DartList<?> destinations;
    private long selectedIndex;
    private Funcs.VoidFunc1<Long> onDestinationSelected;
    private Color backgroundColor;
    private Color indicatorColor;
    private Double height;
    private Object labelBehavior;

    public void destinations(DartList<?> v) { this.destinations = v; }
    public void selectedIndex(long v) { this.selectedIndex = v; }
    public void onDestinationSelected(Funcs.VoidFunc1<Long> v) { this.onDestinationSelected = v; }
    public void backgroundColor(Color v) { this.backgroundColor = v; }
    public void indicatorColor(Color v) { this.indicatorColor = v; }
    public void height(Double v) { this.height = v; }
    public void labelBehavior(Object v) { this.labelBehavior = v; }
    public void elevation(Double v) { }
    public void surfaceTintColor(Object v) { }
    public void shadowColor(Object v) { }
    public void animationDuration(Object v) { }
    public void indicatorShape(Object v) { }

    private static Widget center(Widget w) {
        com.codename1.flutter.widgets.Center c = new com.codename1.flutter.widgets.Center();
        c.child(w);
        return c;
    }

    @Override
    public Widget build(BuildContext context) {
        ColorScheme cs = Theme.of(context).colorScheme();
        NavigationBarThemeData t = Theme.of(context).navigationBarTheme();
        Color bg = backgroundColor != null ? backgroundColor
                : (t != null && t.getBackgroundColor() != null ? t.getBackgroundColor() : cs.surfaceContainer());
        Color ind = indicatorColor != null ? indicatorColor
                : (t != null && t.getIndicatorColor() != null ? t.getIndicatorColor() : cs.secondaryContainer());
        double h = height != null ? height.doubleValue()
                : (t != null && t.getHeight() != null ? t.getHeight().doubleValue() : 80);
        Object behavior = labelBehavior != null ? labelBehavior : (t == null ? null : t.getLabelBehavior());
        String b = behavior == null ? "alwaysShow" : behavior.toString();
        DartList<Widget> cells = DartList.of();
        int i = 0;
        if (destinations != null) {
            for (Object o : destinations) {
                final int index = i++;
                boolean sel = index == selectedIndex;
                Widget icon = (Widget) o;
                String label = null;
                if (o instanceof NavigationDestination) {
                    NavigationDestination d = (NavigationDestination) o;
                    icon = sel && d.getSelectedIcon() != null ? d.getSelectedIcon() : d.getIcon();
                    label = d.getLabel();
                }
                Widget pill = Kit.box(sel ? ind : null, 16, null, 0, Kit.pad(20, 4, icon));
                boolean showLabel = label != null && (b.endsWith("alwaysShow") || (sel && b.endsWith("onlyShowSelected")));
                com.codename1.flutter.TextStyle ls = new com.codename1.flutter.TextStyle();
                ls.fontSize(12);
                ls.color(sel ? cs.onSurface() : cs.onSurfaceVariant());
                Widget cell = Kit.col(null, pill, showLabel ? Kit.pad(0, 4, Kit.text(label, ls)) : null);
                final Funcs.VoidFunc1<Long> cb = onDestinationSelected;
                cells.add(Kit.expanded(1, Kit.tap(cb == null ? null : new Funcs.VoidFunc0() {
                    @Override
                    public void call() {
                        cb.call(Long.valueOf(index));
                    }
                }, center(cell))));
            }
        }
        com.codename1.flutter.widgets.Row row = new com.codename1.flutter.widgets.Row();
        row.children(cells);
        com.codename1.flutter.widgets.SafeArea sa = new com.codename1.flutter.widgets.SafeArea();
        sa.child(Kit.sized(null, h, row));
        return Kit.box(bg, 0, null, 0, sa);
    }
}
