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
    private boolean automaticBackgroundVisibility = true;

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
     *
     * <p>The background is Flutter's OWN {@code _kDefaultNavBarBackgroundColor}
     * (packages/flutter/lib/src/cupertino/nav_bar.dart): {@code 0xF0F9F9F9}, alpha
     * {@code 0xF0} (240/255, about 94%) -- a TRANSLUCENT fill over whatever sits
     * behind the bar, not an opaque one.
     * {@link com.codename1.flutter.material.ThemeDataAdapter#paintColor}
     * already carries a colour's alpha into {@code setBgTransparency}, so the only
     * change needed here was the constant: a fully opaque {@code 0xFFF9F9F9} painted
     * a flat (249,249,249) band that read as a visibly grey strip against the
     * gallery's white demo card, where Flutter's bar -- translucent over that same
     * white card -- composites to effectively (255,255,255), indistinguishable
     * from the content under it. Measured on the cupertino-buttons demo's "Buttons"
     * title bar: reference (255,255,255), this runtime (249,249,249) opaque.</p>
     */
    private static final long BAR_BACKGROUND = 0xF0F9F9F9L;
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

    public void automaticBackgroundVisibility(boolean v) {
        this.automaticBackgroundVisibility = v;
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
        bar.backgroundColor(resolveBackgroundColor(context));
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

    /**
     * Flutter's {@code automaticBackgroundVisibility} (default true,
     * packages/flutter/lib/src/cupertino/nav_bar.dart): inside a {@code
     * CupertinoPageScaffold}, the bar's background (and bottom border --
     * {@code _kTransparentNavBarBorder}, which this runtime has nothing to
     * suppress since it never paints a Cupertino border of its own, only the
     * composed {@link AppBar}'s) stays transparent until content scrolls
     * under it, instead of the stated/default {@code backgroundColor}
     * painting immediately.
     *
     * <p>Flutter tracks that scroll offset itself ({@code
     * _CupertinoNavigationBarState}'s scroll-notification listener, which
     * needs a {@code PrimaryScrollController} this runtime's {@code Scaffold}
     * composition does not wire up) and lerps the fill between the page's
     * own background colour and {@code backgroundColor} as it grows; this
     * runtime has no cheap way to observe that from here, so it only
     * reproduces the REST state -- fully transparent, matching the gallery's
     * reference screenshots, which never scroll content under a nav bar --
     * and stays that way rather than flipping opaque once a real scroll
     * happens. A real scroll-under re-opacifying the bar is NOT implemented.</p>
     *
     * <p>{@code automaticBackgroundVisibility} is true by default for either
     * the default or an explicit {@code backgroundColor} (Flutter applies
     * the SAME lerp regardless of which one it holds), and has no effect at
     * all outside a {@code CupertinoPageScaffold} -- exactly Flutter's own
     * rule -- which {@link #build} with a null {@code context} (every bare,
     * unmounted-widget test in this package) also satisfies, since there is
     * then no ancestor to find.</p>
     */
    private Color resolveBackgroundColor(BuildContext context) {
        Color resolved = backgroundColor != null ? backgroundColor : new Color(BAR_BACKGROUND);
        if (!automaticBackgroundVisibility || context == null) {
            return resolved;
        }
        if (context.findAncestorWidgetOfExactType(CupertinoPageScaffold.class) == null) {
            return resolved;
        }
        return new Color(0x00000000L);
    }
}
