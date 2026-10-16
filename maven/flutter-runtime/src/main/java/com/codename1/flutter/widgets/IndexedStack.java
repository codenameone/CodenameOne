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

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

import dart.core.DartList;

/**
 * Shows a single child of a stack by {@code index}, keeping the others in the
 * tree — Flutter's {@code IndexedStack}. See {@link IndexedStackRenderElement}
 * for how the unselected children are kept alive without being drawn.
 */
public class IndexedStack extends Widget {

    private Object alignment;
    private Object textDirection;
    private Object sizing;
    private long index;
    private DartList<Widget> children;

    public void alignment(Object v) { this.alignment = v; }
    public void textDirection(Object v) { this.textDirection = v; }
    public void sizing(Object v) { this.sizing = v; }
    public void index(long v) { this.index = v; }

    public void children(DartList<Widget> v) {
        this.children = v;
    }

    public long getIndex() {
        return index;
    }

    public DartList<Widget> getChildren() {
        return children;
    }

    @Override
    public Element createElement() {
        return new IndexedStackRenderElement(this, new SimpleChildrenRenderElement.Children() {
            @Override
            public DartList<Widget> get() {
                return children;
            }
        });
    }
}
