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
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.widgets.HasChild;
import com.codename1.flutter.widgets.Icon;
import com.codename1.flutter.widgets.Text;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A button finds the glyph inside a WRAPPED icon.
 *
 * <p>Buttons consume their content instead of mounting it, so a content widget that was
 * neither a Text nor an Icon used to be drawn via {@code toString()}. That is how the
 * gallery's back button came out as the text "com.codename1.flutter.material.BackButton…",
 * clipped by the app bar to "com.c" — and it is the reason a whole row of demo pages looked
 * like they had no way back.</p>
 */
class ButtonContentUnwrapTest {

    /** A composed widget that builds an icon — the shape of BackButtonIcon. */
    static class ComposedIcon extends StatelessWidget {
        @Override
        public Widget build(BuildContext context) {
            return new Icon(Icons.arrow_back);
        }
    }

    /** A single-child wrapper — the shape of FeatureDiscovery around the options icon. */
    static class Wrapper extends Widget implements HasChild {
        private final Widget child;

        Wrapper(Widget child) {
            this.child = child;
        }

        @Override
        public Widget getChild() {
            return child;
        }

        @Override
        public com.codename1.flutter.Element createElement() {
            return new com.codename1.flutter.widgets.PassThroughRenderElement(this);
        }
    }

    private ButtonRenderElement mount(Widget icon) {
        IconButton b = new IconButton();
        b.icon(icon);
        return (ButtonRenderElement) FlutterUI.mount(b, new RenderHost(), new BuildOwner());
    }

    @Test
    @DisplayName("a plain Icon still yields its glyph")
    void aPlainIconIsFound() {
        assertEquals(Icons.arrow_back.codePoint(),
                mount(new Icon(Icons.arrow_back)).consumedIconChar());
    }

    @Test
    @DisplayName("a composed widget is built to find the icon it produces")
    void aComposedIconIsResolved() {
        ButtonRenderElement e = mount(new ComposedIcon());

        assertEquals(Icons.arrow_back.codePoint(), e.consumedIconChar(),
                "BackButtonIcon-shaped content must resolve to its glyph");
        assertEquals(null, e.consumedLabel(),
                "and must NOT fall back to a toString() label");
    }

    @Test
    @DisplayName("a wrapped icon is found through the wrapper")
    void aWrappedIconIsResolved() {
        ButtonRenderElement e = mount(new Wrapper(new Icon(Icons.search)));

        assertEquals(Icons.search.codePoint(), e.consumedIconChar());
        assertEquals(null, e.consumedLabel());
    }

    @Test
    @DisplayName("nested wrapping still resolves")
    void nestingIsWalked() {
        ButtonRenderElement e = mount(new Wrapper(new ComposedIcon()));

        assertEquals(Icons.arrow_back.codePoint(), e.consumedIconChar());
    }

    @Test
    @DisplayName("a wrapped Text is still used as the label")
    void aWrappedTextBecomesTheLabel() {
        ButtonRenderElement e = mount(new Wrapper(new Text("Go back")));

        assertEquals("Go back", e.consumedLabel());
    }

    /** A stateful wrapper around an icon — the shape of the gallery's FeatureDiscovery. */
    static class StatefulIcon extends com.codename1.flutter.StatefulWidget {
        @Override
        public com.codename1.flutter.State<?> createState() {
            return new com.codename1.flutter.State<StatefulIcon>() {
                @Override
                public Widget build(BuildContext context) {
                    return new Icon(Icons.tune);
                }
            };
        }
    }

    @Test
    @DisplayName("a STATEFUL wrapper is built to find the icon it produces")
    void aStatefulIconIsResolved() {
        ButtonRenderElement e = mount(new StatefulIcon());

        assertEquals(Icons.tune.codePoint(), e.consumedIconChar(),
                "FeatureDiscovery-shaped content must resolve to its glyph");
        assertEquals(null, e.consumedLabel());
    }

    @Test
    @DisplayName("unresolvable content NEVER becomes a toString() label")
    void unresolvableContentIsNeverStringified() {
        // A widget the walk cannot see through. Printing its class name put
        // "com.codename1.flutter..." across the app bar; no label is the only honest answer.
        // It has to be MOUNTABLE as well as opaque: a button now mounts content it cannot
        // consume rather than dropping it, so a fixture whose element does not match its
        // widget fails on the cast rather than on what this test is about.
        class Opaque extends Widget implements com.codename1.flutter.widgets.HasChild {
            @Override
            public Widget getChild() {
                return null;
            }

            @Override
            public com.codename1.flutter.Element createElement() {
                return new com.codename1.flutter.widgets.PassThroughRenderElement(this);
            }
        }
        Widget opaque = new Opaque();
        ButtonRenderElement e = mount(opaque);

        assertEquals(null, e.consumedLabel(), "no label beats a class name");
        assertEquals(0, e.consumedIconChar());
    }

    @Test
    @DisplayName("content that resolves to nothing renders no glyph and no label text")
    void unresolvableContentIsNotStringified() {
        // A widget that is neither wrapper nor composed: the walk gives up. It must not
        // crash, and the old toString() behaviour is what remains for it.
        ButtonRenderElement e = mount(null);

        assertEquals(0, e.consumedIconChar());
        assertEquals(null, e.consumedLabel());
    }
}
