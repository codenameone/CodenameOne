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
import com.codename1.flutter.widgets.LayoutBuilder;
import com.codename1.flutter.widgets.SizedBox;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A LayoutBuilder's builder must not be handed a speculative, unbounded box.
 *
 * <p>Codename One asks a box how wide it would like to be -- a real layout call,
 * not a dry one, with the width unbounded -- before it lays the box out at the
 * size it will occupy. A Flutter builder is allowed to LATCH: the gallery's
 * 2D-transformations demo centres its board against the first
 * {@code constraints.maxWidth} it is shown and never recomputes. So a builder
 * that runs against infinity once has decided the screen, and the board drew in
 * the corner for the life of the route.</p>
 */
class LayoutBuilderLatchTest {

    /** The constraints each builder call was given, in order. */
    private final List<BoxConstraints> seen = new ArrayList<BoxConstraints>();

    private LayoutBuilder recordingBuilder() {
        LayoutBuilder lb = new LayoutBuilder();
        lb.builder(new dart.runtime.Funcs.Func2<BuildContext, BoxConstraints, Widget>() {
            @Override
            public Widget call(BuildContext context, BoxConstraints constraints) {
                seen.add(constraints);
                return new SizedBox();
            }
        });
        return lb;
    }

    private RenderElement mount(Widget root) {
        FlutterUI.mount(root, new RenderHost(), new BuildOwner());
        return null;
    }

    @Test
    void anUnboundedPassIsSatOutSoTheFirstBuildSeesTheRealBox() {
        LayoutBuilder lb = recordingBuilder();
        RenderHost host = new RenderHost();
        FlutterUI.mount(lb, host, new BuildOwner());
        RenderElement r = host.rootRenderElement();

        // How Codename One measures: real pass, width unbounded.
        r.layout(new BoxConstraints(0, Double.POSITIVE_INFINITY, 0, 550));
        assertTrue(seen.isEmpty(), "the builder latched onto an unbounded measurement");

        // Then the box it will actually occupy.
        r.layout(BoxConstraints.tight(343, 550));
        assertEquals(1, seen.size(), "the builder should have run exactly once by now");
        assertEquals(343.0, seen.get(0).maxWidth(), 0.001);
        assertEquals(550.0, seen.get(0).maxHeight(), 0.001);
    }

    @Test
    void aViewportChildBuildsIMMEDIATELY() {
        // The regression this pins. A vertical list hands its child a TIGHT
        // width and an unbounded height, and Flutter runs the builder against
        // exactly that. Sitting it out returns a zero size the list then keeps,
        // which emptied the reply study's whole mail list -- while the diff
        // score went DOWN, because blank background differs from the reference
        // less than mis-rendered cards do.
        LayoutBuilder lb = recordingBuilder();
        RenderHost host = new RenderHost();
        FlutterUI.mount(lb, host, new BuildOwner());
        RenderElement r = host.rootRenderElement();

        r.layout(new BoxConstraints(367, 367, 0, Double.POSITIVE_INFINITY));
        assertEquals(1, seen.size(), "a viewport child must not be sat out");
        assertEquals(367.0, seen.get(0).maxWidth(), 0.001);
    }

    @Test
    void aGenuinelyUnboundedLayoutStillBuilds() {
        // A viewport's child really is unbounded and Flutter runs the builder
        // against infinity, so sitting out MUST NOT mean never building.
        LayoutBuilder lb = recordingBuilder();
        RenderHost host = new RenderHost();
        FlutterUI.mount(lb, host, new BuildOwner());
        RenderElement r = host.rootRenderElement();

        BoxConstraints unbounded = new BoxConstraints(0, 300, 0, Double.POSITIVE_INFINITY);
        r.layout(unbounded);
        r.layout(unbounded);
        assertEquals(1, seen.size(), "the builder never ran on a truly unbounded layout");
        assertTrue(Double.isInfinite(seen.get(0).maxHeight()));
    }
}
