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

/// The default (`Custom`) camera: orbits the camera subject at a distance
/// the player drags and pinches, and stays put for a `Scriptable` camera a
/// script drives itself.
public final class CameraRig {
    private double yaw = Math.PI;
    private double pitch = 0.35;
    private double distance = 16;

    /// Turns the orbit; `dx`, `dy` in radians.
    public void orbit(double dx, double dy) {
        yaw += dx;
        pitch = Math.max(-1.2, Math.min(1.4, pitch + dy));
    }

    /// Multiplies the distance (a pinch or wheel step).
    public void zoom(double factor) {
        distance = Math.max(2, Math.min(120, distance * factor));
    }

    /// Places `CurrentCamera` for this frame.
    public void update(Engine e) {
        Instance ws = e.getWorkspace();
        Object c = ws.prop("CurrentCamera");
        if (!(c instanceof Instance)) {
            return;
        }
        Instance cam = (Instance) c;
        if ("Scriptable".equals(SceneSnapshot.enumName(cam.prop("CameraType"), "Custom"))) {
            return;
        }
        Vector3 t = target(cam.prop("CameraSubject"));
        if (t == null) {
            return;
        }
        double cp = Math.cos(pitch);
        double ex = t.x + Math.sin(yaw) * cp * distance;
        double ey = t.y + Math.sin(pitch) * distance;
        double ez = t.z + Math.cos(yaw) * cp * distance;
        cam.put("CFrame", lookAt(ex, ey, ez, t.x, t.y, t.z));
        cam.put("Focus", new CFrame(t.x, t.y, t.z, 1, 0, 0, 0, 1, 0, 0, 0, 1));
    }

    private static Vector3 target(Object subject) {
        if (!(subject instanceof Instance)) {
            return null;
        }
        Instance s = (Instance) subject;
        Instance part = s;
        if (s.isA("Humanoid")) {
            Instance m = s.getParent();
            part = m == null ? null : m.findFirstChild("HumanoidRootPart");
        }
        if (part == null || !part.isA("BasePart")) {
            return null;
        }
        CFrame f = Behaviors.cf(part);
        return new Vector3(f.x, f.y + 1.5, f.z);
    }

    /// A camera frame at the eye looking at the target; -Z faces the target.
    static CFrame lookAt(double ex, double ey, double ez, double tx, double ty, double tz) {
        double bx = ex - tx;
        double by = ey - ty;
        double bz = ez - tz;
        double bl = Math.sqrt(bx * bx + by * by + bz * bz);
        if (bl < 1e-9) {
            return new CFrame(ex, ey, ez, 1, 0, 0, 0, 1, 0, 0, 0, 1);
        }
        bx /= bl;
        by /= bl;
        bz /= bl;
        // right = up x back
        double rx = bz;
        double rz = -bx;
        double rl = Math.sqrt(rx * rx + rz * rz);
        if (rl < 1e-9) {
            rx = 1;
            rz = 0;
            rl = 1;
        }
        rx /= rl;
        rz /= rl;
        // up = back x right
        double ux = by * rz;
        double uy = bz * rx - bx * rz;
        double uz = -by * rx;
        return new CFrame(ex, ey, ez, rx, ux, bx, 0, uy, by, rz, uz, bz);
    }
}
