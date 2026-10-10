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
package com.codename1.fxcompat;

import com.codename1.fxcompat.runtime.PeerPaint;

import javafx.scene.shape.Rectangle;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/// A node's matrix as the matrix a peer installs: in pixels, about the
/// place the peer is drawn at.
public class DeviceMatrixTest {

    private static double[] map(double[] d, double x, double y) {
        return new double[] {d[0] * x + d[2] * y + d[4], d[1] * x + d[3] * y + d[5]};
    }

    @Test
    public void aScaleAboutTheCentreKeepsTheCentreWhereverThePeerIsDrawn() {
        Rectangle r = new Rectangle(10, 10);
        r.setScaleX(2);
        r.setScaleY(2);
        double[] m = r.cn1PaintMatrix();
        // Three pixels to the unit, the peer's corner drawn at (100, 200).
        double[] d = PeerPaint.deviceMatrix(m, 0, 0, 3, 100, 200);
        double[] centre = map(d, 115, 215);
        assertEquals(115, centre[0], 1e-9);
        assertEquals(215, centre[1], 1e-9);
        double[] corner = map(d, 100, 200);
        assertEquals(85, corner[0], 1e-9);
        assertEquals(185, corner[1], 1e-9);
        // The same node drawn elsewhere: the same picture, moved.
        double[] moved = PeerPaint.deviceMatrix(m, 0, 0, 3, 400, 50);
        double[] movedCorner = map(moved, 400, 50);
        assertEquals(385, movedCorner[0], 1e-9);
        assertEquals(35, movedCorner[1], 1e-9);
    }

    /// A shape whose layout bounds do not start at zero is drawn with its
    /// bounds' corner at the peer's corner.
    @Test
    public void theOriginOfTheLayoutBoundsIsThePeersCorner() {
        Rectangle r = new Rectangle(20, 30, 10, 10);
        r.setScaleX(0.5);
        double[] m = r.cn1PaintMatrix();
        double[] d = PeerPaint.deviceMatrix(m, 20, 30, 1, 7, 9);
        // The centre of the rectangle, five pixels in, does not move.
        double[] centre = map(d, 12, 14);
        assertEquals(12, centre[0], 1e-9);
        assertEquals(14, centre[1], 1e-9);
        double[] corner = map(d, 7, 9);
        assertEquals(9.5, corner[0], 1e-9);
        assertEquals(9, corner[1], 1e-9);
    }
}
