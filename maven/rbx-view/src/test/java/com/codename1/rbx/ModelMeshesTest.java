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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.gpu.GltfScene;
import org.junit.jupiter.api.Test;

class ModelMeshesTest {
    @Test
    void characterLimbsLoadAndFitTheUnitCube() {
        GltfScene c = ModelMeshes.scene(ModelMeshes.CHARACTER);
        assertNotNull(c, "character model");
        for (int k = SceneSnapshot.TORSO; k <= SceneSnapshot.RIGHT_LEG; k++) {
            String node = ModelMeshes.characterNode(k);
            assertNotNull(node);
            assertFits(ModelMeshes.fitted(c, node, true), node);
        }
        assertNull(ModelMeshes.characterNode(SceneSnapshot.PLAIN));
        assertNull(ModelMeshes.fitted(c, "no-such-node", true));
    }

    @Test
    void treesAndRocksLoadAndFitTheUnitCube() {
        for (String[] set : new String[][] {ModelMeshes.TREES, ModelMeshes.ROCKS}) {
            for (String n : set) {
                GltfScene s = ModelMeshes.scene(n);
                assertNotNull(s, n);
                assertFits(ModelMeshes.fitted(s, null, false), n);
            }
        }
        // A tree keeps separate bark and leaf colours.
        ModelMeshes.Piece[] tree = ModelMeshes.fitted(ModelMeshes.scene(ModelMeshes.TREES[0]), null, false);
        assertTrue(tree.length >= 2, "bark and leaves");
    }

    @Test
    void snapshotMarksLimbsAndScenery() {
        Engine e = new Engine(Engine.SOLO);
        Instance model = e.create("Model");
        e.create("Humanoid").setParent(model);
        String[] names = {"Torso", "Left Arm", "Right Arm", "Left Leg", "Right Leg"};
        for (String n : names) {
            Instance p = e.create("Part");
            p.setName(n);
            p.setParent(model);
        }
        model.setParent(e.getWorkspace());
        Instance loose = e.create("Part");
        loose.setName("Torso");
        loose.setParent(e.getWorkspace());
        for (String n : new String[] {"Trunk", "Leaves", "Rock"}) {
            Instance p = e.create("Part");
            p.setName(n);
            p.setParent(e.getWorkspace());
        }
        int[] seen = new int[SceneSnapshot.ROCK + 1];
        for (SceneSnapshot.Item it : SceneSnapshot.of(e).items) {
            seen[it.kind]++;
        }
        for (int k = SceneSnapshot.TORSO; k <= SceneSnapshot.ROCK; k++) {
            assertEquals(1, seen[k], "kind " + k);
        }
        assertEquals(1, seen[SceneSnapshot.PLAIN], "a Torso outside a character stays plain");
    }

    @Test
    void treeGrowsFromTheTrunkBottom() {
        // An upright trunk 6 long centred at y=3: its tree spans y 0..10.5.
        float[] trunk = {0, 6, 0, 0, -1.4f, 0, 0, 0, 0, 0, 1.4f, 0, 5, 3, 7, 1};
        float[] box = new float[16];
        PartRenderer.treeBox(trunk, box);
        assertEquals(10.5f, box[5], 1e-4f);
        assertEquals(5.25f, box[13], 1e-4f);
        assertEquals(5f, box[12], 0);
        assertEquals(7f, box[14], 0);
    }

    private static void assertFits(ModelMeshes.Piece[] pieces, String what) {
        assertNotNull(pieces, what);
        float[] lo = {1, 1, 1};
        float[] hi = {-1, -1, -1};
        for (ModelMeshes.Piece p : pieces) {
            assertTrue(p.indices.length > 0 && p.indices.length % 3 == 0, what);
            for (int v = 0; v < p.vertices.length; v += 8) {
                for (int a = 0; a < 3; a++) {
                    lo[a] = Math.min(lo[a], p.vertices[v + a]);
                    hi[a] = Math.max(hi[a], p.vertices[v + a]);
                }
            }
        }
        for (int a = 0; a < 3; a++) {
            assertEquals(-0.5f, lo[a], 1e-3f, what);
            assertEquals(0.5f, hi[a], 1e-3f, what);
        }
    }
}
