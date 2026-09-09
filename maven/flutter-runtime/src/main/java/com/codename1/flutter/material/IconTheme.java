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
import com.codename1.flutter.Key;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * Establishes an ambient {@link IconThemeData} for its subtree — Flutter's
 * {@code IconTheme}. Descendant {@code Icon}s read {@code IconTheme.of(context)}
 * for their default size/color. This pass hosts the {@code child} and records
 * the data; wiring the value into the inherited-widget lookup is deferred, so
 * {@link #of(BuildContext)} returns a fresh default.
 */
public class IconTheme extends StatelessWidget {

    private IconThemeData data;
    private Widget child;

    public void data(IconThemeData v) {
        this.data = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public IconThemeData getData() {
        return data;
    }

    public Widget getChild() {
        return child;
    }

    /** Dart's {@code IconTheme.of(context)}: the ambient icon theme. */
    public static IconThemeData of(BuildContext context) {
        return new IconThemeData();
    }

    /** Dart's {@code IconTheme.merge(...)} named constructor. */
    public static IconTheme merge(Key key, IconThemeData data, Widget child) {
        IconTheme t = new IconTheme();
        t.key(key);
        t.data(data);
        t.child(child);
        return t;
    }

    @Override
    public Widget build(BuildContext context) {
        return child;
    }
}
