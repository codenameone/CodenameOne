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
import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.TextStyle;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Column;
import com.codename1.flutter.widgets.DefaultTextStyle;
import com.codename1.flutter.widgets.Expanded;
import com.codename1.flutter.widgets.Padding;
import com.codename1.flutter.widgets.Row;
import com.codename1.flutter.widgets.SingleChildScrollView;
import com.codename1.flutter.widgets.SizedBox;

import dart.core.DartList;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A TabBar pads and colours its labels.
 *
 * <p>Both were accepted and discarded. Without the padding the labels run
 * together -- Crane's three tabs rendered as the single word "FLYSLEEPEAT" --
 * and without the colour they are painted in the default ink on a bar whose
 * labels are meant to be white.</p>
 */
class TabBarLabelTest {

    private static Row rowOf(TabBar bar) {
        Widget built = bar.build(null);
        assertTrue(built instanceof SizedBox, "expected the fixed-height box, got " + built);
        Widget inner = ((SizedBox) built).getChild();
        if (inner instanceof SingleChildScrollView) {
            inner = ((SingleChildScrollView) inner).getChild();
        }
        assertTrue(inner instanceof Row, "expected a Row of tabs, got " + inner);
        return (Row) inner;
    }

    /**
     * Each Row child is now a {@code Column} stacking the padded label over
     * the indicator strip (an {@link Expanded} holding the {@link Padding},
     * then a {@code SizedBox}) -- see {@link TabBarIndicatorTest} for the
     * strip itself. This unwraps back down to that {@link Padding}.
     */
    private static Padding paddingOf(Widget tabCell) {
        assertTrue(tabCell instanceof Column, "expected a Column tab cell, got " + tabCell);
        DartList<Widget> cellKids = ((Column) tabCell).getChildren();
        assertEquals(2, cellKids.size(), "expected [label, indicator strip]");
        Widget content = cellKids.get(0);
        assertTrue(content instanceof Expanded, "expected the label wrapped in Expanded, got " + content);
        Widget padded = ((Expanded) content).getChild();
        assertTrue(padded instanceof Padding, "tab is not padded: " + padded);
        return (Padding) padded;
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

    @Test
    void everyLabelGetsTheRequestedPadding() {
        TabBar bar = barWithThreeTabs();
        bar.labelPadding(EdgeInsets.symmetric(32, 0));
        DartList<Widget> kids = rowOf(bar).getChildren();
        assertEquals(3, kids.size());
        for (int i = 0; i < kids.size(); i++) {
            EdgeInsets p = (EdgeInsets) paddingOf(kids.get(i)).getPadding();
            assertEquals(32.0, p.left(), 0.001);
            assertEquals(32.0, p.right(), 0.001);
        }
    }

    @Test
    void aBarThatNamesNoPaddingStillGetsFluttersDefault() {
        // Flutter's kTabLabelPadding is EdgeInsets.symmetric(horizontal: 16).
        DartList<Widget> kids = rowOf(barWithThreeTabs()).getChildren();
        EdgeInsets p = (EdgeInsets) paddingOf(kids.get(0)).getPadding();
        assertEquals(16.0, p.left(), 0.001);
        assertEquals(16.0, p.right(), 0.001);
    }

    @Test
    void theSelectedTabTakesLabelColourAndTheRestTakeTheUnselectedOne() {
        TabBar bar = barWithThreeTabs();
        Color selected = new Color(0xFFFFFFFFL);
        Color rest = new Color(0x99FFFFFFL);
        bar.labelColor(selected);
        bar.unselectedLabelColor(rest);
        DartList<Widget> kids = rowOf(bar).getChildren();
        // No controller means tab 0 is the selected one.
        assertEquals(selected.value(), inkOf(kids.get(0)));
        assertEquals(rest.value(), inkOf(kids.get(1)));
        assertEquals(rest.value(), inkOf(kids.get(2)));
    }

    private static long inkOf(Widget tabCell) {
        Widget child = paddingOf(tabCell).getChild();
        assertTrue(child instanceof DefaultTextStyle, "the tab was not styled: " + child);
        TextStyle s = ((DefaultTextStyle) child).getStyle();
        assertNotNull(s, "no style on the tab");
        assertNotNull(s.getColor(), "no colour on the tab");
        return s.getColor().value();
    }
}
