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
import com.codename1.flutter.MainAxisAlignment;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.TextStyle;
import com.codename1.flutter.Widget;
import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.EdgeInsetsGeometry;
import com.codename1.flutter.widgets.ColoredBox;
import com.codename1.flutter.widgets.Column;
import com.codename1.flutter.widgets.DefaultTextStyle;
import com.codename1.flutter.widgets.Expanded;
import com.codename1.flutter.widgets.Padding;
import com.codename1.flutter.widgets.Row;
import com.codename1.flutter.widgets.SingleChildScrollView;
import com.codename1.flutter.widgets.SizedBox;

import dart.core.DartList;
import dart.runtime.Funcs;

/**
 * A horizontal row of tabs — Flutter's {@code TabBar}. Renders the {@code tabs}
 * as an evenly-spaced (or, when {@code isScrollable}, horizontally scrollable)
 * {@link Row}, each stacked over a bottom indicator strip -- Flutter's default
 * {@code UnderlineTabIndicator} at {@code TabBarIndicatorSize.tab}, painted
 * only under the selected tab. Gesture-driven tab switching via the
 * {@link TabController} is deferred; selecting a tab still only happens
 * through the controller changing out from under a rebuild.
 */
public class TabBar extends StatelessWidget {

    private DartList<Widget> tabs;
    private TabController controller;
    private boolean isScrollable;
    private Color labelColor;
    private Color unselectedLabelColor;
    private TextStyle labelStyle;
    private EdgeInsetsGeometry labelPadding;
    private TextStyle unselectedLabelStyle;
    private Color indicatorColor;
    private Double indicatorWeight;
    private Funcs.VoidFunc1<Long> onTap;

    /** Flutter's default {@code TabBar.indicatorWeight}: 2 logical pixels. */
    private static final double DEFAULT_INDICATOR_WEIGHT_LP = 2.0;

    public void tabs(DartList<Widget> v) {
        this.tabs = v;
    }

    public void controller(TabController v) {
        this.controller = v;
    }

    public void isScrollable(boolean v) {
        this.isScrollable = v;
    }

    public void indicatorColor(Color v) {
        this.indicatorColor = v;
    }

    public void indicatorWeight(double v) {
        this.indicatorWeight = v;
    }

    public void indicatorPadding(Object v) {
    }

    public void indicator(Object v) {
    }

    public void indicatorSize(Object v) {
    }

    public void labelColor(Color v) {
        this.labelColor = v;
    }

    public void labelStyle(TextStyle v) {
        this.labelStyle = v;
    }

    public void labelPadding(Object v) {
        this.labelPadding = v instanceof EdgeInsetsGeometry ? (EdgeInsetsGeometry) v : null;
    }

    public void unselectedLabelColor(Color v) {
        this.unselectedLabelColor = v;
    }

    public void unselectedLabelStyle(TextStyle v) {
        this.unselectedLabelStyle = v;
    }

    public void padding(Object v) {
    }

    public void dragStartBehavior(Object v) {
    }

    public void mouseCursor(Object v) {
    }

    public void enableFeedback(boolean v) {
    }

    public void physics(Object v) {
    }

    public void onTap(Funcs.VoidFunc1<Long> v) {
        this.onTap = v;
    }

    /** Flutter's {@code _kTabHeight}: the height of a text-only tab bar. */
    public static final double TAB_HEIGHT_LP = 46;

    @Override
    public Widget build(BuildContext context) {
        Row row = new Row();
        // A scrollable bar packs its tabs from the start and lets the row
        // overflow; a fixed one shares the width between them.
        row.mainAxisAlignment(isScrollable
                ? MainAxisAlignment.start : MainAxisAlignment.spaceBetween);
        row.children(styledTabs(context));
        Widget content = row;
        if (isScrollable) {
            SingleChildScrollView sv = new SingleChildScrollView();
            sv.scrollDirection(com.codename1.flutter.Axis.horizontal);
            sv.child(row);
            content = sv;
        }
        // A tab bar has a FIXED height — Flutter declares it as a
        // PreferredSizeWidget for exactly this reason. Without it a scrollable
        // bar is greedy: offered the whole app bar it took all of it, leaving
        // the toolbar row zero pixels tall, so the colors demo lost its title
        // and stacked its tab strip over the top of the bar.
        com.codename1.flutter.widgets.SizedBox box =
                new com.codename1.flutter.widgets.SizedBox();
        box.height(TAB_HEIGHT_LP);
        box.child(content);
        return box;
    }

    /** Flutter's {@code kTabLabelPadding}, used when the bar names none. */
    private static final double DEFAULT_LABEL_H_PADDING_LP = 16;

    /**
     * The tabs, each padded, coloured and indicator-stacked the way Flutter's
     * TabBar does it.
     *
     * <p>Flutter pads every tab with {@code labelPadding} and styles it through
     * the surrounding text style: {@code labelStyle}/{@code labelColor} for the
     * selected tab and {@code unselectedLabelStyle}/{@code unselectedLabelColor}
     * for the rest. Dropping the padding ran the labels together -- Crane's
     * three tabs rendered as one word, "FLYSLEEPEAT" -- and dropping the colour
     * painted them in the default ink on a bar whose whole point was that they
     * are white.</p>
     */
    private DartList<Widget> styledTabs(BuildContext context) {
        DartList<Widget> out = new DartList<Widget>();
        if (tabs == null) {
            return out;
        }
        long selected = controller == null ? 0 : controller.index();
        EdgeInsetsGeometry pad = labelPadding != null ? labelPadding
                : EdgeInsets.symmetric(DEFAULT_LABEL_H_PADDING_LP, 0);
        // TabBar's real resolution (tab_bar.dart _TabBarState): a WIDGET-stated
        // labelColor with no unselectedLabelColor of its own fades to it at
        // alpha 0xB2 (~70%) -- the legacy shortcut kept for callers that only
        // ever set one colour, which is exactly what the gallery's colors demo
        // does (labelColor: colorScheme.onPrimary, no unselectedLabelColor).
        // An explicit unselectedLabelColor always wins outright.
        Color unselectedInk = unselectedLabelColor != null ? unselectedLabelColor
                : (labelColor != null ? labelColor.withAlpha(0xB2) : null);
        Color resolvedIndicatorColor = resolvedIndicatorColor(context);
        double weight = indicatorWeight != null ? indicatorWeight : DEFAULT_INDICATOR_WEIGHT_LP;
        for (int i = 0; i < tabs.size(); i++) {
            Widget tab = tabs.get(i);
            if (tab == null) {
                continue;
            }
            boolean isSelected = i == selected;
            TextStyle base = isSelected ? labelStyle
                    : (unselectedLabelStyle != null ? unselectedLabelStyle : labelStyle);
            Color ink = isSelected ? labelColor : unselectedInk;
            Widget styled = tab;
            if (base != null || ink != null) {
                TextStyle style = new TextStyle();
                if (base != null) {
                    style = style.merge(base);
                }
                if (ink != null) {
                    style.color(ink);
                }
                styled = DefaultTextStyle.wrap(style, tab);
            }
            Padding p = new Padding();
            p.padding(pad);
            p.child(styled);
            out.add(indicated(p, isSelected, resolvedIndicatorColor, weight));
        }
        return out;
    }

    /**
     * {@code indicatorColor}: the bar's own, then {@code ThemeData.indicatorColor}
     * -- Flutter also checks an ambient {@code TabBarTheme} between the two,
     * which this runtime does not yet thread onto {@link ThemeData} (its
     * constructor still holds {@code tabBarTheme} opaquely).
     */
    private Color resolvedIndicatorColor(BuildContext context) {
        if (indicatorColor != null) {
            return indicatorColor;
        }
        try {
            return Theme.of(context).indicatorColor();
        } catch (Throwable noTheme) {
            return new Color(0xFFFFFFFFL);
        }
    }

    /**
     * Stacks {@code label} over a bottom indicator strip -- Flutter's default
     * {@code UnderlineTabIndicator} at {@code TabBarIndicatorSize.tab}: a
     * {@code weight}-tall bar the full width of the tab's own cell, painted
     * only for the selected tab. An unselected tab still reserves the SAME
     * strip height, transparent, so the row's baseline does not jump between
     * tabs. {@link Expanded} absorbs the label's share of the column so the
     * whole stack still fits the bar's fixed {@link #TAB_HEIGHT_LP}, the way
     * Flutter paints the indicator inside its own PreferredSize rather than
     * growing the bar by it.
     */
    private Widget indicated(Widget label, boolean isSelected, Color indicatorColor, double weight) {
        Column col = new Column();
        col.crossAxisAlignment(CrossAxisAlignment.stretch);
        Expanded content = new Expanded();
        content.child(label);
        SizedBox bar = new SizedBox();
        bar.height(weight);
        if (isSelected) {
            ColoredBox cb = new ColoredBox();
            cb.color(indicatorColor);
            bar.child(cb);
        }
        DartList<Widget> kids = new DartList<Widget>();
        kids.add(content);
        kids.add(bar);
        col.children(kids);
        return col;
    }
}
