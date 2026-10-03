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

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.TextStyle;
import com.codename1.flutter.material.AppBar;

import dart.core.DartList;

/**
 * The iOS top navigation bar — Flutter's {@code CupertinoNavigationBar}: a
 * centered middle title with optional leading/trailing widgets. Composed onto
 * the material {@link AppBar} with a centered title (visually approximate this
 * pass).
 */
public class CupertinoNavigationBar extends StatelessWidget {

    private Widget leading;
    private Widget middle;
    private Widget trailing;
    private Color backgroundColor;
    private boolean automaticallyImplyLeading = true;

    /**
     * The iOS bar's own defaults, so the material {@code AppBarTheme} never
     * decides how a Cupertino bar looks.
     *
     * <p>This bar composes onto the material {@link AppBar}, which resolves an
     * unset background and foreground through the ambient {@code AppBarTheme}.
     * Inside a {@code MaterialApp} that themes its bars purple-on-white — as the
     * gallery does — an unset Cupertino bar would inherit it and stop looking
     * like iOS at all. Naming both colours here keeps that resolution from ever
     * running.</p>
     */
    private static final long BAR_BACKGROUND = 0xFFF9F9F9L;
    private static final long BAR_FOREGROUND = 0xFF000000L;
    /** CupertinoTheme's navTitleTextStyle: 17pt semibold label. */
    private static final double TITLE_SIZE = 17;

    public void leading(Widget v) {
        this.leading = v;
    }

    public void automaticallyImplyLeading(boolean v) {
        this.automaticallyImplyLeading = v;
    }

    public void automaticallyImplyMiddle(boolean v) {
    }

    public void previousPageTitle(String v) {
    }

    public void middle(Widget v) {
        this.middle = v;
    }

    public void trailing(Widget v) {
        this.trailing = v;
    }

    public void backgroundColor(Color v) {
        this.backgroundColor = v;
    }

    public void brightness(Object v) {
    }

    public void padding(Object v) {
    }

    public void border(Object v) {
    }

    public void transitionBetweenRoutes(Object v) {
    }

    @Override
    public Widget build(BuildContext context) {
        AppBar bar = new AppBar();
        if (middle != null) {
            bar.title(middle);
        }
        bar.centerTitle(true);
        bar.backgroundColor(backgroundColor != null
                ? backgroundColor : new Color(BAR_BACKGROUND));
        bar.foregroundColor(new Color(BAR_FOREGROUND));
        TextStyle title = new TextStyle();
        title.fontSize(TITLE_SIZE);
        title.fontWeight(com.codename1.flutter.FontWeight.w600);
        bar.titleTextStyle(title);
        // leading and trailing used to be stored and never passed on, so an iOS
        // bar rendered its title and nothing else.
        bar.automaticallyImplyLeading(automaticallyImplyLeading);
        if (leading != null) {
            bar.leading(leading);
        }
        if (trailing != null) {
            DartList<Widget> actions = new DartList<Widget>();
            actions.add(trailing);
            bar.actions(actions);
        }
        return bar;
    }
}
