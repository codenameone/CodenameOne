/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codenameone.developerguide.flutter;

import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.widgets.Text;
import com.codename1.ui.Button;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;

/**
 * The developer guide's Flutter interop examples, compiled rather than taken
 * on trust.
 *
 * <p>Both entry points are shown with a {@code Text} widget standing in for a
 * real one, because the point is where the boundary sits, not what the widget
 * draws.</p>
 */
public final class FlutterInteropSnippets {

    private FlutterInteropSnippets() {
    }

    /** One Dart screen inside an otherwise ordinary Codename One form. */
    public static void embedOneScreen() {
        // tag::flutter-interop-java-001[]
        Form dashboard = new Form("Dashboard", BoxLayout.y());
        dashboard.add(new Label("Written in Codename One"));

        // A Dart widget tree, compiled to Java at build time. wrap() returns
        // an ordinary Container, so it is added like any other component.
        dashboard.add(FlutterUI.wrap(new Text("Rendered from Dart")));

        dashboard.add(new Button("Also Codename One"));
        dashboard.show();
        // end::flutter-interop-java-001[]
    }

    /** The widget tree IS the application. */
    public static void runTheWholeApplication() {
        // tag::flutter-interop-java-002[]
        // runApp mounts the tree in a Form of its own and shows it. Unlike
        // wrap(), this installs the Material base theme, because the widget
        // tree is expected to be the whole UI.
        FlutterUI.runApp(new Text("The entire application"));
        // end::flutter-interop-java-002[]
    }
}
