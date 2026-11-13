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

/// Flutter's IntrinsicColumnWidth: a table column as wide as its widest cell.
/// The table lays such a column out with its flex, 1 unless given, which gives
/// the remaining width out in proportion rather than measuring every cell.
public class IntrinsicColumnWidth extends TableColumnWidth {
    private double flex = 1;

    public IntrinsicColumnWidth() {
    }

    public void flex(Double v) {
        this.flex = v == null ? 1 : v.doubleValue();
    }

    @Override
    public double flexFactor() {
        return flex;
    }

    @Override
    public double fixedWidth() {
        return -1;
    }
}
