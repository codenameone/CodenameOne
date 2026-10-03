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

import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Text;
import com.codename1.ui.Container;
import com.codename1.ui.list.DefaultListModel;
import com.codename1.ui.spinner.Spinner3D;

import dart.core.DartList;

/**
 * {@link CupertinoPicker}: a wheel of the strings its children carry.
 *
 * <p>The children are widgets, and Flutter draws each one. This draws the TEXT in each,
 * because a Codename One spinner is a list of strings -- which covers what a picker is
 * for and what the gallery asks of it, a column of day names. A child with no text in it
 * contributes an empty row rather than disappearing, so the wheel keeps its indices and
 * {@code onSelectedItemChanged} still reports the right one.</p>
 */
class CupertinoPickerRenderElement extends CupertinoWheelRenderElement {

    CupertinoPickerRenderElement(Widget widget) {
        super(widget);
    }

    private CupertinoPicker picker() {
        return (CupertinoPicker) widget();
    }

    @Override
    protected Container createWheel() {
        DartList<Widget> kids = picker().getChildren();
        int n = kids == null ? 0 : kids.size();
        String[] rows = new String[Math.max(1, n)];
        for (int iter = 0; iter < n; iter++) {
            rows[iter] = labelOf(kids.get(iter));
        }
        if (n == 0) {
            rows[0] = "";
        }
        return new Spinner3D(new DefaultListModel<String>(rows));
    }

    /// The text inside a child, looking through one level of wrapping, or "".
    private static String labelOf(Widget w) {
        Widget cur = w;
        for (int depth = 0; depth < 6 && cur != null; depth++) {
            if (cur instanceof Text) {
                String d = ((Text) cur).getData();
                return d == null ? "" : d;
            }
            Widget next = com.codename1.flutter.WidgetPreview.step(cur, null);
            if (next == null) {
                return "";
            }
            cur = next;
        }
        return "";
    }

    @Override
    protected void report(Object value) {
        dart.runtime.Funcs.VoidFunc1<Long> f = picker().getOnSelectedItemChanged();
        if (f == null) {
            return;
        }
        com.codename1.ui.Component c = component();
        if (c instanceof Spinner3D) {
            f.call(Long.valueOf(((Spinner3D) c).getSelectedIndex()));
        }
    }
}
