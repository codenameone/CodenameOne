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
package com.codenameone.examples.brickyard.rbx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class PickTest {
    private static Instance part(Engine e, String name, double x, double y, double z) {
        Instance p = e.create("Part");
        p.setName(name);
        p.put("Size", new Vector3(2, 2, 2));
        p.put("CFrame", new CFrame(x, y, z, 1, 0, 0, 0, 1, 0, 0, 0, 1));
        p.setParent(e.getWorkspace());
        return p;
    }

    private static Engine looking() {
        Engine e = new Engine(Engine.SOLO);
        Instance cam = (Instance) e.getWorkspace().prop("CurrentCamera");
        cam.put("CFrame", CameraRig.lookAt(0, 0, 20, 0, 0, 0));
        cam.put("FieldOfView", 70.0);
        return e;
    }

    @Test
    void theCentreOfTheViewPicksThePartInFrontOfTheCamera() {
        Engine e = looking();
        Instance near = part(e, "Near", 0, 0, 5);
        part(e, "Far", 0, 0, -5);
        assertSame(near, PlayView.pick(e, 400, 300, 800, 600));
    }

    @Test
    void aClickBesideEveryPartPicksNothing() {
        Engine e = looking();
        part(e, "Only", 0, 0, 0);
        assertNull(PlayView.pick(e, 10, 10, 800, 600));
    }

    @Test
    void anOffCentreClickFindsTheOffCentrePart() {
        Engine e = looking();
        part(e, "Centre", 0, 0, 0);
        Instance right = part(e, "Right", 6, 0, 0);
        // 6 studs right at 20 away: tan = 0.3 of tan(35 deg) * aspect
        double tx = Math.tan(Math.toRadians(35)) * 800 / 600;
        int px = (int) Math.round((0.3 / tx + 1) / 2 * 800);
        assertSame(right, PlayView.pick(e, px, 300, 800, 600));
    }

    @Test
    void aRayFromInsideABoxEntersAtZero() {
        Engine e = looking();
        Instance p = part(e, "Box", 0, 0, 0);
        assertEquals(0, PlayView.rayBox(p, 0, 0, 0, 1, 0, 0), 1e-9);
    }
}
