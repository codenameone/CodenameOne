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

import com.codename1.flutter.Color;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Column;
import com.codename1.flutter.widgets.ColoredBox;
import com.codename1.flutter.widgets.Expanded;
import com.codename1.flutter.widgets.Row;
import com.codename1.flutter.widgets.SingleChildScrollView;
import com.codename1.flutter.widgets.SizedBox;

import dart.core.DartList;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TabBar's default indicator and unselected-label colour, per Flutter's own
 * {@code _TabBarState} (packages/flutter/lib/src/material/tab_bar.dart):
 * a widget-stated {@code labelColor} with no {@code unselectedLabelColor} of
 * its own fades to it at {@code withAlpha(0xB2)} (~70%); the indicator is a
 * {@code UnderlineTabIndicator} at {@code TabBarIndicatorSize.tab} --
 * {@code indicatorWeight} (default 2) logical pixels tall, the tab's full
 * width, in {@code indicatorColor} ({@code ThemeData.indicatorColor}'s
 * default is plain white) -- painted only under the selected tab. The
 * gallery's colors demo states {@code labelColor} alone and relies on both
 * defaults, which is exactly what left it with no visible indicator and
 * every label the same colour before this.
 */
class TabBarIndicatorTest {

    private static Row rowOf(TabBar bar) {
        Widget built = bar.build(null);
        assertTrue(built instanceof SizedBox);
        Widget inner = ((SizedBox) built).getChild();
        if (inner instanceof SingleChildScrollView) {
            inner = ((SingleChildScrollView) inner).getChild();
        }
        assertTrue(inner instanceof Row, "expected a Row of tabs, got " + inner);
        return (Row) inner;
    }

    private static TabBar barWithThreeTabs() {
        TabBar bar = new TabBar();
        DartList<Widget> tabs = new DartList<Widget>();
        for (int i = 0; i < 3; i++) {
            Tab t = new Tab();
            t.text("tab" + i);
            tabs.add(t);
        }
        bar.tabs(tabs);
        return bar;
    }

    /** The bottom indicator strip ({@code [1]} of the Column) for one tab cell. */
    private static SizedBox stripOf(Widget tabCell) {
        assertTrue(tabCell instanceof Column, "expected a Column tab cell, got " + tabCell);
        DartList<Widget> kids = ((Column) tabCell).getChildren();
        assertEquals(2, kids.size(), "expected [label, indicator strip]");
        assertTrue(kids.get(0) instanceof Expanded, "the label must share space with the strip");
        Widget strip = kids.get(1);
        assertTrue(strip instanceof SizedBox, "expected the indicator strip, got " + strip);
        return (SizedBox) strip;
    }

    // ------------------------------------------------------------------
    // Indicator
    // ------------------------------------------------------------------

    @Test
    void onlyTheSelectedTabPaintsAnIndicatorStrip() {
        TabBar bar = barWithThreeTabs();
        bar.indicatorColor(new Color(0xFFFFFFFFL));
        DartList<Widget> kids = rowOf(bar).getChildren();

        SizedBox selected = stripOf(kids.get(0));
        assertTrue(selected.getChild() instanceof ColoredBox, "tab 0 is selected: it paints the indicator");
        assertEquals(0xFFFFFFFFL, ((ColoredBox) selected.getChild()).getColor().value());

        for (int i = 1; i < kids.size(); i++) {
            SizedBox unselected = stripOf(kids.get(i));
            assertNull(unselected.getChild(), "tab " + i + " is NOT selected: no indicator paint");
        }
    }

    @Test
    void everyStripReservesTheSameHeightSoTheRowNeverJumps() {
        TabBar bar = barWithThreeTabs();
        bar.indicatorWeight(4.0);
        DartList<Widget> kids = rowOf(bar).getChildren();
        for (int i = 0; i < kids.size(); i++) {
            assertEquals(4.0, stripOf(kids.get(i)).getHeight(), 0.001,
                    "tab " + i + " must reserve the indicator's height whether selected or not");
        }
    }

    @Test
    void indicatorWeightDefaultsToTwoLogicalPixels() {
        DartList<Widget> kids = rowOf(barWithThreeTabs()).getChildren();
        assertEquals(2.0, stripOf(kids.get(0)).getHeight(), 0.001);
    }

    @Test
    void indicatorColorDefaultsToWhiteWhenNamedNowhere() {
        // No ThemeData ancestor at all (build(null)) still resolves a colour:
        // ThemeData's own default is plain white.
        TabBar bar = barWithThreeTabs();
        SizedBox selected = stripOf(rowOf(bar).getChildren().get(0));
        assertEquals(0xFFFFFFFFL, ((ColoredBox) selected.getChild()).getColor().value());
    }

    @Test
    void anExplicitIndicatorColorWinsOverTheThemeDefault() {
        TabBar bar = barWithThreeTabs();
        bar.indicatorColor(new Color(0xFF00FF00L));
        SizedBox selected = stripOf(rowOf(bar).getChildren().get(0));
        assertEquals(0xFF00FF00L, ((ColoredBox) selected.getChild()).getColor().value());
    }

    // ------------------------------------------------------------------
    // Unselected label colour
    // ------------------------------------------------------------------

    @Test
    void aWidgetStatedLabelColorFadesForTheUnselectedTabsByDefault() {
        TabBar bar = barWithThreeTabs();
        Color white = new Color(0xFFFFFFFFL);
        bar.labelColor(white);
        // No unselectedLabelColor stated: Flutter's TabBar fades labelColor
        // itself, withAlpha(0xB2), rather than leaving every tab the same
        // colour or falling back to a theme default.
        Color expectedUnselected = white.withAlpha(0xB2);

        DartList<Widget> kids = rowOf(bar).getChildren();
        assertEquals(expectedUnselected.value(), inkOf(kids.get(1)));
        assertEquals(expectedUnselected.value(), inkOf(kids.get(2)));
        assertEquals(white.value(), inkOf(kids.get(0)), "the selected tab keeps the full labelColor");
    }

    @Test
    void anExplicitUnselectedLabelColorStillWinsOverTheFade() {
        TabBar bar = barWithThreeTabs();
        bar.labelColor(new Color(0xFFFFFFFFL));
        Color rest = new Color(0xFF123456L);
        bar.unselectedLabelColor(rest);
        DartList<Widget> kids = rowOf(bar).getChildren();
        assertEquals(rest.value(), inkOf(kids.get(1)));
    }

    private static long inkOf(Widget tabCell) {
        assertTrue(tabCell instanceof Column);
        Widget content = ((Column) tabCell).getChildren().get(0);
        assertTrue(content instanceof Expanded);
        Widget padded = ((Expanded) content).getChild();
        Widget styled = ((com.codename1.flutter.widgets.Padding) padded).getChild();
        assertTrue(styled instanceof com.codename1.flutter.widgets.DefaultTextStyle,
                "the tab was not styled: " + styled);
        com.codename1.flutter.TextStyle s =
                ((com.codename1.flutter.widgets.DefaultTextStyle) styled).getStyle();
        return s.getColor().value();
    }
}
