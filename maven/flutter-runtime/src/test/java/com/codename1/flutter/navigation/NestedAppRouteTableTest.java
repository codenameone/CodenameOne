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
package com.codename1.flutter.navigation;

import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.Element;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.Widget;
import com.codename1.flutter.material.MaterialApp;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A route table belongs to the app that published it, not to the process.
 *
 * <p>The gallery's studies are each a whole {@code MaterialApp}. While the
 * runtime kept ONE global table, opening a study replaced the gallery's table
 * with the study's — and popping back never restored it, because the outer app
 * does not rebuild. Everything after that first study visit resolved against
 * the wrong app: {@code /shrine} answered "no route", and re-entering a study
 * mounted its page above the wrong Theme, MediaQuery and Localizations, where
 * {@code GalleryLocalizations.of(context)!} is null.</p>
 */
class NestedAppRouteTableTest {

    @BeforeEach
    @AfterEach
    void reset() {
        Navigator.reset();
        Navigator.resetRouteTable();
    }

    private static MaterialPageRoute pageNamed(String name) {
        MaterialPageRoute r = new MaterialPageRoute();
        r.builder((context) -> new ProbeBox(10, 10));
        RouteSettings s = new RouteSettings();
        s.name(name);
        r.settings(s);
        return r;
    }

    /** An app that answers exactly one route name. */
    private static MaterialApp appServing(String name, Widget home) {
        MaterialApp app = new MaterialApp();
        app.home(home);
        app.onGenerateRoute(settings ->
                name.equals(settings.name()) ? pageNamed(name) : null);
        return app;
    }

    private static Element mount(Widget root) {
        return FlutterUI.mount(root, new RenderHost(), new BuildOwner());
    }

    @Test
    @DisplayName("an inner app does not take over the outer app's routes")
    void innerAppDoesNotClobberOuter() {
        MaterialApp inner = appServing("/study/detail", new ProbeBox(10, 10));
        mount(appServing("/gallery/demo", inner));

        // The inner app built last. Without per-app tables this was null.
        assertNotNull(Navigator.resolveRoute("/gallery/demo", null),
                "a context-less push resolves against the OUTER app");
        assertNull(Navigator.resolveRoute("/study/detail", null),
                "the inner app's private route is not reachable from the root");
    }

    @Test
    @DisplayName("a push from inside the inner app sees both tables")
    void innerContextSeesItsOwnRoutesThenTheRoot() {
        MaterialApp inner = appServing("/study/detail", new ProbeBox(10, 10));
        Element root = mount(appServing("/gallery/demo", inner));

        Element innerElement = find(root, inner);
        assertNotNull(innerElement, "the inner app is mounted");

        assertNotNull(Navigator.resolveRoute(innerElement, "/study/detail", null),
                "its own route resolves");
        assertNotNull(Navigator.resolveRoute(innerElement, "/gallery/demo", null),
                "and a name it has never heard of falls through to the root app");
    }

    /** The element whose widget is {@code widget}, or null. */
    private static Element find(Element from, Widget widget) {
        if (from.widget() == widget) {
            return from;
        }
        final Element[] found = {null};
        from.visitChildren(child -> {
            if (found[0] == null) {
                found[0] = find(child, widget);
            }
        });
        return found[0];
    }
}
