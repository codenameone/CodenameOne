/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.desktopcompat.java.awt;

/// The space a container leaves free along each of its four edges.
public class Insets implements Cloneable {

    public int top;

    public int left;

    public int bottom;

    public int right;

    public Insets(int top, int left, int bottom, int right) {
        this.top = top;
        this.left = left;
        this.bottom = bottom;
        this.right = right;
    }

    public void set(int top, int left, int bottom, int right) {
        this.top = top;
        this.left = left;
        this.bottom = bottom;
        this.right = right;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Insets) {
            Insets i = (Insets) obj;
            return top == i.top && left == i.left && bottom == i.bottom && right == i.right;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return ((top * 31 + left) * 31 + bottom) * 31 + right;
    }

    @Override
    public String toString() {
        return "java.awt.Insets[top=" + top + ",left=" + left + ",bottom=" + bottom + ",right=" + right + "]";
    }

    @Override
    public Object clone() {
        return new Insets(top, left, bottom, right);
    }
}
