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

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * A material list row: leading | (title above subtitle) | trailing, with a
 * 56lp minimum height, 16lp horizontal padding and an onTap callback.
 * Backed by a CN1 Container (UIID "FlutterListTile") plus a transparent tap
 * overlay.
 */
public class ListTile extends Widget {

    private Widget leading;
    private Widget title;
    private Widget subtitle;
    private Widget trailing;
    private Funcs.VoidFunc0 onTap;
    private boolean selected;

    public void selected(boolean v) {
        this.selected = v;
    }

    public void contentPadding(com.codename1.flutter.EdgeInsetsGeometry v) {
    }

    public void mouseCursor(Object v) {
    }

    public void dense(boolean v) {
    }

    public boolean getSelected() {
        return selected;
    }

    public void leading(Widget v) {
        this.leading = v;
    }

    public void title(Widget v) {
        this.title = v;
    }

    public void subtitle(Widget v) {
        this.subtitle = v;
    }

    public void trailing(Widget v) {
        this.trailing = v;
    }

    public void onTap(Funcs.VoidFunc0 v) {
        this.onTap = v;
    }

    public Widget getLeading() {
        return leading;
    }

    public Widget getTitle() {
        return title;
    }

    public Widget getSubtitle() {
        return subtitle;
    }

    public Widget getTrailing() {
        return trailing;
    }

    public Funcs.VoidFunc0 getOnTap() {
        return onTap;
    }

    @Override
    public Element createElement() {
        return new ListTileRenderElement(this);
    }
}
