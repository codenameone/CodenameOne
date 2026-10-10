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
package com.codename1.flutter;

import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.rendering.Size;
import com.codename1.flutter.testsupport.ProbeBox;
import com.codename1.flutter.MediaQuery;
import com.codename1.flutter.widgets.SafeArea;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * SafeArea INSETS its child, and MediaQuery reports the device's real insets.
 *
 * <p>Both were zero: padding defaulted to nothing and SafeArea rendered its child through.
 * The result is a class of bug no error report can see — content under the notch, the status
 * bar and the home indicator, and controls near the bottom edge that cannot be reached. The
 * app throws nothing and looks plausible in a screenshot taken on a device without a notch,
 * which is why a clean sweep said nothing about it.</p>
 */
class SafeAreaTest {

    /** A MediaQuery with the insets a notched phone reports. */
    private static Widget withInsets(Widget child, double top, double bottom) {
        MediaQuery q = new MediaQuery();
        q.data(new MediaQueryData(new Size(400, 800), 1.0, Brightness.light, 1.0,
                EdgeInsets.fromLTRB(0, top, 0, bottom)));
        q.child(child);
        return q;
    }

    /**
     * Lays the tree out in a full-screen box and returns the CONTENT's render element.
     *
     * <p>The content, not the root: under a tight constraint the outer box still fills the
     * screen — what a SafeArea changes is where the child sits inside it. Asserting the
     * root's size would pass whether or not anything was inset, which is the same mistake
     * that let this bug through in the first place.</p>
     */
    private static RenderElement contentOf(Widget root, double w, double h) {
        Element e = FlutterUI.mount(root, new RenderHost(), new BuildOwner());
        RenderElement r = RenderElement.findRenderElement(e);
        r.layout(BoxConstraints.tight(w, h));
        r.position(0, 0);
        return deepest(r);
    }

    /** The ProbeBox at the bottom of the subtree. */
    private static RenderElement deepest(RenderElement from) {
        final RenderElement[] found = {null};
        from.visitChildren(new dart.runtime.Funcs.VoidFunc1<Element>() {
            @Override
            public void call(Element child) {
                RenderElement r = RenderElement.findRenderElement(child);
                if (r != null && r.widget() instanceof ProbeBox) {
                    found[0] = r;
                } else if (r != null && found[0] == null) {
                    RenderElement deeper = deepest(r);
                    if (deeper != null && deeper.widget() instanceof ProbeBox) {
                        found[0] = deeper;
                    }
                }
            }
        });
        return found[0] != null ? found[0] : from;
    }

    @Test
    @DisplayName("the metric math reports the safe-area insets it is given")
    void computeCarriesThePadding() {
        MediaQueryData d = MediaQueryData.compute(1125, 2436, 3.0, Boolean.FALSE,
                EdgeInsets.fromLTRB(0, 47, 0, 34));

        assertEquals(47, d.padding().top(), 1e-9);
        assertEquals(34, d.padding().bottom(), 1e-9);
    }

    @Test
    @DisplayName("with no insets given the padding is zero, as before")
    void computeDefaultsToZero() {
        assertEquals(0, MediaQueryData.compute(400, 800, 1.0, Boolean.FALSE).padding().top(),
                1e-9);
    }

    @Test
    @DisplayName("a SafeArea shrinks its child by the top and bottom insets")
    void safeAreaInsetsItsChild() {
        SafeArea area = new SafeArea();
        area.child(new ProbeBox(400, 800));

        RenderElement r = contentOf(withInsets(area, 47, 34), 400, 800);

        // Pushed clear of the notch, and short by both intrusions.
        assertEquals(47, r.y(), "the content must start below the status bar");
        assertEquals(800 - 47 - 34, r.size().height(), 1e-9,
                "and must not run under the home indicator");
    }

    @Test
    @DisplayName("a disabled edge is not inset")
    void aDisabledEdgeIsNotInset() {
        SafeArea area = new SafeArea();
        area.bottom(false);
        area.child(new ProbeBox(400, 800));

        RenderElement r = contentOf(withInsets(area, 47, 34), 400, 800);

        assertEquals(47, r.y());
        assertEquals(800 - 47, r.size().height(), 1e-9, "only the top is avoided");
    }

    @Test
    @DisplayName("minimum wins when the device reports less")
    void minimumIsRespected() {
        SafeArea area = new SafeArea();
        area.minimum(EdgeInsets.fromLTRB(0, 20, 0, 20));
        area.child(new ProbeBox(400, 800));

        RenderElement r = contentOf(withInsets(area, 5, 5), 400, 800);

        assertEquals(20, r.y());
        assertEquals(800 - 20 - 20, r.size().height(), 1e-9);
    }

    @Test
    @DisplayName("with no intrusions a SafeArea costs nothing")
    void noInsetsMeansNoWrapper() {
        SafeArea area = new SafeArea();
        area.child(new ProbeBox(400, 800));

        RenderElement r = contentOf(withInsets(area, 0, 0), 400, 800);

        assertEquals(0, r.y());
        assertEquals(800, r.size().height(), 1e-9);
    }
}
