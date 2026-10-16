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

import com.codename1.flutter.Axis;
import com.codename1.flutter.Clip;
import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * Makes its child scrollable: the child subtree becomes a real CN1 scrollable container
 * boundary laid out with an unbounded main axis inside.
 *
 * <p>{@code scrollDirection} picks the axis. It defaults to vertical, as in Flutter, and
 * the horizontal case is what lets wide content — a data table with more columns than fit
 * a phone — be reached rather than crushed into the available width.</p>
 */
public class SingleChildScrollView extends Widget {

    private EdgeInsets padding;
    private Widget child;
    private String restorationId;
    private Clip clipBehavior;
    private Axis scrollDirection = Axis.vertical;

    public void restorationId(String v) {
        this.restorationId = v;
    }

    public void clipBehavior(Clip v) {
        this.clipBehavior = v;
    }

    public void scrollDirection(Axis v) {
        this.scrollDirection = v == null ? Axis.vertical : v;
    }

    public Axis getScrollDirection() {
        return scrollDirection;
    }

    public void padding(EdgeInsets v) {
        this.padding = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public EdgeInsets getPadding() {
        return padding;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Element createElement() {
        return new SingleChildScrollViewRenderElement(this);
    }
}
