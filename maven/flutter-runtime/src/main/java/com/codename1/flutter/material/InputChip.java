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
import com.codename1.flutter.Color;
import com.codename1.flutter.CrossAxisAlignment;
import com.codename1.flutter.MainAxisSize;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.TextStyle;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Row;
import com.codename1.flutter.widgets.SizedBox;

import dart.core.DartList;
import dart.runtime.Funcs;

/**
 * A chip representing a complex piece of information (a contact, tag, ...) that
 * can be selected, pressed or deleted — Flutter's {@code InputChip}. This
 * milestone renders {@code avatar} + {@code label} in a {@link Row}; the
 * selection/press/delete interactions are deferred.
 */
public class InputChip extends StatelessWidget {

    private Widget avatar;
    private Widget label;
    private Widget deleteIcon;
    private boolean selected;
    private Color backgroundColor;
    private Color deleteIconColor;
    private TextStyle labelStyle;
    private Funcs.VoidFunc1<Boolean> onSelected;
    private Funcs.VoidFunc0 onPressed;
    private Funcs.VoidFunc0 onDeleted;

    public void avatar(Widget v) {
        this.avatar = v;
    }

    public void label(Widget v) {
        this.label = v;
    }

    public void labelStyle(TextStyle v) {
        this.labelStyle = v;
    }

    public void labelPadding(Object v) {
    }

    public void selected(boolean v) {
        this.selected = v;
    }

    public void isEnabled(boolean v) {
    }

    public void onSelected(Funcs.VoidFunc1<Boolean> v) {
        this.onSelected = v;
    }

    public void deleteIcon(Widget v) {
        this.deleteIcon = v;
    }

    public void onDeleted(Funcs.VoidFunc0 v) {
        this.onDeleted = v;
    }

    public void deleteIconColor(Color v) {
        this.deleteIconColor = v;
    }

    public void onPressed(Funcs.VoidFunc0 v) {
        this.onPressed = v;
    }

    public void pressElevation(Object v) {
    }

    public void disabledColor(Color v) {
    }

    public void selectedColor(Color v) {
    }

    public void tooltip(Object v) {
    }

    public void side(Object v) {
    }

    public void shape(Object v) {
    }

    public void backgroundColor(Color v) {
        this.backgroundColor = v;
    }

    public void padding(Object v) {
    }

    public void elevation(double v) {
    }

    @Override
    public Widget build(BuildContext context) {
        DartList<Widget> kids = new DartList<Widget>();
        if (avatar != null) {
            kids.add(avatar);
        }
        if (label != null) {
            kids.add(label);
        }
        if (deleteIcon != null) {
            kids.add(deleteIcon);
        }
        if (kids.size() == 0) {
            return new SizedBox();
        }
        Row row = new Row();
        row.mainAxisSize(MainAxisSize.min);
        row.crossAxisAlignment(CrossAxisAlignment.center);
        row.children(kids);
        return row;
    }
}
