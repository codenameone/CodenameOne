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
import com.codename1.flutter.Element;
import com.codename1.flutter.FlutterAssets;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.InheritedValueProvider;
import com.codename1.flutter.State;
import com.codename1.flutter.StatefulElement;
import com.codename1.flutter.StatefulWidget;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.navigation.MaterialPageRoute;
import com.codename1.flutter.navigation.Navigator;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;

import dart.async.Completer;
import dart.async.Future;
import dart.runtime.Funcs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// FutureBuilder delivers its future, a pushed route answers its pop result, an
/// unmounted reader leaves its inherited widget, and read() does not subscribe.
class AsyncAndDependencyTest {

    private BuildOwner owner;

    private Element mount(Widget root) {
        owner = new BuildOwner();
        return FlutterUI.mount(root, new RenderHost(), owner);
    }

    // --- FutureBuilder ---------------------------------------------------------

    private static FutureBuilder<Object> futureBuilder(Object future, final AsyncSnapshot[] seen) {
        FutureBuilder<Object> fb = new FutureBuilder<Object>();
        fb.future(future);
        fb.builder(new Funcs.Func2<BuildContext, AsyncSnapshot, Widget>() {
            @Override
            public Widget call(BuildContext context, AsyncSnapshot snapshot) {
                seen[0] = snapshot;
                return new ProbeBox(1, 1);
            }
        });
        return fb;
    }

    @Test
    void aFutureBuilderWaitsAndThenDeliversTheValue() {
        Completer<Object> c = new Completer<Object>();
        AsyncSnapshot[] seen = new AsyncSnapshot[1];
        mount(futureBuilder(c.future(), seen));
        assertEquals(ConnectionState.waiting, seen[0].connectionState());

        c.complete("loaded");
        owner.flushSync();
        assertEquals(ConnectionState.done, seen[0].connectionState(), "the result must reach the builder");
        assertEquals("loaded", seen[0].data());
    }

    @Test
    void aFutureBuilderDeliversTheError() {
        Completer<Object> c = new Completer<Object>();
        AsyncSnapshot[] seen = new AsyncSnapshot[1];
        mount(futureBuilder(c.future(), seen));
        IllegalStateException boom = new IllegalStateException("boom");
        c.completeError(boom);
        owner.flushSync();
        assertEquals(ConnectionState.done, seen[0].connectionState());
        assertSame(boom, seen[0].error());
    }

    @Test
    void anUnmountedFutureBuilderIgnoresALateResult() {
        Completer<Object> c = new Completer<Object>();
        AsyncSnapshot[] seen = new AsyncSnapshot[1];
        Element root = mount(futureBuilder(c.future(), seen));
        FlutterUI.unmountTree(root);
        c.complete("late");
        owner.flushSync();
        assertEquals(ConnectionState.waiting, seen[0].connectionState(), "no build after unmount");
    }

    // --- Navigator results -----------------------------------------------------

    @Test
    void aPushedRouteCompletesWithItsPopResult() {
        Navigator.reset();
        MaterialPageRoute r = new MaterialPageRoute();
        r.builder((context) -> new ProbeBox(1, 1));
        Future<Object> result = Navigator.push(null, r);
        final Object[] got = {"not yet"};
        result.then(new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object v) {
                got[0] = v;
            }
        });
        assertEquals("not yet", got[0], "nothing until the route is popped");
        Navigator.pop(null, "picked");
        assertEquals("picked", got[0]);
    }

    @Test
    void aStatesPushNamedAnswersAFutureEvenForAMissingRoute() {
        Navigator.reset();
        Navigator.resetRouteTable();
        Object f = Navigator.of(null, null).pushNamed("/nowhere", null);
        assertTrue(f instanceof Future, "never null, or `await` has nothing to wait on");
    }

    // --- dependencies ----------------------------------------------------------

    static final class Model extends InheritedWidget {
        static InheritedElement element;

        Model(Widget child) {
            child(child);
        }

        @Override
        public Element createElement() {
            element = (InheritedElement) super.createElement();
            return element;
        }

        /// Never notifies, like a Theme that stays put: a notification prunes
        /// unmounted readers as it goes, which would hide a missing unregister.
        @Override
        public boolean updateShouldNotify(InheritedWidget oldWidget) {
            return false;
        }
    }

    static final class Reader extends StatelessWidget {
        @Override
        public Widget build(BuildContext context) {
            context.dependOnInheritedWidgetOfExactType(Model.class);
            return new ProbeBox(1, 1);
        }
    }

    static final class Toggle extends StatefulWidget {
        @Override
        public State createState() {
            return new ToggleState();
        }
    }

    static final class ToggleState extends State<Toggle> {
        boolean showReader = true;

        void hide() {
            setState(() -> showReader = false);
        }

        @Override
        public Widget build(BuildContext context) {
            return new Model(showReader ? new Reader() : new ProbeBox(1, 1));
        }
    }

    @Test
    void anUnmountedReaderLeavesItsInheritedWidget() {
        Element root = mount(new Toggle());
        assertEquals(1, Model.element.dependentCount());
        ((ToggleState) ((StatefulElement) root).state()).hide();
        owner.flushSync();
        assertEquals(0, Model.element.dependentCount(),
                "the popped reader must not stay reachable from a long-lived inherited widget");
    }

    static final class ValueHost extends StatelessWidget implements InheritedValueProvider {
        final Widget child;

        ValueHost(Widget child) {
            this.child = child;
        }

        @Override
        public Object providedValueFor(Class<?> type) {
            return type == String.class ? "model" : null;
        }

        @Override
        public Widget build(BuildContext context) {
            return child;
        }
    }

    static final class Consumer extends StatelessWidget {
        final boolean watch;
        int builds;

        Consumer(boolean watch) {
            this.watch = watch;
        }

        @Override
        public Widget build(BuildContext context) {
            builds++;
            if (watch) {
                context.watch(String.class);
            } else {
                context.read(String.class);
            }
            return new ProbeBox(1, 1);
        }
    }

    @Test
    void readDoesNotSubscribeAndWatchDoes() {
        Consumer reader = new Consumer(false);
        Element root = mount(new ValueHost(reader));
        root.rebuildProviderDependents();
        owner.flushSync();
        assertEquals(1, reader.builds, "context.read must not rebuild on a notification");

        Consumer watcher = new Consumer(true);
        root = mount(new ValueHost(watcher));
        root.rebuildProviderDependents();
        owner.flushSync();
        assertEquals(2, watcher.builds, "context.watch does");
    }
}
