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

/**
 * Flutter's Material 3 {@code SegmentedButton<T>}: outlined segments in a row; the
 * selected ones are filled with secondaryContainer and show a check. Tapping reports
 * the new selection set through {@code onSelectionChanged}: single selection replaces
 * it, {@code multiSelectionEnabled} toggles, and {@code emptySelectionAllowed} decides
 * whether the last selected segment can be cleared.
 */
public class SegmentedButton<T> extends StatelessWidget {
    private DartList<?> segments = DartList.of();
    private java.util.Set<Object> selected = new java.util.LinkedHashSet<Object>();
    private Funcs.VoidFunc1<Object> onSelectionChanged;
    private boolean multiSelectionEnabled;
    private boolean emptySelectionAllowed;
    private boolean showSelectedIcon = true;
    @SuppressWarnings("unchecked")
    public void segments(DartList<?> v) { this.segments = v == null ? DartList.of() : v; }
    @SuppressWarnings("unchecked")
    public void selected(Object v) {
        selected = new java.util.LinkedHashSet<Object>();
        if (v instanceof Iterable) {
            for (Object o : (Iterable<Object>) v) {
                selected.add(o);
            }
        }
    }
    public void onSelectionChanged(Funcs.VoidFunc1<Object> v) { this.onSelectionChanged = v; }
    public void multiSelectionEnabled(boolean v) { this.multiSelectionEnabled = v; }
    public void emptySelectionAllowed(boolean v) { this.emptySelectionAllowed = v; }
    public void showSelectedIcon(boolean v) { this.showSelectedIcon = v; }
    public void style(Object v) { }
    public void selectedIcon(Object v) { }
    @Override
    public Widget build(BuildContext context) {
        ColorScheme cs = Theme.of(context).colorScheme();
        DartList<Widget> cells = DartList.of();
        for (Object o : segments) {
            if (!(o instanceof ButtonSegment)) {
                continue;
            }
            final ButtonSegment<?> seg = (ButtonSegment<?>) o;
            boolean on = selected.contains(seg.getValue());
            Widget icon = on && showSelectedIcon
                    ? new com.codename1.flutter.widgets.Icon(com.codename1.flutter.Icons.check()) : seg.getIcon();
            com.codename1.flutter.widgets.DefaultTextStyle dts = new com.codename1.flutter.widgets.DefaultTextStyle();
            com.codename1.flutter.TextStyle ts = new com.codename1.flutter.TextStyle();
            ts.color(on ? cs.onSecondaryContainer() : cs.onSurface());
            dts.style(ts);
            dts.child(Kit.row(com.codename1.flutter.MainAxisAlignment.center, icon,
                    icon != null && seg.getLabel() != null ? Kit.sized(8.0, null, null) : null, seg.getLabel()));
            Widget cell = Kit.box(on ? cs.secondaryContainer() : null, 0, cs.outline(), 1,
                    Kit.pad(12, 10, dts));
            final Funcs.VoidFunc1<Object> cb = onSelectionChanged;
            cells.add(Kit.expanded(1, Kit.tap(cb == null || !seg.isEnabled() ? null : new Funcs.VoidFunc0() {
                @Override
                public void call() {
                    java.util.Set<Object> next = new java.util.LinkedHashSet<Object>(
                            multiSelectionEnabled ? selected : java.util.Collections.emptySet());
                    if (selected.contains(seg.getValue())) {
                        if (multiSelectionEnabled) {
                            next.remove(seg.getValue());
                        } else if (emptySelectionAllowed) {
                            next.clear();
                        } else {
                            return;
                        }
                        if (next.isEmpty() && !emptySelectionAllowed) {
                            return;
                        }
                    } else {
                        next.add(seg.getValue());
                    }
                    cb.call(dart.core.DartSet.from(next));
                }
            }, cell)));
        }
        com.codename1.flutter.widgets.Row row = new com.codename1.flutter.widgets.Row();
        row.children(cells);
        com.codename1.flutter.widgets.IntrinsicHeight ih = new com.codename1.flutter.widgets.IntrinsicHeight();
        ih.child(row);
        com.codename1.flutter.widgets.ClipRRect clip = new com.codename1.flutter.widgets.ClipRRect();
        clip.borderRadius(com.codename1.flutter.BorderRadius.circular(20));
        clip.child(ih);
        return Kit.box(null, 20, cs.outline(), 1, clip);
    }
}
