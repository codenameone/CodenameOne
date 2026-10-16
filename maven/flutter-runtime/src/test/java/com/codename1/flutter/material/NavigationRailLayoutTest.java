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

import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Column;
import com.codename1.flutter.widgets.Container;
import com.codename1.flutter.widgets.Text;

import dart.core.DartList;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A navigation rail lays out its destinations.
 *
 * <p>It used to build {@code leading} and nothing else, so a rail whose
 * destinations ARE its content rendered as a single floating button: the
 * navigation-rail demo drew its create button in the middle of an otherwise
 * empty page, with no rail behind it and no destinations at all.</p>
 */
class NavigationRailLayoutTest {

    private static NavigationRailDestination destination(String label) {
        NavigationRailDestination d = new NavigationRailDestination();
        d.icon(new Text("icon-" + label));
        d.selectedIcon(new Text("selected-" + label));
        d.label(new Text(label));
        return d;
    }

    private static NavigationRail railOfThree(NavigationRailLabelType type, long selected) {
        NavigationRail rail = new NavigationRail();
        DartList<NavigationRailDestination> ds = new DartList<NavigationRailDestination>();
        ds.add(destination("First"));
        ds.add(destination("Second"));
        ds.add(destination("Third"));
        rail.destinations(ds);
        rail.labelType(type);
        rail.selectedIndex(selected);
        return rail;
    }

    /** Every Text anywhere under a widget, in order. */
    private static void collectText(Widget w, DartList<String> out) {
        if (w == null) {
            return;
        }
        if (w instanceof Text) {
            out.add(((Text) w).getData());
            return;
        }
        if (w instanceof Container) {
            collectText(((Container) w).getChild(), out);
            return;
        }
        if (w instanceof Column) {
            DartList<Widget> kids = ((Column) w).getChildren();
            for (int i = 0; kids != null && i < kids.size(); i++) {
                collectText(kids.get(i), out);
            }
        }
    }

    private static DartList<String> textOf(NavigationRail rail) {
        DartList<String> out = new DartList<String>();
        collectText(rail.build(null), out);
        return out;
    }

    @Test
    void theRailIsAFixedWidthColumnOfItsDestinations() {
        Widget built = railOfThree(NavigationRailLabelType.none, 0).build(null);
        assertTrue(built instanceof Container, "expected the rail container, got " + built);
        Container rail = (Container) built;
        // Flutter's default minWidth.
        assertEquals(72.0, rail.getWidth().doubleValue(), 0.001);
        assertTrue(rail.getChild() instanceof Column, "the rail holds a column");
    }

    @Test
    void theSelectedDestinationUsesItsSelectedIcon() {
        DartList<String> t = textOf(railOfThree(NavigationRailLabelType.none, 1));
        assertTrue(t.contains("icon-First"), t.toString());
        assertTrue(t.contains("selected-Second"), t.toString());
        assertTrue(t.contains("icon-Third"), t.toString());
    }

    @Test
    void labelTypeSelectedShowsOnlyTheSelectedLabel() {
        DartList<String> t = textOf(railOfThree(NavigationRailLabelType.selected, 2));
        assertTrue(t.contains("Third"), "the selected label is missing: " + t);
        assertTrue(!t.contains("First") && !t.contains("Second"),
                "an unselected label was drawn: " + t);
    }

    @Test
    void labelTypeAllShowsEveryLabel() {
        DartList<String> t = textOf(railOfThree(NavigationRailLabelType.all, 0));
        assertTrue(t.contains("First") && t.contains("Second") && t.contains("Third"), t.toString());
    }

    @Test
    void labelTypeNoneShowsNoLabelAtAll() {
        DartList<String> t = textOf(railOfThree(NavigationRailLabelType.none, 0));
        assertTrue(!t.contains("First") && !t.contains("Second") && !t.contains("Third"),
                "a label was drawn for labelType none: " + t);
    }

    @Test
    void anExtendedRailIsWider() {
        NavigationRail rail = railOfThree(NavigationRailLabelType.all, 0);
        rail.extended(true);
        Container built = (Container) rail.build(null);
        assertEquals(256.0, built.getWidth().doubleValue(), 0.001);
    }

    @Test
    void theLeadingWidgetStillComesFirst() {
        NavigationRail rail = railOfThree(NavigationRailLabelType.none, 0);
        rail.leading(new Text("lead"));
        DartList<String> t = textOf(rail);
        assertNotNull(t);
        assertEquals("lead", t.get(0), "leading must head the rail: " + t);
    }
}
