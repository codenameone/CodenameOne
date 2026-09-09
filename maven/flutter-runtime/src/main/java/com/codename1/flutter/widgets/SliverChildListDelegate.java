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
import com.codename1.flutter.Widget;

import dart.core.DartList;

/**
 * A child delegate backed by an explicit list — Flutter's
 * {@code SliverChildListDelegate}.
 */
public class SliverChildListDelegate extends SliverChildDelegate {

    private final DartList<Widget> children;

    public SliverChildListDelegate(DartList<Widget> children) {
        this.children = children;
    }

    public void addAutomaticKeepAlives(boolean v) {
    }

    public void addRepaintBoundaries(boolean v) {
    }

    public void addSemanticIndexes(boolean v) {
    }

    @Override
    public DartList<Widget> buildChildren(BuildContext context) {
        return children != null ? children : new DartList<Widget>();
    }
}
