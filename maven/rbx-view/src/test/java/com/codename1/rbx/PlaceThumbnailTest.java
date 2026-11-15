/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.rbx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.rbx.place.PlaceFile;
import com.codename1.rbx.place.PlaceNode;
import com.codename1.rbx.place.Templates;

import java.util.List;

import org.junit.jupiter.api.Test;

class PlaceThumbnailTest {
    @Test
    void everyShapesPolygonsFaceOutward() {
        // A polygon wound the wrong way would be culled from the front and
        // painted from behind: holes in a box, insides of a cylinder.
        int[] shapes = {SceneSnapshot.BLOCK, SceneSnapshot.BALL, SceneSnapshot.CYLINDER, SceneSnapshot.WEDGE,
            SceneSnapshot.CORNER_WEDGE};
        for (int shape : shapes) {
            for (float[] poly : PlaceThumbnail.polygons(shape)) {
                int n = poly.length / 3;
                double[] x = new double[n];
                double[] y = new double[n];
                double[] z = new double[n];
                double cx = 0;
                double cy = 0;
                double cz = 0;
                for (int i = 0; i < n; i++) {
                    x[i] = poly[i * 3];
                    y[i] = poly[i * 3 + 1];
                    z[i] = poly[i * 3 + 2];
                    cx += x[i] / n;
                    cy += y[i] / n;
                    cz += z[i] / n;
                }
                double[] nrm = PlaceThumbnail.outward(x, y, z);
                assertTrue(nrm != null, "shape " + shape + " has a polygon with no normal; it would never be drawn");
                // the solids are convex and contain a point near the origin
                double ox = shape == SceneSnapshot.CORNER_WEDGE ? 0.15 : 0;
                double oz = shape == SceneSnapshot.CORNER_WEDGE ? 0.15 : shape == SceneSnapshot.WEDGE ? 0.1 : 0;
                double oy = shape == SceneSnapshot.WEDGE || shape == SceneSnapshot.CORNER_WEDGE ? -0.25 : 0;
                double d = nrm[0] * (cx - ox) + nrm[1] * (cy - oy) + nrm[2] * (cz - oz);
                assertTrue(d > 0, "shape " + shape + " has an inward polygon");
            }
        }
    }

    @Test
    void aBlockShowsThreeFacesFromAThreeQuarterView() {
        PlaceFile f = new PlaceFile();
        PlaceNode p = new PlaceNode("Part");
        p.put("CFrame", "CoordinateFrame", new CFrame(0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1));
        p.put("size", "Vector3", new Vector3(4, 4, 4));
        p.put("Color3uint8", "Color3uint8", Color3.rgb(255, 0, 0));
        f.roots.add(p);
        List<PlaceThumbnail.Face> faces = PlaceThumbnail.faces(f, 200, 120);
        assertEquals(3, faces.size());
        for (PlaceThumbnail.Face face : faces) {
            for (int i = 0; i < face.xs.length; i++) {
                assertTrue(face.xs[i] >= 0 && face.xs[i] <= 200 && face.ys[i] >= 0 && face.ys[i] <= 120,
                        "the part is framed");
            }
        }
    }

    @Test
    void theObbyTemplateIsPaintedBackToFront() throws Exception {
        PlaceFile f = PlaceFile.read(Templates.bytes("obby"));
        List<PlaceThumbnail.Face> faces = PlaceThumbnail.faces(f, 320, 180);
        assertTrue(faces.size() > 100, "faces: " + faces.size());
        // the lava sea is ground: painted first, then the rest back to front
        int first = 1;
        while (first < faces.size() && faces.get(first).depth <= faces.get(first - 1).depth) {
            first++;
        }
        for (int i = first + 1; i < faces.size(); i++) {
            assertTrue(faces.get(i - 1).depth >= faces.get(i).depth);
        }
    }

    @Test
    void aPolygonCrossingTheNearPlaneIsClippedNotDropped() {
        // A sea under the course has corners behind the eye; dropping such
        // polygons left the thumbnail without its ground.
        double[][] c = PlaceThumbnail.clipNear(new double[] {-1, 1, 1, -1}, new double[] {0, 0, 0, 0},
                new double[] {-5, -5, 5, 5}, 0.5);
        assertEquals(4, c[0].length);
        for (double z : c[2]) {
            assertTrue(z >= 0.5);
        }
        assertEquals(null, PlaceThumbnail.clipNear(new double[] {0, 1, 0}, new double[] {0, 0, 1},
                new double[] {-1, -1, -1}, 0.5));
    }
}
