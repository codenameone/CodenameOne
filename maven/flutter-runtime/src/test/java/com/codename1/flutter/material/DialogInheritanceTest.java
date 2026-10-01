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
import com.codename1.flutter.Widget;
import com.codename1.flutter.navigation.Navigator;
import com.codename1.flutter.provider.Consumer;
import com.codename1.flutter.provider.Provider;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;

import dart.runtime.Funcs;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * A dialog mounts as a root of its own, yet in Flutter it is a route built under the
 * navigator: its builder's {@code Provider.of}, {@code Theme.of} and localizations see
 * the app above. Mounted with no context to continue from, they all came back null.
 */
class DialogInheritanceTest {

    static final class Store {
    }

    @BeforeEach
    @AfterEach
    void reset() {
        Navigator.reset();
        Dialogs.reset();
    }

    @Test
    @DisplayName("a dialog builder finds the provider above the context that showed it")
    void dialogBuilderSeesTheAppsProviders() {
        final Store store = new Store();
        final BuildContext[] showing = new BuildContext[1];
        Consumer<Object> capture = new Consumer<Object>();
        capture.builder(new Funcs.Func3<BuildContext, Object, Widget, Widget>() {
            @Override
            public Widget call(BuildContext context, Object value, Widget child) {
                showing[0] = context;
                return new ProbeBox(1, 1);
            }
        });
        Provider app = Provider.value(null, store, capture);
        FlutterUI.mount(app, new RenderHost(), new BuildOwner());

        final Object[] seen = {"builder never ran"};
        Dialogs.showDialog(showing[0], new Funcs.Func1<BuildContext, Widget>() {
            @Override
            public Widget call(BuildContext context) {
                seen[0] = Provider.of(context, false, Store.class);
                return new ProbeBox(5, 5);
            }
        });
        assertSame(store, seen[0], "Provider.of inside the dialog resolves the app's store");
    }
}
