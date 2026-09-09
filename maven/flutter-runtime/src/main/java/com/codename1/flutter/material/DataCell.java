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

import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * One cell in a {@link DataRow} — Flutter's {@code DataCell}. Wraps the cell's
 * {@code child} widget.
 */
public class DataCell {

    private final Widget child;
    private boolean placeholder;
    private boolean showEditIcon;
    private Funcs.VoidFunc0 onTap;

    public DataCell(Widget child) {
        this.child = child;
    }

    public void placeholder(boolean v) {
        this.placeholder = v;
    }

    public void showEditIcon(boolean v) {
        this.showEditIcon = v;
    }

    public void onTap(Funcs.VoidFunc0 v) {
        this.onTap = v;
    }

    public void onLongPress(Funcs.VoidFunc0 v) {
    }

    public void onTapDown(Object v) {
    }

    public Widget getChild() {
        return child;
    }
}
