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

/** Flutter's {@code CheckboxListTile}: a {@link ListTile} with a {@link Checkbox}. */
public class CheckboxListTile extends StatelessWidget {
    private Boolean value;
    private Funcs.VoidFunc1<Boolean> onChanged;
    private Widget title;
    private Widget subtitle;
    private Widget secondary;
    private Object controlAffinity;
    private boolean tristate;

    public void value(Boolean v) { this.value = v; }
    public void onChanged(Funcs.VoidFunc1<Boolean> v) { this.onChanged = v; }
    public void title(Widget v) { this.title = v; }
    public void subtitle(Widget v) { this.subtitle = v; }
    public void secondary(Widget v) { this.secondary = v; }
    public void controlAffinity(Object v) { this.controlAffinity = v; }
    public void tristate(boolean v) { this.tristate = v; }
    public void dense(boolean v) { }
    public void isThreeLine(boolean v) { }
    public void activeColor(Object v) { }
    public void checkColor(Object v) { }
    public void contentPadding(Object v) { }
    public void enabled(Boolean v) { }

    @Override
    public Widget build(BuildContext context) {
        Checkbox cb = new Checkbox();
        cb.value(value);
        cb.tristate(tristate);
        cb.onChanged(onChanged);
        ListTile tile = new ListTile();
        if (title != null) {
            tile.title(title);
        }
        if (subtitle != null) {
            tile.subtitle(subtitle);
        }
        // Flutter's default affinity is platform, which puts the box at the end.
        if (ListTileControlAffinity.isTrailing(controlAffinity, true)) {
            if (secondary != null) {
                tile.leading(secondary);
            }
            tile.trailing(cb);
        } else {
            tile.leading(cb);
            if (secondary != null) {
                tile.trailing(secondary);
            }
        }
        final Funcs.VoidFunc1<Boolean> cb2 = onChanged;
        final Boolean v = value;
        final boolean tri = tristate;
        if (cb2 != null) {
            // The whole tile toggles, as in Flutter: false -> true -> (null when tristate) -> false.
            tile.onTap(new Funcs.VoidFunc0() {
                @Override
                public void call() {
                    cb2.call(v == null ? Boolean.FALSE : v.booleanValue() ? (tri ? null : Boolean.FALSE) : Boolean.TRUE);
                }
            });
        }
        return tile;
    }
}
