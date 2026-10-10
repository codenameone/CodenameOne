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

    public DartList<Widget> getChildren() {
        return children;
    }

    public MainAxisAlignment getMainAxisAlignment() {
        return mainAxisAlignment;
    }

    public CrossAxisAlignment getCrossAxisAlignment() {
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
