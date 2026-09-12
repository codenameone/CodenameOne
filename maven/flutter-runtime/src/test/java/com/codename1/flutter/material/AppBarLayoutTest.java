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

import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.Element;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;

import dart.core.DartList;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The app bar lays out ALL THREE of its slots — leading, title, actions.
 *
 * <p>It used to lay out only the title and drop the rest, which is a failure that looks
 * like a design decision: the bar renders, the page has a heading, and nothing announces
 * that the back button and every action are missing. In the gallery that turned every demo
 * page into a dead end, since the back button is the {@code leading}.</p>
 *
 * <p>Headless, so Dp scale is 1 and logical pixels are pixels.</p>
 */
class AppBarLayoutTest {

    /** Everything on the bar, in the order the layout emitted it. */
    private static final class Bar {
        AppBarRenderElement element;
        List<RenderElement> children = new ArrayList<RenderElement>();
    }

    private Bar mountAndLayout(AppBar bar, BoxConstraints c) {
        Bar out = new Bar();
        out.element = (AppBarRenderElement)
                FlutterUI.mount(bar, new RenderHost(), new BuildOwner());
        out.element.layout(c);
        out.element.position(0, 0);
        final List<RenderElement> kids = out.children;
        out.element.visitChildren(new dart.runtime.Funcs.VoidFunc1<Element>() {
            @Override
            public void call(Element e) {
                RenderElement r = RenderElement.findRenderElement(e);
                if (r != null) {
                    kids.add(r);
                }
            }
        });
        return out;
    }

    private static AppBar barWith(Widget leading, Widget title, Widget... actions) {
        AppBar b = new AppBar();
        if (leading != null) {
            b.leading(leading);
        }
        if (title != null) {
            b.title(title);
        }
        if (actions != null && actions.length > 0) {
            DartList<Widget> l = new DartList<Widget>();
            for (Widget a : actions) {
                l.add(a);
            }
            b.actions(l);
        }
        // The implied back button needs a Navigator; these tests pin the explicit slots.
        b.automaticallyImplyLeading(false);
        return b;
    }

    @Test
    @DisplayName("leading, title and actions are all laid out")
    void everySlotIsLaidOut() {
        Bar bar = mountAndLayout(
                barWith(new ProbeBox(40, 40), new ProbeBox(100, 20),
                        new ProbeBox(40, 40), new ProbeBox(40, 40)),
                BoxConstraints.tight(400, 56));

        assertEquals(4, bar.children.size(),
                "leading + title + 2 actions must all become children");
    }

    @Test
    @DisplayName("the leading sits at the start and the actions flush to the end")
    void slotsArePlacedAcrossTheBar() {
        ProbeBox leading = new ProbeBox(40, 40);
        ProbeBox title = new ProbeBox(100, 20);
        ProbeBox action = new ProbeBox(40, 40);
        Bar bar = mountAndLayout(barWith(leading, title, action),
                BoxConstraints.tight(400, 56));

        RenderElement l = bar.children.get(0);
        RenderElement t = bar.children.get(1);
        RenderElement a = bar.children.get(2);

        assertEquals(0, l.x(), "leading at the very start");
        assertEquals(400 - 40, a.x(), "the action is flush to the trailing edge");
        assertEquals(40 + 16, t.x(), "the title clears the leading by kMiddleSpacing");
        assertTrue(t.x() + 100 <= a.x(), "the title must not run under the actions");
    }

    @Test
    @DisplayName("flexibleSpace fills the whole bar, behind the row")
    void flexibleSpaceFillsTheBarBehindTheRow() {
        AppBar b = barWith(new ProbeBox(40, 40), new ProbeBox(100, 20));
        ProbeBox background = new ProbeBox(10, 10);
        b.flexibleSpace(background);

        Bar bar = mountAndLayout(b, BoxConstraints.tight(400, 56));

        assertEquals(3, bar.children.size(),
                "flexibleSpace joins leading and title as a child");
        RenderElement flexible = bar.children.get(0);
        assertEquals(0, flexible.x(), "the background starts at the bar's leading edge");
        assertEquals(0, flexible.y(), "and at its top, not centred like a slot");
        assertEquals(400, flexible.size().width(), "it is stretched across the bar");
        assertEquals(56, flexible.size().height(), "and down it");
    }

    @Test
    @DisplayName("flexibleSpace does not take width from the row")
    void flexibleSpaceIsNotAFourthSlot() {
        ProbeBox leading = new ProbeBox(40, 40);
        ProbeBox title = new ProbeBox(100, 20);
        ProbeBox action = new ProbeBox(40, 40);

        AppBar plain = barWith(leading, title, action);
        Bar without = mountAndLayout(plain, BoxConstraints.tight(400, 56));
        double titleXWithout = without.children.get(1).x();
        double actionXWithout = without.children.get(2).x();

        AppBar withBackground = barWith(new ProbeBox(40, 40), new ProbeBox(100, 20),
                new ProbeBox(40, 40));
        withBackground.flexibleSpace(new ProbeBox(180, 56));
        Bar with = mountAndLayout(withBackground, BoxConstraints.tight(400, 56));

        assertEquals(titleXWithout, with.children.get(2).x(),
                "the title sits where it did before a background was added");
        assertEquals(actionXWithout, with.children.get(3).x(),
                "and so does the action");
    }

    @Test
    @DisplayName("a bar with only a flexibleSpace still has a width")
    void flexibleSpaceOnlyBarMeasuresItsBackground() {
        // Crane's bar is exactly this: no title, no leading, no actions — the
        // whole thing is the flexibleSpace. Summing the row alone would measure
        // the bar as zero wide and collapse it.
        AppBar b = new AppBar();
        b.automaticallyImplyLeading(false);
        b.flexibleSpace(new ProbeBox(240, 56));

        AppBarRenderElement element = (AppBarRenderElement)
                FlutterUI.mount(b, new RenderHost(), new BuildOwner());
        assertEquals(240, element.layout(
                BoxConstraints.loose(Double.POSITIVE_INFINITY, 56)).width(),
                "the bar measures its background when it has nothing else");
    }

    @Test
    @DisplayName("slots are centred vertically in the bar")
    void slotsAreVerticallyCentred() {
        Bar bar = mountAndLayout(
                barWith(new ProbeBox(40, 40), new ProbeBox(100, 20)),
                BoxConstraints.tight(400, 56));

        assertEquals((56 - 40) / 2, bar.children.get(0).y(), "leading centred");
        assertEquals((56 - 20) / 2, bar.children.get(1).y(), "title centred");
    }

    @Test
    @DisplayName("several actions stack rightwards, ending at the edge")
    void actionsPackTowardsTheEdge() {
        Bar bar = mountAndLayout(
                barWith(null, new ProbeBox(50, 20),
                        new ProbeBox(48, 40), new ProbeBox(48, 40), new ProbeBox(48, 40)),
                BoxConstraints.tight(400, 56));

        // title, then the three actions in order
        assertEquals(400 - 48 * 3, bar.children.get(1).x());
        assertEquals(400 - 48 * 2, bar.children.get(2).x());
        assertEquals(400 - 48, bar.children.get(3).x());
    }

    @Test
    @DisplayName("a centred title stays clear of both the leading and the actions")
    void aCentredTitleDoesNotSlideUnderTheSlots() {
        AppBar b = barWith(new ProbeBox(56, 40), new ProbeBox(300, 20), new ProbeBox(48, 40));
        b.centerTitle(true);
        Bar bar = mountAndLayout(b, BoxConstraints.tight(400, 56));

        RenderElement title = bar.children.get(1);
        assertTrue(title.x() >= 56, "a wide centred title is pushed clear of the leading");
        assertTrue(title.x() + title.size().width() <= 400,
                "and never runs off the trailing edge");
    }

    @Test
    @DisplayName("the title is measured against what the other slots leave free")
    void theTitleGetsOnlyTheRemainingWidth() {
        // A greedy title (very wide probe) must be squeezed, not allowed to overlap.
        Bar bar = mountAndLayout(
                barWith(new ProbeBox(56, 40), new ProbeBox(1000, 20), new ProbeBox(48, 40)),
                BoxConstraints.tight(400, 56));

        RenderElement title = bar.children.get(1);
        assertEquals(400 - 56 - 48 - 32, title.size().width(),
                "title width = bar - leading - actions - spacing either side");
    }

    @Test
    @DisplayName("with no bounded width the bar is as wide as its contents")
    void anUnboundedBarSizesToItsContents() {
        Bar bar = mountAndLayout(
                barWith(new ProbeBox(56, 40), new ProbeBox(100, 20), new ProbeBox(48, 40)),
                BoxConstraints.loose(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));

        assertEquals(56 + 48 + 100 + 32, bar.element.size().width(),
                "leading + actions + title + its spacing");
        assertEquals(56, bar.element.size().height(), "the Material default height");
    }

    @Test
    @DisplayName("a bar with only a title still puts it at the leading inset")
    void titleOnlyKeepsTheInset() {
        Bar bar = mountAndLayout(barWith(null, new ProbeBox(100, 20)),
                BoxConstraints.tight(400, 56));

        assertEquals(1, bar.children.size());
        assertEquals(16, bar.children.get(0).x(), "no leading, so the title starts at 16lp");
    }
}
