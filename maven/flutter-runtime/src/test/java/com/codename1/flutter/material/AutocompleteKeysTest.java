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
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.StatefulElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.widgets.SizedBox;
import com.codename1.ui.Display;

import dart.core.DartList;

import com.codename1.flutter.testsupport.RasterDisplay;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Autocomplete headless: the arrow and enter keys move and pick the highlight, and
/// fieldViewBuilder / optionsViewBuilder are handed the controller, the selection
/// callback and the options as Flutter hands them.
class AutocompleteKeysTest {

    @BeforeEach
    void installDisplay() {
        RasterDisplay.install();
    }

    @AfterEach
    void uninstallDisplay() {
        RasterDisplay.uninstall();
    }

    private static final List<String> WORDS = List.of("apple", "apricot", "avocado", "banana");

    private static Autocomplete<String> autocomplete(List<String> picked) {
        Autocomplete<String> a = new Autocomplete<String>();
        a.optionsBuilder(v -> {
            DartList<String> out = DartList.of();
            for (String s : WORDS) {
                if (s.startsWith(v.text())) {
                    out.add(s);
                }
            }
            return out;
        });
        a.onSelected(picked::add);
        return a;
    }

    @SuppressWarnings("unchecked")
    private static Autocomplete.AutocompleteState<String> mount(Widget w, BuildOwner owner) {
        RenderHost host = new RenderHost();
        StatefulElement e = (StatefulElement) FlutterUI.mount(w, host, owner);
        host.rootRenderElement().layout(BoxConstraints.tight(300, 600));
        return (Autocomplete.AutocompleteState<String>) e.state();
    }

    @Test
    void arrowsWrapAndEnterSelectsTheHighlightedOption() {
        List<String> picked = new ArrayList<String>();
        BuildOwner owner = new BuildOwner();
        Autocomplete.AutocompleteState<String> s = mount(autocomplete(picked), owner);
        assertFalse(s.handleKey(Display.GAME_DOWN), "no options, the key is not taken");
        s.textController().text("ap");
        owner.flushSync();
        assertEquals(List.of("apple", "apricot"), new ArrayList<String>(s.visibleOptions()));
        assertEquals(0, s.highlightedIndex());
        assertTrue(s.handleKey(Display.GAME_DOWN));
        assertEquals(1, s.highlightedIndex());
        assertTrue(s.handleKey(Display.GAME_DOWN));
        assertEquals(0, s.highlightedIndex(), "wraps past the last");
        assertTrue(s.handleKey(Display.GAME_UP));
        assertEquals(1, s.highlightedIndex(), "wraps before the first");
        assertTrue(s.handleKey(Display.GAME_FIRE));
        owner.flushSync();
        assertEquals(List.of("apricot"), picked);
        assertEquals("apricot", s.textController().text());
        assertTrue(s.visibleOptions().isEmpty(), "hidden until the text is edited again");
        assertFalse(s.handleKey(Display.GAME_FIRE));
        s.textController().text("b");
        owner.flushSync();
        assertEquals(List.of("banana"), new ArrayList<String>(s.visibleOptions()));
    }

    @Test
    void customFieldAndOptionsViewsAreBuiltWithTheStatesCallbacks() {
        List<String> picked = new ArrayList<String>();
        final TextEditingController[] field = new TextEditingController[1];
        final Runnable[] submit = new Runnable[1];
        final List<List<String>> shown = new ArrayList<List<String>>();
        final List<dart.runtime.Funcs.VoidFunc1<String>> onSelected =
                new ArrayList<dart.runtime.Funcs.VoidFunc1<String>>();
        Autocomplete<String> a = autocomplete(picked);
        a.fieldViewBuilder((ctx, controller, focus, onFieldSubmitted) -> {
            field[0] = controller;
            submit[0] = onFieldSubmitted::call;
            return new SizedBox();
        });
        a.optionsViewBuilder((ctx, select, options) -> {
            List<String> l = new ArrayList<String>();
            for (String o : options) {
                l.add(o);
            }
            shown.add(l);
            onSelected.add(select);
            return new SizedBox();
        });
        BuildOwner owner = new BuildOwner();
        Autocomplete.AutocompleteState<String> s = mount(a, owner);
        assertSame(s.textController(), field[0]);
        assertTrue(shown.isEmpty(), "no options view without options");
        field[0].text("a");
        owner.flushSync();
        assertEquals(List.of("apple", "apricot", "avocado"), shown.get(shown.size() - 1));
        s.handleKey(Display.GAME_DOWN);
        s.handleKey(Display.GAME_DOWN);
        submit[0].run();
        owner.flushSync();
        assertEquals(List.of("avocado"), picked, "onFieldSubmitted picks the highlight");
        field[0].text("ban");
        owner.flushSync();
        onSelected.get(onSelected.size() - 1).call("banana");
        owner.flushSync();
        assertEquals(List.of("avocado", "banana"), picked);
        assertEquals("banana", field[0].text());
    }
}
