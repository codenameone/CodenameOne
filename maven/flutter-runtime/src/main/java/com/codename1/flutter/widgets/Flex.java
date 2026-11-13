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

import com.codename1.flutter.CrossAxisAlignment;
import com.codename1.flutter.Element;
import com.codename1.flutter.MainAxisAlignment;
import com.codename1.flutter.MainAxisSize;
import com.codename1.flutter.Widget;

import dart.core.DartList;

/**
 * Shared configuration of {@link Column} and {@link Row}.
 */
public abstract class Flex extends Widget {

    private DartList<Widget> children;
    private MainAxisAlignment mainAxisAlignment = MainAxisAlignment.start;
    private CrossAxisAlignment crossAxisAlignment = CrossAxisAlignment.center;
    private MainAxisSize mainAxisSize = MainAxisSize.max;

    public void children(DartList<Widget> v) {
        this.children = v;
    }

    public void mainAxisAlignment(MainAxisAlignment v) {
        this.mainAxisAlignment = v == null ? MainAxisAlignment.start : v;
    }

    public void crossAxisAlignment(CrossAxisAlignment v) {
        this.crossAxisAlignment = v == null ? CrossAxisAlignment.center : v;
    }

    public void mainAxisSize(MainAxisSize v) {
        this.mainAxisSize = v == null ? MainAxisSize.max : v;
    }

    private com.codename1.flutter.VerticalDirection verticalDirection =
            com.codename1.flutter.VerticalDirection.down;

    /**
     * Flutter's {@code verticalDirection}. {@code up} lays a Column out bottom to top,
     * which is the reversed children with start and end swapped; on a Row it swaps the
     * cross axis's start and end.
     */
    public void verticalDirection(com.codename1.flutter.VerticalDirection v) {
        this.verticalDirection = v == null ? com.codename1.flutter.VerticalDirection.down : v;
    }

    private boolean up() {
        return verticalDirection == com.codename1.flutter.VerticalDirection.up;
    }

    public DartList<Widget> getChildren() {
        if (!up() || !isVertical() || children == null) {
            return children;
        }
        // Built on every read, never cached: a list kept in a field and changed in place
        // would otherwise show its old order.
        DartList<Widget> r = new DartList<Widget>(dart.runtime.TypeWitness.of(Widget.class, false));
        for (int i = children.size() - 1; i >= 0; i--) {
            r.add(children.get(i));
        }
        return r;
    }

    private com.codename1.flutter.TextBaseline textBaseline;

    /** The baseline {@code CrossAxisAlignment.baseline} aligns on. */
    public void textBaseline(com.codename1.flutter.TextBaseline v) {
        this.textBaseline = v;
    }

    public com.codename1.flutter.TextBaseline textBaseline() {
        return textBaseline;
    }

    public MainAxisAlignment getMainAxisAlignment() {
        if (up() && isVertical()) {
            if (mainAxisAlignment == MainAxisAlignment.start) {
                return MainAxisAlignment.end;
            }
            if (mainAxisAlignment == MainAxisAlignment.end) {
                return MainAxisAlignment.start;
            }
        }
        return mainAxisAlignment;
    }

    public CrossAxisAlignment getCrossAxisAlignment() {
        if (up() && !isVertical()) {
            if (crossAxisAlignment == CrossAxisAlignment.start) {
                return CrossAxisAlignment.end;
            }
            if (crossAxisAlignment == CrossAxisAlignment.end) {
                return CrossAxisAlignment.start;
            }
        }
        return crossAxisAlignment;
    }

    public MainAxisSize getMainAxisSize() {
        return mainAxisSize;
    }

    /**
     * True for Column (vertical main axis), false for Row.
     */
    public abstract boolean isVertical();

    @Override
    public Element createElement() {
        return new FlexRenderElement(this);
    }
}
