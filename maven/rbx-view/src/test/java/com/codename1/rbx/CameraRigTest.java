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
        assertEquals(16, dist, 1e-6);
    }
}
