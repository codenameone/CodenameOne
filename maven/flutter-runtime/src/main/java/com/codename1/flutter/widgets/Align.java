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

/**
 * Aligns its child within itself per an {@link Alignment} (default center).
 * Expands to the incoming constraints when they are bounded, otherwise sizes
 * to the child.
 */
/// Implements {@link HasChild} so the widget walkers can see THROUGH it.
/// A button consumes its content rather than mounting it, and the walk that finds that
/// content stops at any wrapper it cannot open: Shrine's login buttons wrap their label
/// in a Padding, and both rendered with no label at all -- the row collapsed to a blob
/// where the reference reads CANCEL and NEXT.
public class Align extends Widget implements HasChild {

    private Alignment alignment;
    private Widget child;
    private Double widthFactor;
    private Double heightFactor;

    public void widthFactor(Double v) {
        this.widthFactor = v;
    }

    public void heightFactor(Double v) {
        this.heightFactor = v;
    }

    public Double getWidthFactor() {
        return widthFactor;
    }

    public Double getHeightFactor() {
        return heightFactor;
    }

    public void alignment(Alignment v) {
        this.alignment = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Alignment getAlignment() {
        return alignment;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Element createElement() {
        return new AlignRenderElement(this);
    }
}
