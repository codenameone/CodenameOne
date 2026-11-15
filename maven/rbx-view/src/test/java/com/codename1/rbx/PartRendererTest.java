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

import java.util.List;
import org.junit.jupiter.api.Test;

class PartRendererTest {
    @Test
    void snapshotMapsPartsLightingAndEffects() {
        Engine e = new Engine(Engine.SOLO);
        Instance p = e.create("Part");
        p.put("Size", new Vector3(4, 2, 6));
        p.put("CFrame", new CFrame(1, 2, 3, 1, 0, 0, 0, 1, 0, 0, 0, 1));
        p.put("Color", new Color3(1, 0, 0));
        p.setParent(e.getWorkspace());
        Instance hidden = e.create("Part");
        hidden.put("Transparency", Double.valueOf(1));
        hidden.setParent(e.getWorkspace());
        Instance w = e.create("WedgePart");
        w.setParent(e.getWorkspace());
        Instance bloom = e.create("BloomEffect");
        bloom.setParent(e.service("Lighting"));
        e.service("Lighting").put("ClockTime", Double.valueOf(18.5));
        SceneSnapshot s = SceneSnapshot.of(e);
        int wedges = 0;
        SceneSnapshot.Item red = null;
        for (SceneSnapshot.Item it : s.items) {
            if (it.part == p) { // NOPMD CompareObjectsWithEquals - identity
                red = it;
            }
            if (it.shape == SceneSnapshot.WEDGE) {
                wedges++;
            }
            assertTrue(it.part != hidden); // NOPMD CompareObjectsWithEquals - identity
        }
        assertEquals(1, wedges);
        assertEquals(0xff0000, red.rgb);
        assertEquals(4f, red.model[0], 1e-6);
        assertEquals(6f, red.model[10], 1e-6);
        assertEquals(3f, red.model[14], 1e-6);
        assertEquals(1f, s.bloomIntensity, 1e-6);
        assertEquals(18.5f, s.clockTime, 1e-6);
        assertEquals(0xe08850, PartRenderer.sky(s.clockTime));
    }

    @Test
    void batchesByShapeAndMaterial() {
        Engine e = new Engine(Engine.SOLO);
        for (int i = 0; i < 20; i++) {
            Instance p = e.create("Part");
            p.put("CFrame", new CFrame(i * 5, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1));
            p.setParent(e.getWorkspace());
        }
        Instance glass = e.create("Part");
        glass.put("Transparency", Double.valueOf(0.5));
        glass.setParent(e.getWorkspace());
        List<String> keys = new PartRenderer().batchKeys(SceneSnapshot.of(e));
        assertEquals(2, keys.size(), keys.toString());
    }

    @Test
    void meshesAreClosedUnitShapes() {
        for (float[] t : new float[][] {PartMeshes.cylinderTriangles(12), PartMeshes.wedgeTriangles(),
            PartMeshes.cornerWedgeTriangles()}) {
            assertEquals(0, t.length % 9);
            for (float v : t) {
                assertTrue(Math.abs(v) <= 0.5f + 1e-6f);
            }
        }
    }
}
