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
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;

import dart.runtime.Funcs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A {@code Navigator} used AS A WIDGET (Reply's mail navigator, several Gallery
 * demos) rather than reached through {@code Navigator.of(context)} -- the
 * {@code initialRoute}/{@code onGenerateRoute} constructor surface the Dart stub
 * used to leave out entirely, so {@code Navigator(initialRoute: ...,
 * onGenerateRoute: ...)} failed to transpile with E0140 ("no parameter named
 * initialRoute") even though the Java class implements every one of these
 * setters. Mounted directly with {@link FlutterUI#mount}, headless, exactly
 * like {@link RouteInheritanceTest} -- no Display, no Form, just the element
 * tree -- which is enough to exercise {@link Navigator#build}.
 */
class NestedNavigatorWidgetTest {

    /** Records that it was actually built, the way the other Probe widgets here do. */
    private static final class BuiltFlag extends StatelessWidget {
        boolean built;

        @Override
        public Widget build(BuildContext context) {
            built = true;
            return new ProbeBox(1, 1);
        }
    }

    @Test
    void initialRouteAndOnGenerateRouteBuildTheFirstPage() {
        final BuiltFlag page = new BuiltFlag();
        final String[] seenRouteName = {null};

        Navigator nav = new Navigator();
        nav.initialRoute("/detail");
        nav.onGenerateRoute(new Funcs.Func1<RouteSettings, Object>() {
            @Override
            public Object call(RouteSettings settings) {
                seenRouteName[0] = settings.name();
                MaterialPageRoute<Object> r = new MaterialPageRoute<Object>();
                r.builder(new Funcs.Func1<BuildContext, Widget>() {
                    @Override
                    public Widget call(BuildContext context) {
                        return page;
                    }
                });
                r.settings(settings);
                return r;
            }
        });

        Element root = FlutterUI.mount(nav, new RenderHost(), new BuildOwner());

        assertEquals("/detail", seenRouteName[0],
                "onGenerateRoute must be called with the Navigator's own initialRoute");
        assertTrue(page.built, "the page onGenerateRoute built must actually mount");
        assertTrue(root.isMounted());
    }

    @Test
    void theFullConstructorSurfaceIsAcceptedWithoutThrowing() {
        // Flutter's full Navigator constructor -- restorationScopeId/observers/pages/
        // onPopPage are captured but not acted on by this runtime yet; what matters here
        // is that setting every one of them compiles and runs without throwing, the way
        // the Dart stub's constructor now declares them (flutter_material.dart).
        Navigator nav = new Navigator();
        nav.initialRoute("/");
        nav.onGenerateRoute(new Funcs.Func1<RouteSettings, Object>() {
            @Override
            public Object call(RouteSettings settings) {
                return null;
            }
        });
        nav.onUnknownRoute(new Funcs.Func1<RouteSettings, Object>() {
            @Override
            public Object call(RouteSettings settings) {
                return null;
            }
        });
        nav.restorationScopeId("scope");
        nav.observers(new Object());
        nav.pages(new Object());
        nav.onPopPage(new Object());

        FlutterUI.mount(nav, new RenderHost(), new BuildOwner());
    }
}
