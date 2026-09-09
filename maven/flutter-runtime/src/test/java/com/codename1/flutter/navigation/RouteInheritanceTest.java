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

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.Element;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.InheritedValueProvider;
import com.codename1.flutter.MediaQuery;
import com.codename1.flutter.MediaQueryData;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.provider.SingleChildWidget;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * A route mounts as its own element-tree root (its own Form), but in Flutter a
 * route's page builds below the app and inherits everything above it. The
 * context fallback restores that: ancestor lookups continue from the context
 * that pushed the route.
 */
class RouteInheritanceTest {

    /** A value published to a subtree, like MaterialApp's localizations scope. */
    private static class Scope extends SingleChildWidget implements InheritedValueProvider {
        private final String value;

        Scope(String value) {
            this.value = value;
        }

        @Override
        public Object providedValueFor(Class<?> type) {
            return type == String.class ? value : null;
        }
    }

    /** Records what its context could see at build time. */
    private static class Probe extends StatelessWidget {
        String seen;
        Element context;

        @Override
        public Widget build(BuildContext context) {
            this.context = (Element) context;
            seen = (String) context.providerValueOfType(String.class);
            return new ProbeBox(1, 1);
        }
    }

    private static Element mountAppWithScope(String value, Probe inApp) {
        Scope scope = new Scope(value);
        scope.child(inApp);
        return FlutterUI.mount(scope, new RenderHost(), new BuildOwner());
    }

    @Test
    void aFreshRootSeesNothingFromTheAppTree() {
        Probe inApp = new Probe();
        mountAppWithScope("app-value", inApp);
        assertSame("app-value", inApp.seen, "sanity: the app's own subtree sees the scope");

        Probe orphan = new Probe();
        FlutterUI.mount(orphan, new RenderHost(), new BuildOwner());
        assertNull(orphan.seen, "an unlinked root has no ancestors to inherit from");
    }

    @Test
    void aRouteRootInheritsThroughTheContextThatPushedIt() {
        Probe inApp = new Probe();
        mountAppWithScope("app-value", inApp);

        // the element that would call Navigator.of(context).push(...)
        Element pusher = elementOf(inApp);

        Probe page = new Probe();
        FlutterUI.mount(page, new RenderHost(), new BuildOwner(), pusher);

        assertSame("app-value", page.seen,
                "the pushed page resolves the app's scope through the pushing context");
    }

    @Test
    void theFallbackIsOnlyForLookups_notStructure() {
        Probe inApp = new Probe();
        mountAppWithScope("app-value", inApp);

        Probe page = new Probe();
        Element root = FlutterUI.mount(page, new RenderHost(), new BuildOwner(), elementOf(inApp));

        assertNull(root.parent(),
                "a route root stays a genuine root — render/host logic must not see it as embedded");
    }

    /** The element built for {@code w}: the probe records its own context. */
    /// Records the top padding its context resolves, the way SafeArea does.
    private static class PaddingProbe extends StatelessWidget {
        double top = -1;
        Element context;

        @Override
        public Widget build(BuildContext context) {
            this.context = (Element) context;
            top = MediaQuery.of(context).padding().top();
            return new ProbeBox(1, 1);
        }
    }

    private static MediaQueryData withTopPadding(double top) {
        return new MediaQueryData(new com.codename1.flutter.rendering.Size(100, 100), 1.0, null, 1.0,
                EdgeInsets.only(0, top, 0, 0));
    }

    /// The defect this pins: a vertical scroll view removes the top padding FOR ITS
    /// DESCENDANTS, because it has already inset its own content by it. A route pushed
    /// from a row inside that list used to inherit from the row, so its SafeArea
    /// resolved to zero and the page drew under the status bar.
    @Test
    void aRoutePushedFromInsideAScrollViewStillSeesTheDisplayPadding() {
        PaddingProbe deep = new PaddingProbe();
        Navigator.RootScope scope = new Navigator.RootScope(
                MediaQuery.removePadding(null, Boolean.FALSE, Boolean.TRUE,
                        Boolean.FALSE, Boolean.TRUE, deep),
                false);
        FlutterUI.mount(MediaQuery.scope(withTopPadding(44), scope),
                new RenderHost(), new BuildOwner());

        assertEquals(0.0, deep.top,
                "sanity: inside the scroll view the padding is deliberately spent");

        PaddingProbe page = new PaddingProbe();
        FlutterUI.mount(page, new RenderHost(), new BuildOwner(),
                Navigator.pushingElement(deep.context));

        assertEquals(44.0, page.top,
                "a pushed route mounts under the navigator, above the scroll view's "
                        + "removePadding, so it insets for the status bar itself");
    }

    /// The nearest navigator wins. A study is a MaterialApp of its own wrapped in the
    /// providers it needs; anchoring its routes at the OUTERMOST app's scope climbs out
    /// of those providers and the page's first Provider.of comes back null.
    @Test
    void theNearestNavigatorScopeWins() {
        // The real nesting: each app publishes its scopes ABOVE the navigator position
        // it inserts, and the study's whole app is a descendant of the outer one.
        Probe deep = new Probe();
        Navigator.RootScope study = new Navigator.RootScope(deep, false);
        Navigator.RootScope app = new Navigator.RootScope(scoped("study-value", study), true);
        FlutterUI.mount(scoped("app-value", app), new RenderHost(), new BuildOwner());

        Element anchor = Navigator.pushingElement(deep.context);
        assertSame(study, anchor.widget(), "the study's own scope is the nearest navigator");

        Probe page = new Probe();
        FlutterUI.mount(page, new RenderHost(), new BuildOwner(), anchor);
        assertSame("study-value", page.seen,
                "the route still sees what the study wrapped its app in");
    }

    private static Widget scoped(String value, Widget child) {
        Scope s = new Scope(value);
        s.child(child);
        return s;
    }

    private static Element elementOf(Probe w) {
        return w.context;
    }
}
