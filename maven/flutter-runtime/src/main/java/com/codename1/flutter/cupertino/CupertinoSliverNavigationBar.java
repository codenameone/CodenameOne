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
import com.codename1.flutter.TextStyle;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.material.AppBar;

/**
 * The large-title iOS navigation bar used inside a scroll view — Flutter's
 * {@code CupertinoSliverNavigationBar}. The collapsing large-title behavior is
 * not modeled this pass; it composes a static {@link AppBar} using the large
 * title (or middle) as its title.
 */
public class CupertinoSliverNavigationBar extends StatelessWidget {

    private Widget largeTitle;
    private Widget leading;
    private Widget middle;
    private Widget trailing;
    private Color backgroundColor;

    public void largeTitle(Widget v) {
        this.largeTitle = v;
    }

    public void leading(Widget v) {
        this.leading = v;
    }

    private boolean automaticallyImplyLeading = true;

    public void automaticallyImplyLeading(boolean v) {
        this.automaticallyImplyLeading = v;
    }

    public void automaticallyImplyTitle(boolean v) {
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

    public void border(Object v) {
    }

    public void stretch(boolean v) {
    }

    @Override
    public Widget build(BuildContext context) {
        AppBar bar = new AppBar();
        Widget title = largeTitle != null ? largeTitle : middle;
        if (title != null) {
            bar.title(title);
        }
        // The iOS bar's own defaults, so the material AppBarTheme never reaches it. This
        // composes onto AppBar, which resolves an unset background and foreground through
        // that theme -- and the gallery gives every demo page a purple one, so an iOS bar
        // rendered as a purple material bar with a white back arrow where the reference
        // has a white bar and a large black title. Its sibling CupertinoNavigationBar
        // already sets these; this one did not.
        bar.backgroundColor(backgroundColor != null ? backgroundColor
                : new Color(BAR_BACKGROUND));
        bar.foregroundColor(new Color(BAR_FOREGROUND));
        // A large title is iOS's 34pt, and it is left aligned, not centred.
        TextStyle large = new TextStyle();
        large.fontSize(LARGE_TITLE_SIZE);
        large.fontWeight(com.codename1.flutter.FontWeight.bold);
        bar.titleTextStyle(large);
        bar.centerTitle(false);
        bar.automaticallyImplyLeading(automaticallyImplyLeading);
        if (leading != null) {
            bar.leading(leading);
        }
        if (trailing != null) {
            dart.core.DartList<Widget> actions = new dart.core.DartList<Widget>();
            actions.add(trailing);
            bar.actions(actions);
        }
        return bar;
    }

    /** The iOS bar's own background, matching {@link CupertinoNavigationBar}. */
    private static final long BAR_BACKGROUND = 0xFFF9F9F9L;
    private static final long BAR_FOREGROUND = 0xFF000000L;

    /** iOS's large-title size. */
    private static final double LARGE_TITLE_SIZE = 34;
}
