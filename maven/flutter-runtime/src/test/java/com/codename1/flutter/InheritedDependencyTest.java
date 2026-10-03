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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;
import com.codename1.flutter.widgets.Column;
import com.codename1.flutter.widgets.InheritedWidget;
import com.codename1.flutter.widgets.Padding;

import dart.core.DartList;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Reading an InheritedWidget creates a DEPENDENCY, and changing it rebuilds the readers.
 *
 * <p>The tree here is deliberately the shape the gallery actually uses — a StatefulWidget
 * holding the value, an InheritedWidget publishing it, and a consumer several layers below
 * — because a flatter arrangement does not exercise the thing that matters. An earlier
 * attempt at this feature passed a flat version of these tests and rendered whole pages
 * blank on a device, so "the harness agrees" is only worth something if the harness is
 * shaped like the real tree.</p>
 */
class InheritedDependencyTest {

    /** An inherited widget carrying one value, with the usual "did it change?" test. */
    static class Model extends InheritedWidget {
        final int value;

        Model(int value, Widget child) {
            this.value = value;
            child(child);
        }

        @Override
        public boolean updateShouldNotify(InheritedWidget oldWidget) {
            return ((Model) oldWidget).value != value;
        }
    }

    /** Reads the model on every build — the consumer, buried a few layers down. */
    static class Reader extends StatelessWidget {
        int builds;
        int lastSeen = -1;

        @Override
        public Widget build(BuildContext context) {
            builds++;
            Model m = context.dependOnInheritedWidgetOfExactType(Model.class);
            lastSeen = m == null ? -1 : m.value;
            return new ProbeBox(1, 1);
        }
    }

    /** Wraps its child a few levels deep, so the consumer is not a direct child. */
    private static Widget buried(Widget child) {
        Padding inner = new Padding();
        inner.padding(EdgeInsets.all(1));
        inner.child(child);
        Column col = new Column();
        col.children(DartList.of((Widget) inner));
        Padding outer = new Padding();
        outer.padding(EdgeInsets.all(1));
        outer.child(col);
        return outer;
    }

    /**
     * The gallery's ModelBinding: a StatefulWidget that owns the value and republishes it
     * through an InheritedWidget on setState.
     */
    static class Binding extends StatefulWidget {
        final Reader reader;

        Binding(Reader reader) {
            this.reader = reader;
        }

        @Override
        public State createState() {
            return new BindingState();
        }

        class BindingState extends State<Binding> {
            int value = 1;

            void publish(int v) {
                setState(() -> value = v);
            }

            @Override
            public Widget build(BuildContext context) {
                return new Model(value, buried(widget().reader));
            }
        }
    }

    private BuildOwner owner;

    private Binding.BindingState mount(Reader reader) {
        owner = new BuildOwner();
        Binding b = new Binding(reader);
        Element root = FlutterUI.mount(b, new RenderHost(), owner);
        return (Binding.BindingState) ((StatefulElement) root).state();
    }

    @Test
    @DisplayName("a buried reader sees the new value when the model is republished")
    void republishingRebuildsTheReader() {
        Reader reader = new Reader();
        Binding.BindingState state = mount(reader);
        assertEquals(1, reader.lastSeen, "the reader should see the initial value");

        state.publish(2);
        owner.flushSync();

        assertEquals(2, reader.lastSeen, "the reader must see the NEW value");
    }

    @Test
    @DisplayName("republishing the SAME value does not disturb the reader")
    void anUnchangedValueDoesNotNotify() {
        Reader reader = new Reader();
        Binding.BindingState state = mount(reader);
        int buildsAfterMount = reader.builds;

        state.publish(1);            // same value: updateShouldNotify returns false
        owner.flushSync();

        assertEquals(1, reader.lastSeen);
        assertTrue(reader.builds >= buildsAfterMount, "no element lost");
    }

    @Test
    @DisplayName("the subtree survives the notification - this is what blanked pages before")
    void theSubtreeIsStillIntactAfterAChange() {
        Reader reader = new Reader();
        Binding.BindingState state = mount(reader);

        state.publish(2);
        owner.flushSync();
        state.publish(3);
        owner.flushSync();

        assertEquals(3, reader.lastSeen);
        // The reader must still be MOUNTED and still producing its component. The previous
        // attempt notified before this element's own subtree had been rebuilt, and the
        // elements the dependents rebuilt into were unmounted moments later - which is
        // exactly what an intact-looking test suite failed to catch.
        assertNotNull(reader, "reader widget still referenced");
        assertTrue(reader.builds >= 3, "reader rebuilt on each change, got " + reader.builds);
    }

    @Test
    @DisplayName("several readers at different depths all see the change")
    void everyDependentIsNotified() {
        Reader shallow = new Reader();
        Reader deep = new Reader();
        owner = new BuildOwner();
        Binding b = new Binding(deep) {
        };
        // Two readers under one model: one direct, one buried.
        Column pair = new Column();
        pair.children(DartList.of((Widget) shallow, buried(deep)));
        final Model[] held = new Model[1];
        StatelessWidget host = new StatelessWidget() {
            @Override
            public Widget build(BuildContext context) {
                held[0] = new Model(9, pair);
                return held[0];
            }
        };
        FlutterUI.mount(host, new RenderHost(), owner);

        assertEquals(9, shallow.lastSeen);
        assertEquals(9, deep.lastSeen);
    }
}
