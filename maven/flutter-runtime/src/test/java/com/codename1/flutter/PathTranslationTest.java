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
package com.codename1.flutter;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@code Path.addPath}/{@code Path.shift} must translate (and, for {@code
 * addPath}'s {@code matrix4} overload, affine-transform) only a segment's
 * position operands -- never an arc's angle/radius/rotation, a conic's
 * weight, or a boolean flag. Before the fix, both methods summed every
 * {@code coords} entry in consecutive pairs regardless of what it meant,
 * and {@code addPath} did not translate at all.
 */
class PathTranslationTest {

    @Test
    void addPathTranslatesEverySegmentByTheOffset() {
        Path source = new Path();
        source.moveTo(1, 2);
        source.lineTo(3, 4);
        source.cubicTo(5, 6, 7, 8, 9, 10);
        source.quadraticBezierTo(11, 12, 13, 14);
        source.conicTo(15, 16, 17, 18, 0.5);
        source.arcTo(Rect.fromLTRB(20, 21, 22, 23), 1.0, 2.0, true);
        source.arcToPoint(new Offset(30, 31), Radius.elliptical(5, 6), 45.0, true, false);
        source.addRect(Rect.fromLTRB(40, 41, 42, 43));
        source.close();

        Path target = new Path();
        target.addPath(source, new Offset(100, 1000));

        List<Path.Segment> segs = target.segments();
        assertEquals(9, segs.size());

        assertArrayEquals(new double[] {101, 1002}, segs.get(0).coords, 0.0, "moveTo");
        assertArrayEquals(new double[] {103, 1004}, segs.get(1).coords, 0.0, "lineTo");
        assertArrayEquals(new double[] {105, 1006, 107, 1008, 109, 1010}, segs.get(2).coords, 0.0, "cubicTo");
        assertArrayEquals(new double[] {111, 1012, 113, 1014}, segs.get(3).coords, 0.0, "quadraticBezierTo");
        // conicTo's weight (0.5) is a ratio, not a position -- must survive untouched.
        assertArrayEquals(new double[] {115, 1016, 117, 1018, 0.5}, segs.get(4).coords, 0.0, "conicTo");
        // arcTo: only the rect corners move; startAngle/sweepAngle/forceMoveTo do not.
        assertArrayEquals(new double[] {120, 1021, 122, 1023, 1.0, 2.0, 1}, segs.get(5).coords, 0.0, "arcTo");
        // arcToPoint: only the arc-end point moves; radius/rotation/flags do not.
        assertArrayEquals(new double[] {130, 1031, 5, 6, 45.0, 1, 0}, segs.get(6).coords, 0.0, "arcToPoint");
        assertArrayEquals(new double[] {140, 1041, 142, 1043}, segs.get(7).coords, 0.0, "addRect");
        assertArrayEquals(new double[0], segs.get(8).coords, 0.0, "close");
    }

    @Test
    void shiftTranslatesEverySegmentAndLeavesTheSourceAlone() {
        Path p = new Path();
        p.arcTo(Rect.fromLTRB(0, 0, 10, 10), 0.3, 1.2, false);
        p.arcToPoint(new Offset(5, 5), Radius.circular(2), 90.0, false, true);

        Path shifted = p.shift(new Offset(-1, 2));

        // The source path is a copy's input, not touched by the copy.
        assertArrayEquals(new double[] {0, 0, 10, 10, 0.3, 1.2, 0}, p.segments().get(0).coords, 0.0);
        assertArrayEquals(new double[] {5, 5, 2, 2, 90.0, 0, 1}, p.segments().get(1).coords, 0.0);

        // Only the rect corners / arc-end point moved; angle, radius, rotation and the
        // flags did not -- before the fix these were corrupted by being summed with
        // dx/dy as if every coords entry were a coordinate.
        assertArrayEquals(new double[] {-1, 2, 9, 12, 0.3, 1.2, 0}, shifted.segments().get(0).coords, 0.0);
        assertArrayEquals(new double[] {4, 7, 2, 2, 90.0, 0, 1}, shifted.segments().get(1).coords, 0.0);
    }

    @Test
    void addPathWithMatrix4AppliesTheAffineTransformToPositionsOnly() {
        Path source = new Path();
        source.moveTo(1, 0);
        source.lineTo(0, 1);
        source.conicTo(2, 0, 0, 2, 0.75);

        // Scale x2 on both axes, then translate by (10, 20) -- column-major, matching
        // vector_math's Matrix4.storage layout (see Path.addPath's javadoc and
        // com.codename1.flutter.vectormath.Matrix4).
        double[] matrix4 = {
                2, 0, 0, 0,
                0, 2, 0, 0,
                0, 0, 1, 0,
                10, 20, 0, 1,
        };

        Path target = new Path();
        target.addPath(source, Offset.zero, matrix4);

        List<Path.Segment> segs = target.segments();
        assertArrayEquals(new double[] {12, 20}, segs.get(0).coords, 0.0, "(1,0) -> (2,0)+(10,20)");
        assertArrayEquals(new double[] {10, 22}, segs.get(1).coords, 0.0, "(0,1) -> (0,2)+(10,20)");
        // The conic weight is invariant under an affine transform of the control points.
        assertArrayEquals(new double[] {14, 20, 10, 24, 0.75}, segs.get(2).coords, 0.0, "conicTo");
    }

    @Test
    void addPathWithMatrix4FoldsTheOffsetIntoTheMatrixTranslation() {
        // dart:ui folds `offset` into matrix4's own translation before transforming
        // (CanvasPath.addPathWithMatrix), rather than translating separately.
        Path source = new Path();
        source.moveTo(1, 1);

        double[] identity = {
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1,
        };

        Path target = new Path();
        target.addPath(source, new Offset(5, 6), identity);

        assertArrayEquals(new double[] {6, 7}, target.segments().get(0).coords, 0.0);
    }

    @Test
    void addPathWithNullMatrix4BehavesLikeTheTwoArgOverload() {
        Path source = new Path();
        source.lineTo(3, 4);

        Path target = new Path();
        target.addPath(source, new Offset(1, 1), null);

        assertArrayEquals(new double[] {4, 5}, target.segments().get(0).coords, 0.0);
    }
}
