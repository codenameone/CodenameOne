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
import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.Icons;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;
import com.codename1.flutter.widgets.Icon;

import dart.core.DartList;
import dart.runtime.Funcs;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A popup menu button SHOWS something and DOES something.
 *
 * <p>It previously did neither: with no explicit child or icon it laid out as nothing at
 * all, and a tap went nowhere because the trigger was never made tappable. Both failures are
 * silent — the app bar simply had one fewer button than the Dart described.</p>
 */
class PopupMenuTest {

    @AfterEach
    void closeAnyMenu() {
        PopupMenus.dismiss();
    }

    private static PopupMenuItem<String> item(String value, Widget child) {
        PopupMenuItem<String> i = new PopupMenuItem<String>();
        i.value(value);
        i.child(child);
        return i;
    }

    private PopupMenuButton<String> buttonWithItems(final String... values) {
        PopupMenuButton<String> b = new PopupMenuButton<String>();
        b.itemBuilder(new Funcs.Func1<BuildContext, Object>() {
            @Override
            public Object call(BuildContext context) {
                DartList<PopupMenuEntry<String>> list = new DartList<PopupMenuEntry<String>>();
                for (String v : values) {
                    list.add(item(v, new ProbeBox(60, 20)));
                }
                return list;
            }
        });
        return b;
    }

    @Test
    @DisplayName("with no child or icon the trigger is the overflow glyph")
    void theDefaultTriggerIsTheOverflowIcon() {
        Widget trigger = new PopupMenuButton<String>().effectiveTrigger();

        assertNotNull(trigger, "a menu button with no icon must still show something");
        assertTrue(trigger instanceof Icon, "Flutter falls back to an Icon");
        assertEquals(Icons.more_vert.codePoint(), ((Icon) trigger).getIcon().codePoint());
    }

    @Test
    @DisplayName("an explicit icon wins over the default")
    void anExplicitIconIsUsed() {
        PopupMenuButton<String> b = new PopupMenuButton<String>();
        b.icon(new Icon(Icons.search));

        assertEquals(Icons.search.codePoint(),
                ((Icon) b.effectiveTrigger()).getIcon().codePoint());
    }

    @Test
    @DisplayName("the button renders the trigger glyph and is pressable")
    void theTriggerIsRendered() {
        PopupMenuButton<String> b = buttonWithItems("a");
        PopupMenuButtonRenderElement e = (PopupMenuButtonRenderElement)
                FlutterUI.mount(b, new RenderHost(), new BuildOwner());

        assertEquals(Icons.more_vert.codePoint(), e.consumedIconChar(),
                "the overflow glyph must be what the button draws");
        assertNotNull(e.pressHandler(), "and pressing it must do something");
    }

    @Test
    @DisplayName("a disabled button has no press handler, so CN1 disables it")
    void aDisabledButtonHasNoHandler() {
        PopupMenuButton<String> b = buttonWithItems("a");
        b.enabled(false);
        PopupMenuButtonRenderElement e = (PopupMenuButtonRenderElement)
                FlutterUI.mount(b, new RenderHost(), new BuildOwner());

        assertEquals(null, e.pressHandler());
    }

    @Test
    @DisplayName("showing the menu builds the items")
    void showingBuildsTheItems() {
        final int[] builds = {0};
        PopupMenuButton<String> b = new PopupMenuButton<String>();
        b.itemBuilder(new Funcs.Func1<BuildContext, Object>() {
            @Override
            public Object call(BuildContext context) {
                builds[0]++;
                DartList<PopupMenuEntry<String>> l = new DartList<PopupMenuEntry<String>>();
                l.add(item("one", new ProbeBox(60, 20)));
                return l;
            }
        });
        PopupMenuButtonRenderElement e = (PopupMenuButtonRenderElement)
                FlutterUI.mount(b, new RenderHost(), new BuildOwner());

        PopupMenus.show(e, b);

        assertEquals(1, builds[0], "the itemBuilder must run when the menu opens");
        assertTrue(PopupMenus.isOpen());
    }

    @Test
    @DisplayName("a disabled button opens nothing")
    void aDisabledButtonDoesNotOpen() {
        PopupMenuButton<String> b = buttonWithItems("a");
        b.enabled(false);
        PopupMenuButtonRenderElement e = (PopupMenuButtonRenderElement)
                FlutterUI.mount(b, new RenderHost(), new BuildOwner());

        PopupMenus.show(e, b);

        assertFalse(PopupMenus.isOpen());
    }

    @Test
    @DisplayName("an itemBuilder returning nothing opens nothing")
    void anEmptyMenuDoesNotOpen() {
        PopupMenuButton<String> b = buttonWithItems();
        PopupMenuButtonRenderElement e = (PopupMenuButtonRenderElement)
                FlutterUI.mount(b, new RenderHost(), new BuildOwner());

        PopupMenus.show(e, b);

        assertFalse(PopupMenus.isOpen(), "an empty menu must not put up an empty surface");
    }

    @Test
    @DisplayName("dismiss closes the menu")
    void dismissClosesIt() {
        PopupMenuButton<String> b = buttonWithItems("a", "b");
        PopupMenuButtonRenderElement e = (PopupMenuButtonRenderElement)
                FlutterUI.mount(b, new RenderHost(), new BuildOwner());
        PopupMenus.show(e, b);

        PopupMenus.dismiss();

        assertFalse(PopupMenus.isOpen());
    }
}
