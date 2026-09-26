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

import com.codename1.flutter.foundation.ChangeNotifier;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;
import com.codename1.flutter.widgets.InheritedWidget;

import dart.runtime.Funcs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A nested inherited widget is visible to its first children even under a non-empty
/// inherited map; a dependent State hears didChangeDependencies; and a ChangeNotifier's
/// listener entry goes when its last listener does.
class Round6InheritedTest {

    static final class Outer extends InheritedWidget {
        Outer(Widget child) {
            child(child);
        }
    }

    static final class Inner extends InheritedWidget {
        final int value;

        Inner(int value, Widget child) {
            this.value = value;
            child(child);
        }

        @Override
        public boolean updateShouldNotify(InheritedWidget old) {
            return ((Inner) old).value != value;
        }
    }

    static final class Reader extends StatelessWidget {
        Inner seen;

        @Override
        public Widget build(BuildContext context) {
            seen = context.dependOnInheritedWidgetOfExactType(Inner.class);
            return new ProbeBox(1, 1);
        }
    }

    /**
     * Builds Outer(first nothing, then Inner(reader)). Outer has published by the time it
     * is rebuilt, so the Inner it then mounts starts from a NON-EMPTY inherited map --
     * what every subtree in a real app has. On an initial mount the maps are still empty,
     * lookups fall back to walking ancestors, and the publication order never shows.
     */
    static final class Reveal extends StatefulWidget {
        final Reader reader;

        Reveal(Reader reader) {
            this.reader = reader;
        }

        @Override
        public State createState() {
            return new RevealState();
        }
    }

    static final class RevealState extends State<Reveal> {
        boolean show;

        void reveal() {
            setState(() -> show = true);
        }

        @Override
        public Widget build(BuildContext context) {
            return new Outer(show ? new Inner(5, widget().reader) : new ProbeBox(1, 1));
        }
    }

    @Test
    void aNestedInheritedWidgetIsSeenByItsFirstChildren() {
        Reader r = new Reader();
        BuildOwner owner = new BuildOwner();
        Element root = FlutterUI.mount(new Reveal(r), new RenderHost(), owner);
        ((RevealState) ((StatefulElement) root).state()).reveal();
        owner.flushSync();
        assertTrue(r.seen != null, "the reader must find the Inner it was mounted under");
        assertEquals(5, r.seen.value);
    }

    static final class Host extends StatefulWidget {
        @Override
        public State createState() {
            return new HostState();
        }
    }

    static final class HostState extends State<Host> {
        int value = 1;
        final Dependent dependent = new Dependent();

        void publish(int v) {
            setState(() -> value = v);
        }

        @Override
        public Widget build(BuildContext context) {
            return new Inner(value, dependent);
        }
    }

    static final class Dependent extends StatefulWidget {
        static int changes;

        @Override
        public State createState() {
            return new State<Dependent>() {
                @Override
                public void didChangeDependencies() {
                    changes++;
                }

                @Override
                public Widget build(BuildContext context) {
                    context.dependOnInheritedWidgetOfExactType(Inner.class);
                    return new ProbeBox(1, 1);
                }
            };
        }
    }

    @Test
    void aDependentStateHearsDidChangeDependencies() {
        Dependent.changes = 0;
        BuildOwner owner = new BuildOwner();
        Element root = FlutterUI.mount(new Host(), new RenderHost(), owner);
        assertEquals(1, Dependent.changes, "once after initState, as in Flutter");
        ((HostState) ((StatefulElement) root).state()).publish(2);
        owner.flushSync();
        assertEquals(2, Dependent.changes, "and again when the inherited widget it read changed");
    }

    static final class Model implements ChangeNotifier {
    }

    @Test
    void aNotifiersEntryGoesWithItsLastListener() {
        Model m = new Model();
        Funcs.VoidFunc0 l = new Funcs.VoidFunc0() {
            @Override
            public void call() {
            }
        };
        m.notifyListeners();
        assertFalse(m.hasListeners(), "notifying with no listeners creates nothing");
        m.addListener(l);
        assertTrue(m.hasListeners());
        m.removeListener(l);
        assertFalse(m.hasListeners(), "and removing the last listener removes the entry");
    }
}
