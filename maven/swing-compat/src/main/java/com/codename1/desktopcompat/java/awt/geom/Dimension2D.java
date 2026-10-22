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
package com.codename1.desktopcompat.java.awt.geom;

/// A width and a height, stored by a subclass in the precision it chooses.
public abstract class Dimension2D implements Cloneable {

    /// The copy handed out for a subclass that does not override `clone()`.
    private static final class Copy extends Dimension2D {

        private double width;
        private double height;

        Copy(double width, double height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public double getWidth() {
            return width;
        }

        @Override
        public double getHeight() {
            return height;
        }

        @Override
        public void setSize(double width, double height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public Object clone() {
            return new Copy(width, height);
        }
    }

    protected Dimension2D() {
    }

    public abstract double getWidth();

    public abstract double getHeight();

    public abstract void setSize(double width, double height);

    public void setSize(Dimension2D d) {
        setSize(d.getWidth(), d.getHeight());
    }

    /// Returns a copy of this size. A subclass that does not override it gets
    /// a plain double precision size back rather than its own type.
    @Override
    public Object clone() {
        return new Copy(getWidth(), getHeight());
    }
}
