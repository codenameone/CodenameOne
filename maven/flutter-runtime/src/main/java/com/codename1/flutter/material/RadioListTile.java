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

import dart.runtime.Funcs;

/**
 * A {@link ListTile} whose trailing (or leading) control is a {@link Radio} —
 * Flutter's {@code RadioListTile<T>}. Tapping anywhere on the row selects the
 * radio, firing {@code onChanged(value)}; grouping is by value equality against
 * {@code groupValue} (see {@link Radio}). Composed as a ListTile hosting the
 * radio.
 */
public class RadioListTile<T> extends StatelessWidget {

    private Object value;
    private Object groupValue;
    private Funcs.VoidFunc1<T> onChanged;
    private Widget title;
    private Widget subtitle;
    private Widget secondary;

    public void value(Object v) {
        this.value = v;
    }

    public void groupValue(Object v) {
        this.groupValue = v;
    }

    public void onChanged(Funcs.VoidFunc1<T> v) {
        this.onChanged = v;
    }

    public void title(Widget v) {
        this.title = v;
    }

    public void subtitle(Widget v) {
        this.subtitle = v;
    }

    public void secondary(Widget v) {
        this.secondary = v;
    }

    public void isThreeLine(boolean v) {
    }

    public void selected(boolean v) {
    }

    public void dense(boolean v) {
    }

    private Object controlAffinity;

    public void controlAffinity(Object v) {
        this.controlAffinity = v;
    }

    public void activeColor(Object v) {
    }

    public void contentPadding(Object v) {
    }

    @Override
    public Widget build(BuildContext context) {
        Radio radio = new Radio();
        radio.value(value);
        radio.groupValue(groupValue);
        radio.onChanged(onChanged);

        ListTile tile = new ListTile();
        if (title != null) {
            tile.title(title);
        }
        if (subtitle != null) {
            tile.subtitle(subtitle);
        }
        // Flutter puts a radio on the LEADING edge by default —
        // ListTileControlAffinity.platform resolves to leading for radios and
        // checkboxes (only a switch trails). Putting it on the trailing edge
        // mirrored every settings list in the app.
        if (ListTileControlAffinity.isTrailing(controlAffinity, false)) {
            if (secondary != null) {
                tile.leading(secondary);
            }
            tile.trailing(radio);
        } else {
            tile.leading(radio);
            if (secondary != null) {
                tile.trailing(secondary);
            }
        }
        return tile;
    }
}
