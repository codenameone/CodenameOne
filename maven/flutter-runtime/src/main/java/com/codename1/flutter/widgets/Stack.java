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

import com.codename1.flutter.Alignment;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

import dart.core.DartList;

/**
 * Overlaps its children: non-positioned children are placed by the stack's
 * alignment, {@link Positioned} children resolve their insets against the
 * stack bounds. Later children paint on top (z-order = child order).
 */
public class Stack extends Widget {

    private Alignment alignment;
    private DartList<Widget> children;
    private com.codename1.flutter.StackFit fit;
    private com.codename1.flutter.Clip clipBehavior;

    public void fit(com.codename1.flutter.StackFit v) {
        this.fit = v;
    }

    public void clipBehavior(com.codename1.flutter.Clip v) {
        this.clipBehavior = v;
    }

    public void alignment(Alignment v) {
        this.alignment = v;
    }

    public void children(DartList<Widget> v) {
        this.children = v;
    }

    public Alignment getAlignment() {
        return alignment;
    }

    public DartList<Widget> getChildren() {
        return children;
    }

    /** How non-positioned children are sized; {@code loose} when unset, as in Flutter. */
    public com.codename1.flutter.StackFit getFit() {
        return fit == null ? com.codename1.flutter.StackFit.loose : fit;
    }

    @Override
    public Element createElement() {
        return new StackRenderElement(this);
    }
}
