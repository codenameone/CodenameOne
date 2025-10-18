/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.ui;

import com.codename1.ui.geom.Rectangle2D;

/**
 * An interface for providing custom painting such as gradients.
 *
 * @author shannah
 * @see Graphics#setColor(com.codename1.ui.Paint)
 * @since 7.0
 */
public interface Paint {

    /**
     * Paints in the given bounds.
     *
     * @param g      Graphics context to paint in.
     * @param bounds Bounds to paint in.  User coordinates.
     */
    void paint(Graphics g, Rectangle2D bounds);

    /**
     * Paints in the given bounds.
     *
     * @param g Graphics context to paint in.
     * @param x x coordinate.  User space.
     * @param y y coordinate.  USer space.
     * @param w width.  User space.
     * @param h Hight.  User space.
     */
    void paint(Graphics g, double x, double y, double w, double h);

}
