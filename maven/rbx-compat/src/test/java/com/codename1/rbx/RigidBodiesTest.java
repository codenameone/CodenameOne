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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RigidBodiesTest {
    private static Instance part(Engine e, double x, double y, double z, double sx, double sy, double sz,
            boolean anchored) {
        Instance p = e.create("Part");
        p.put("Size", new Vector3(sx, sy, sz));
        p.put("CFrame", new CFrame(x, y, z, 1, 0, 0, 0, 1, 0, 0, 0, 1));
        p.put("Anchored", Boolean.valueOf(anchored));
        p.setParent(e.getWorkspace());
        return p;
    }

    @Test
    void fallsLandsAndTouches() {
        Engine e = new Engine(Engine.SOLO);
        e.setPhysics(new RigidBodies());
        part(e, 0, 0, 0, 50, 2, 50, true);
        Instance box = part(e, 0, 20, 0, 2, 2, 2, false);
        final int[] touched = {0};
        box.event("Touched").connect(new Signal.Listener() {
            @Override
            public void fired(Object[] args) {
                touched[0]++;
            }
        });
        for (int i = 0; i < 300; i++) {
            e.step(1 / 60.0);
        }
        assertEquals(2, Behaviors.cf(box).y, 0.05);
        assertTrue(touched[0] >= 1, "bounces may touch more than once");
    }

    @Test
    void fallenPartsAreDestroyed() {
        Engine e = new Engine(Engine.SOLO);
        e.setPhysics(new RigidBodies());
        e.getWorkspace().put("FallenPartsDestroyHeight", Double.valueOf(-50));
        Instance box = part(e, 0, 0, 0, 2, 2, 2, false);
        for (int i = 0; i < 300; i++) {
            e.step(1 / 60.0);
        }
        assertTrue(box.getParent() == null);
    }

    @Test
    void raycastHitsRotatedBox() {
        Engine e = new Engine(Engine.SOLO);
        RigidBodies w = new RigidBodies();
        double c = Math.cos(Math.PI / 4);
        Instance p = e.create("Part");
        p.put("Size", new Vector3(4, 4, 4));
        p.put("CFrame", new CFrame(0, 0, 0, c, 0, c, 0, 1, 0, -c, 0, c));
        p.put("Anchored", Boolean.TRUE);
        p.setParent(e.getWorkspace());
        Object[] hit = w.raycast(e, new Vector3(-10, 0, 0), new Vector3(20, 0, 0), null);
        assertNotNull(hit);
        assertEquals(-2 * Math.sqrt(2), ((Vector3) hit[1]).x, 1e-6);
        assertEquals(1, w.partsInBox(e, new CFrame(0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1), new Vector3(1, 1, 1), null)
                .size());
    }
}
