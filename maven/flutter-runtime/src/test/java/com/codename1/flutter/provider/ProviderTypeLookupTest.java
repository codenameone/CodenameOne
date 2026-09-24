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
package com.codename1.flutter.provider;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;

import dart.runtime.Funcs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A Consumer/Selector reads the model of the type it ASKED FOR, not the nearest one.
 *
 * <p>Java erases {@code Consumer<T>}, so the runtime takes the type as a token the transpiler
 * emits. Without it the lookup returned the nearest provided value of any type — right only
 * when a single model is in scope. Reply has its localizations and its EmailStore above the
 * same Selector: it got the localizations and the study came up blank — a cast error on the
 * desktop, and on iOS, where that cast is unchecked, a wrong object that flowed on until an
 * unrelated switch matched nothing.</p>
 */
class ProviderTypeLookupTest {

    static class Localizations {
    }

    static class EmailStore {
        final String name = "store";
    }

    /** Two models in scope with the WRONG one nearer — the Reply arrangement. */
    private Widget twoProviders(Widget leaf) {
        Provider inner = new Provider();
        inner.value(new Localizations());
        inner.child(leaf);
        Provider outer = new Provider();
        outer.value(new EmailStore());
        outer.child(inner);
        return outer;
    }

    @Test
    @DisplayName("a Consumer that names its type skips the nearer, wrong model")
    void consumerFindsItsOwnType() {
        final Object[] seen = new Object[1];
        Consumer<EmailStore> c = new Consumer<EmailStore>();
        c.providedType(EmailStore.class);
        c.builder(new Funcs.Func3<BuildContext, EmailStore, Widget, Widget>() {
            @Override
            public Widget call(BuildContext context, EmailStore value, Widget child) {
                seen[0] = value;
                return new ProbeBox(1, 1);
            }
        });

        FlutterUI.mount(twoProviders(c), new RenderHost(), new BuildOwner());

        assertTrue(seen[0] instanceof EmailStore, "expected the EmailStore, got " + seen[0]);
    }

    @Test
    @DisplayName("a Selector that names its type skips the nearer, wrong model")
    void selectorFindsItsOwnType() {
        final Object[] seen = new Object[1];
        Selector<EmailStore, String> s = new Selector<EmailStore, String>();
        s.providedType(EmailStore.class);
        s.selector(new Funcs.Func2<BuildContext, EmailStore, String>() {
            @Override
            public String call(BuildContext context, EmailStore store) {
                seen[0] = store;
                return store.name;
            }
        });
        s.builder(new Funcs.Func3<BuildContext, String, Widget, Widget>() {
            @Override
            public Widget call(BuildContext context, String value, Widget child) {
                return new ProbeBox(1, 1);
            }
        });

        FlutterUI.mount(twoProviders(s), new RenderHost(), new BuildOwner());

        assertTrue(seen[0] instanceof EmailStore, "expected the EmailStore, got " + seen[0]);
    }

    @Test
    @DisplayName("with no type named it still takes the nearest - the old behaviour")
    void anUnnamedTypeTakesTheNearest() {
        final Object[] seen = new Object[1];
        Consumer<Object> c = new Consumer<Object>();
        c.builder(new Funcs.Func3<BuildContext, Object, Widget, Widget>() {
            @Override
            public Widget call(BuildContext context, Object value, Widget child) {
                seen[0] = value;
                return new ProbeBox(1, 1);
            }
        });

        FlutterUI.mount(twoProviders(c), new RenderHost(), new BuildOwner());

        assertTrue(seen[0] instanceof Localizations,
                "the default is still nearest-wins, got " + seen[0]);
    }

    /** A model a ChangeNotifierProvider listens to. */
    static class Model implements com.codename1.flutter.foundation.ChangeNotifier {
    }

    /** A Consumer of {@code type} recording the value each build receives. */
    private static <T> Consumer<T> recorder(Class<T> type, final Object[] seen) {
        Consumer<T> c = new Consumer<T>();
        c.providedType(type);
        c.builder(new Funcs.Func3<BuildContext, T, Widget, Widget>() {
            @Override
            public Widget call(BuildContext context, T value, Widget child) {
                seen[0] = value;
                return new ProbeBox(1, 1);
            }
        });
        return c;
    }

    @Test
    @DisplayName("a Provider given a new value rebuilds its Consumer when the child is the same instance")
    void aReplacedProviderValueReachesTheConsumer() {
        Object[] seen = new Object[1];
        Consumer<EmailStore> c = recorder(EmailStore.class, seen);
        EmailStore second = new EmailStore();
        Provider before = new Provider();
        before.value(new EmailStore());
        before.child(c);
        BuildOwner owner = new BuildOwner();
        com.codename1.flutter.Element root = FlutterUI.mount(before, new RenderHost(), owner);
        Provider after = new Provider();
        after.value(second);
        after.child(c);
        root.update(after);
        owner.flushSync();
        assertTrue(seen[0] == second, "the Consumer shows the new value, got " + seen[0]);
    }

    @Test
    @DisplayName("a ChangeNotifierProvider given a new model rebuilds its Consumer when the child is the same instance")
    void aReplacedNotifierReachesTheConsumer() {
        Object[] seen = new Object[1];
        Consumer<Model> c = recorder(Model.class, seen);
        Model second = new Model();
        ChangeNotifierProvider before = new ChangeNotifierProvider();
        before.value(new Model());
        before.child(c);
        BuildOwner owner = new BuildOwner();
        com.codename1.flutter.Element root = FlutterUI.mount(before, new RenderHost(), owner);
        ChangeNotifierProvider after = new ChangeNotifierProvider();
        after.value(second);
        after.child(c);
        root.update(after);
        owner.flushSync();
        assertTrue(seen[0] == second, "the Consumer shows the new model, got " + seen[0]);
    }
}
