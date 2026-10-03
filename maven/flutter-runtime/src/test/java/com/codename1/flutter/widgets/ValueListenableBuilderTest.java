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
package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.Widget;
import com.codename1.flutter.foundation.ValueNotifier;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;

import dart.runtime.Funcs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A ValueListenableBuilder has to LISTEN. Reading the value once at build time produces a
 * correct first frame and then freezes, which is the failure mode that hides best: the
 * widget renders, so it looks wired up, and only never updates again.
 *
 * <p>In the gallery that froze the whole settings panel — the button flipped its
 * ValueNotifier and nothing on screen reacted, so the menu appeared to do nothing.</p>
 */
class ValueListenableBuilderTest {

    private BuildOwner owner;

    private ValueListenableBuilder<Object> builderOn(final ValueNotifier<Object> notifier,
            final int[] builds) {
        ValueListenableBuilder<Object> b = new ValueListenableBuilder<Object>();
        b.valueListenable(notifier);
        b.builder(new Funcs.Func3<BuildContext, Object, Widget, Widget>() {
            @Override
            public Widget call(BuildContext context, Object value, Widget child) {
                builds[0]++;
                // Size the box from the value, so the rebuild is observable as geometry
                // and not merely as a counter.
                int v = value instanceof Number ? ((Number) value).intValue() : 0;
                return new ProbeBox(v, v);
            }
        });
        return b;
    }

    private com.codename1.flutter.Element mount(Widget root) {
        owner = new BuildOwner();
        return FlutterUI.mount(root, new RenderHost(), owner);
    }

    @Test
    void theBuilderRunsOnceForTheFirstFrame() {
        int[] builds = {0};
        ValueNotifier<Object> n = new ValueNotifier<Object>(Integer.valueOf(1));
        mount(builderOn(n, builds));

        assertEquals(1, builds[0]);
    }

    @Test
    void changingTheValueRebuilds() {
        int[] builds = {0};
        ValueNotifier<Object> n = new ValueNotifier<Object>(Integer.valueOf(1));
        mount(builderOn(n, builds));

        n.value(Integer.valueOf(2));
        owner.flushSync();

        assertEquals(2, builds[0], "a value change must re-invoke the builder");
    }

    @Test
    void theBuilderSeesTheNewValue() {
        final Object[] seen = new Object[1];
        ValueNotifier<Object> n = new ValueNotifier<Object>(Integer.valueOf(1));
        ValueListenableBuilder<Object> b = new ValueListenableBuilder<Object>();
        b.valueListenable(n);
        b.builder(new Funcs.Func3<BuildContext, Object, Widget, Widget>() {
            @Override
            public Widget call(BuildContext context, Object value, Widget child) {
                seen[0] = value;
                return new ProbeBox(1, 1);
            }
        });
        mount(b);

        n.value(Integer.valueOf(42));
        owner.flushSync();

        assertEquals(Integer.valueOf(42), seen[0]);
    }

    @Test
    void anUnmountedBuilderStopsListening() {
        int[] builds = {0};
        ValueNotifier<Object> n = new ValueNotifier<Object>(Integer.valueOf(1));
        com.codename1.flutter.Element root = mount(builderOn(n, builds));

        FlutterUI.unmountTree(root);
        int atUnmount = builds[0];

        n.value(Integer.valueOf(2));
        owner.flushSync();

        assertEquals(atUnmount, builds[0],
                "an unmounted builder must have removed its listener");
    }
}
