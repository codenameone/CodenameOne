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

import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.Element;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;
import com.codename1.flutter.widgets.Column;

import dart.core.DartList;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The theme lives in the process-global UIManager, so a nested MaterialApp (a route
 * that builds its own) left ITS theme installed when it went away, and the outer page's
 * later rebuilds read the departed app's colours and type.
 */
class NestedAppThemeTest {

    private static MaterialApp app(ThemeData theme, Widget home) {
        MaterialApp a = new MaterialApp();
        a.theme(theme);
        a.home(home);
        return a;
    }

    @Test
    void removingANestedAppHandsTheThemeBackToTheEnclosingOne() {
        ThemeData outerTheme = new ThemeData();
        ThemeData innerTheme = new ThemeData();
        Column withNested = new Column();
        withNested.children(DartList.of((Widget) app(innerTheme, new ProbeBox(1, 1))));
        Element root = FlutterUI.mount(app(outerTheme, withNested), new RenderHost(), new BuildOwner());
        assertSame(innerTheme, MaterialAppElement.lastInstalledTheme, "the nested app installed its own");

        Column without = new Column();
        without.children(DartList.of((Widget) new ProbeBox(1, 1)));
        root.update(app(outerTheme, without));

        assertSame(outerTheme, MaterialAppElement.lastInstalledTheme,
                "the enclosing app's theme is back once the nested one is gone");
    }
}
