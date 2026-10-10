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
import com.codename1.flutter.widgets.Column;
import com.codename1.flutter.widgets.Expanded;
import com.codename1.flutter.widgets.Row;

import dart.core.DartList;

/**
 * A material banner: a prominent message with optional leading icon and action
 * buttons — Flutter's {@code MaterialBanner}. This milestone renders a
 * {@link Row} of the {@code leading} widget and {@code content}, with the
 * {@code actions} laid out in a trailing {@link Row} below.
 */
public class MaterialBanner extends StatelessWidget {

    private Widget content;
    private Widget leading;
    private DartList<Widget> actions;
    private TextStyle contentTextStyle;
    private Color backgroundColor;

    public void content(Widget v) {
        this.content = v;
    }

    public void contentTextStyle(TextStyle v) {
        this.contentTextStyle = v;
    }

    public void actions(DartList<Widget> v) {
        this.actions = v;
    }

    public void elevation(double v) {
    }

    public void leading(Widget v) {
        this.leading = v;
    }

    public void backgroundColor(Color v) {
        this.backgroundColor = v;
    }

    public void surfaceTintColor(Color v) {
    }

    public void shadowColor(Color v) {
    }

    public void dividerColor(Color v) {
    }

    public void padding(Object v) {
    }

    public void leadingPadding(Object v) {
    }

    public void forceActionsBelow(boolean v) {
    }

    public void overflowAlignment(Object v) {
    }

    public void animation(Object v) {
    }

    public void onVisible(Object v) {
    }

    @Override
    public Widget build(BuildContext context) {
        DartList<Widget> top = new DartList<Widget>();
        if (leading != null) {
            top.add(leading);
            // Material's gap between the leading icon and the message. Without
            // it the icon and the text were touching at the top-left corner.
            com.codename1.flutter.widgets.SizedBox gap =
                    new com.codename1.flutter.widgets.SizedBox();
            gap.width(LEADING_GAP_LP);
            top.add(gap);
        }
        if (content != null) {
            Expanded e = new Expanded();
            e.child(content);
            top.add(e);
        }
        Row topRow = new Row();
        topRow.crossAxisAlignment(CrossAxisAlignment.center);
        topRow.children(top);

        DartList<Widget> rows = new DartList<Widget>();
        rows.add(topRow);
        if (actions != null && actions.size() > 0) {
            Row actionRow = new Row();
            // Actions sit at the END of the banner, not the start. Laid out from
            // the left they read as two more links under the message rather than
            // as the banner's buttons.
            actionRow.mainAxisAlignment(com.codename1.flutter.MainAxisAlignment.end);
            actionRow.children(actions);
            rows.add(actionRow);
        }
        Column col = new Column();
        col.crossAxisAlignment(CrossAxisAlignment.stretch);
        col.mainAxisSize(MainAxisSize.min);
        col.children(rows);

        // The banner is INSET, and it closes with a rule. Built bare it was
        // packed hard into the top-left corner with nothing separating it from
        // the list underneath.
        com.codename1.flutter.widgets.Padding pad =
                new com.codename1.flutter.widgets.Padding();
        pad.padding(com.codename1.flutter.EdgeInsets.fromLTRB(
                PAD_LP, PAD_LP, PAD_LP, 0));
        pad.child(col);

        DartList<Widget> stackRows = new DartList<Widget>();
        stackRows.add(pad);
        stackRows.add(new Divider());
        Column outer = new Column();
        outer.crossAxisAlignment(CrossAxisAlignment.stretch);
        outer.mainAxisSize(MainAxisSize.min);
        outer.children(stackRows);
        return outer;
    }

    /** Material's banner inset, in logical pixels. */
    private static final double PAD_LP = 16;
    /** The gap between the leading icon and the message. */
    private static final double LEADING_GAP_LP = 16;
}
