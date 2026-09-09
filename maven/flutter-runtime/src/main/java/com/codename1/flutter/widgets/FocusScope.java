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
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * A focus container that groups its subtree into a focus scope — Flutter's
 * {@code FocusScope}. This milestone renders the {@code child} through
 * unchanged; scope-based focus traversal is deferred, so the node and focus
 * flags are captured only for API shape.
 */
public class FocusScope extends StatelessWidget {

    private FocusScopeNode node;
    private Widget child;

    public void node(FocusScopeNode v) {
        this.node = v;
    }

    public void autofocus(boolean v) {
    }

    public void onFocusChange(Object v) {
    }

    public void canRequestFocus(boolean v) {
    }

    public void skipTraversal(boolean v) {
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Widget getChild() {
        return child;
    }

    /** Dart's {@code FocusScope.of(context)} — the nearest enclosing scope node. */
    public static FocusScopeNode of(BuildContext context) {
        return new FocusScopeNode();
    }

    @Override
    public Widget build(BuildContext context) {
        return child;
    }
}
