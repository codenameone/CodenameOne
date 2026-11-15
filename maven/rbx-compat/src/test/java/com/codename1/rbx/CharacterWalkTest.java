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

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CharacterWalkTest {
    private static Instance part(Engine e, String cls, double x, double y, double z, double sx, double sy,
            double sz, double r00, double r22) {
        Instance p = e.create(cls);
        p.put("Size", new Vector3(sx, sy, sz));
        p.put("CFrame", new CFrame(x, y, z, r00, 0, 0, 0, 1, 0, 0, 0, r22));
        p.put("Anchored", Boolean.TRUE);
        p.setParent(e.getWorkspace());
        return p;
    }

    /// A ramp is walked up its slope. The controller once treated a wedge as
    /// its bounding box: a wall the height of the ramp's top, so a player
    /// holding forward stood at its foot for good.
    @Test
    void walksUpAWedge() {
        Engine e = new Engine(Engine.SOLO);
        e.setPhysics(new RigidBodies());
        part(e, "Part", 0, -1, 0, 100, 2, 100, 1, 1);
        part(e, "SpawnLocation", 0, 0.5, 0, 6, 1, 6, 1, 1);
        // Turned around, so the slope rises towards -Z, the way a character
        // facing the default camera walks.
        part(e, "WedgePart", 0, 2, -15, 6, 4, 10, -1, -1);
        part(e, "Part", 0, 2, -26, 6, 4, 12, 1, 1);
        e.addPlayer(1, "Walker", null);
        for (int i = 0; i < 60; i++) {
            e.step(1 / 60.0);
        }
        Instance character = (Instance) e.service("Players").getChildren().get(0).prop("Character");
        Instance root = character.findFirstChild("HumanoidRootPart");
        Input.key(e, "W", true);
        for (int i = 0; i < 90; i++) {
            e.step(1 / 60.0);
        }
        CFrame at = Behaviors.cf(root);
        assertTrue(at.z < -21, "walked past the ramp onto the deck, at z=" + at.z);
        assertTrue(at.y > 6, "standing on the deck 4 studs up, at y=" + at.y);
    }
}
