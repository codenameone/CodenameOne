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

import org.junit.jupiter.api.Test;

class CameraRigTest {
    @Test
    void lookAtFacesTheTargetWithAnOrthonormalFrame() {
        CFrame c = CameraRig.lookAt(10, 5, 10, 0, 0, 0);
        Vector3 look = c.look();
        double d = Math.sqrt(10 * 10 + 5 * 5 + 10 * 10);
        assertEquals(-10 / d, look.x, 1e-9);
        assertEquals(-5 / d, look.y, 1e-9);
        assertEquals(-10 / d, look.z, 1e-9);
        assertEquals(1, c.right().magnitude(), 1e-9);
        assertEquals(0, c.right().y, 1e-9);
        assertEquals(0, c.right().dot(look), 1e-9);
    }

    @Test
    void followsTheCharacter() {
        Engine e = new Engine(Engine.SOLO);
        e.addPlayer(1, "Tester", null);
        for (int i = 0; i < 30; i++) {
            e.step(1 / 60.0);
        }
        CameraRig rig = new CameraRig();
        rig.update(e);
        Instance cam = (Instance) e.getWorkspace().prop("CurrentCamera");
        Instance root = ((Instance) e.getLocalPlayer().prop("Character")).findFirstChild("HumanoidRootPart");
        CFrame f = (CFrame) cam.prop("CFrame");
        CFrame r = Behaviors.cf(root);
        double dist = Math.sqrt((f.x - r.x) * (f.x - r.x) + (f.y - r.y - 1.5) * (f.y - r.y - 1.5)
                + (f.z - r.z) * (f.z - r.z));
        assertEquals(12.5, dist, 1e-6);
    }

    private static Engine spawned() {
        Engine e = new Engine(Engine.SOLO);
        e.setPhysics(new RigidBodies());
        e.addPlayer(1, "Tester", null);
        for (int i = 0; i < 30; i++) {
            e.step(1 / 60.0);
        }
        return e;
    }

    private static CFrame eye(Engine e) {
        return (CFrame) ((Instance) e.getWorkspace().prop("CurrentCamera")).prop("CFrame");
    }

    @Test
    void startsBehindTheCharacterLookingWhereItFaces() {
        // The camera used to start in front of a spawned character, looking
        // at its face with the course behind the camera.
        Engine e = spawned();
        new CameraRig().update(e);
        CFrame root = Behaviors.cf(((Instance) e.getLocalPlayer().prop("Character")).findFirstChild("HumanoidRootPart"));
        CFrame f = eye(e);
        Vector3 facing = root.look();
        double ox = f.x - root.x;
        double oz = f.z - root.z;
        assertTrue(ox * facing.x + oz * facing.z < -10, "the eye is behind the character");
        assertTrue(f.look().x * facing.x + f.look().z * facing.z > 0, "and looks the same way");
    }

    @Test
    void pullsInFrontOfAWallInsteadOfGoingThroughIt() {
        Engine e = spawned();
        CameraRig rig = new CameraRig();
        rig.update(e);
        CFrame before = eye(e);
        CFrame root = Behaviors.cf(((Instance) e.getLocalPlayer().prop("Character")).findFirstChild("HumanoidRootPart"));
        double mx = (before.x + root.x) / 2;
        double mz = (before.z + root.z) / 2;
        Instance wall = e.create("Part");
        wall.put("Anchored", Boolean.TRUE);
        wall.put("Size", new Vector3(40, 40, 1));
        wall.put("CFrame", new CFrame(mx, root.y, mz, 1, 0, 0, 0, 1, 0, 0, 0, 1));
        wall.setParent(e.getWorkspace());
        rig.update(e);
        CFrame f = eye(e);
        double d = Math.sqrt((f.x - root.x) * (f.x - root.x) + (f.z - root.z) * (f.z - root.z));
        double wallD = Math.sqrt((mx - root.x) * (mx - root.x) + (mz - root.z) * (mz - root.z));
        assertTrue(d < wallD, "the eye sits between the character and the wall: " + d + " vs " + wallD);
    }

    @Test
    void theCameraNeverDropsBelowTheHead() {
        CameraRig rig = new CameraRig();
        rig.orbit(0, -10);
        assertEquals(CameraRig.MIN_PITCH, rig.pitch(), 1e-9);
    }
}
