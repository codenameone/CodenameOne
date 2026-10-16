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
package com.codename1.flutter.animation;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.Element;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.Widget;
import com.codename1.flutter.foundation.ValueNotifier;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * An AnimatedWidget subclass has to REBUILD when its listenable notifies.
 *
 * <p>The builder-callback form ({@link AnimatedBuilder}) always listened; the subclass form
 * did not, and the difference is invisible in a screenshot of the first frame. In the
 * gallery it left the settings button painting whichever glyph it was built with — the
 * sliders never swept round into the close X, however far the controller ran.</p>
 */
class AnimatedWidgetTest {

    /** The shape the gallery uses: a subclass that renders from the listenable's value. */
    static class Driven extends AnimatedWidget {
        int builds;
        double lastSeen = -1;

        @Override
        public Widget build(BuildContext context) {
            builds++;
            Object v = ((ValueNotifier<?>) listenable()).value();
            lastSeen = v instanceof Number ? ((Number) v).doubleValue() : -1;
            // Sized from the value, so a rebuild is observable as geometry and not only as
            // a counter that a stale build could still increment.
            int side = (int) (lastSeen * 10);
            return new ProbeBox(side, side);
        }
    }

    private BuildOwner owner;

    private Element mount(Widget root) {
        owner = new BuildOwner();
        return FlutterUI.mount(root, new RenderHost(), owner);
    }

    private Driven drivenBy(ValueNotifier<Object> n) {
        Driven d = new Driven();
        d.listenable(n);
        return d;
    }

    @Test
    @DisplayName("the first frame reads the current value")
    void theFirstFrameReadsTheValue() {
        ValueNotifier<Object> n = new ValueNotifier<Object>(Double.valueOf(1));
        Driven d = drivenBy(n);
        mount(d);

        assertEquals(1, d.builds);
        assertEquals(1.0, d.lastSeen, 1e-9);
    }

    @Test
    @DisplayName("a notification rebuilds the widget with the new value")
    void aNotificationRebuilds() {
        ValueNotifier<Object> n = new ValueNotifier<Object>(Double.valueOf(0));
        Driven d = drivenBy(n);
        mount(d);

        n.value(Double.valueOf(0.5));
        owner.flushSync();

        assertEquals(2, d.builds, "the listenable must drive a rebuild");
        assertEquals(0.5, d.lastSeen, 1e-9, "and the rebuild must see the NEW value");
    }

    @Test
    @DisplayName("every tick of a run is seen, not just the last")
    void everyTickIsSeen() {
        ValueNotifier<Object> n = new ValueNotifier<Object>(Double.valueOf(0));
        Driven d = drivenBy(n);
        mount(d);

        // A controller sweeping 0 -> 1 is the real case: the icon interpolates through the
        // transition phase, so dropping intermediate frames would still land on the right
        // final glyph while never animating.
        for (int i = 1; i <= 4; i++) {
            n.value(Double.valueOf(i / 4.0));
            owner.flushSync();
        }

        assertEquals(5, d.builds, "one build per tick, plus the first frame");
        assertEquals(1.0, d.lastSeen, 1e-9);
    }

    @Test
    @DisplayName("an unmounted widget stops listening")
    void anUnmountedWidgetStopsListening() {
        ValueNotifier<Object> n = new ValueNotifier<Object>(Double.valueOf(0));
        Driven d = drivenBy(n);
        Element root = mount(d);

        FlutterUI.unmountTree(root);
        int atUnmount = d.builds;

        n.value(Double.valueOf(1));
        owner.flushSync();

        assertEquals(atUnmount, d.builds,
                "a widget off the tree must have removed its listener");
    }
}
