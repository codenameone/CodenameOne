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
    /// The closest an obstruction may pull the camera in: nearer than this
    /// the camera sits inside the character's head and shows only that.
    static final double MIN_CLEAR = 3.5;

    private double yaw;
    private double pitch = 0.35;
    private double distance = 12.5;
    /// The subject the yaw was last aimed from; a new one (a spawn, a
    /// respawn) puts the camera behind it again.
    private Instance aimedAt;
    private Vector3 pivot;

    /// The lowest pitch: level with the head. Below it the camera looks up
    /// from under the character, and a floor closer than [#MIN_CLEAR] along
    /// that ray is then passed through rather than stopped at, so the player
    /// saw the underside of the world as a ceiling.
    static final double MIN_PITCH = 0.05;
    /// The highest pitch, nearly straight down.
    static final double MAX_PITCH = 1.4;

    /// Orbits `p` at `dist` instead of following the camera subject, as an
    /// editor's viewport does; null goes back to following.
    public void setPivot(Vector3 p, double dist) {
        pivot = p;
        if (p != null) {
            distance = Math.max(2, Math.min(400, dist));
        }
    }

    /// The current pitch in radians, above the horizontal.
    double pitch() {
        return pitch;
    }

    /// Turns the orbit; `dx`, `dy` in radians.
    public void orbit(double dx, double dy) {
        yaw += dx;
        pitch = Math.max(MIN_PITCH, Math.min(MAX_PITCH, pitch + dy));
    }

    /// Multiplies the distance (a pinch or wheel step).
    public void zoom(double factor) {
        distance = Math.max(2, Math.min(pivot == null ? 120 : 400, distance * factor));
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
        if (pivot != null) {
            // The studio: no subject to follow, only a point to orbit, and
            // nothing to pull in for -- the editor wants to see through walls.
            double cp = Math.cos(pitch);
            cam.put("CFrame", lookAt(pivot.x + Math.sin(yaw) * cp * distance, pivot.y + Math.sin(pitch) * distance,
                    pivot.z + Math.cos(yaw) * cp * distance, pivot.x, pivot.y, pivot.z));
            cam.put("Focus", new CFrame(pivot.x, pivot.y, pivot.z, 1, 0, 0, 0, 1, 0, 0, 0, 1));
            return;
        }
        Object subject = cam.prop("CameraSubject");
        Vector3 t = target(subject);
        if (t == null) {
            return;
        }
        Instance root = rootOf(subject);
        if (root != aimedAt) { // NOPMD CompareObjectsWithEquals - tree identity
            aimedAt = root;
            yaw = behind(Behaviors.cf(root));
        }
        // Something solid closer than MIN_CLEAR would put the eye inside it
        // (a fence, a pad, the lava the character fell into), so the camera
        // rises over the obstruction, a step at a time, before it settles
        // for being pushed through it.
        double p = pitch;
        double d;
        while (true) {
            d = clearDistance(e, root, t, Math.sin(yaw) * Math.cos(p), Math.sin(p), Math.cos(yaw) * Math.cos(p));
            if (d >= MIN_CLEAR || p >= MAX_PITCH) {
                break;
            }
            p = Math.min(MAX_PITCH, p + 0.2);
        }
        d = Math.max(MIN_CLEAR, d);
        double cp = Math.cos(p);
        double ex = t.x + Math.sin(yaw) * cp * d;
        double ey = t.y + Math.sin(p) * d;
        double ez = t.z + Math.cos(yaw) * cp * d;
        cam.put("CFrame", lookAt(ex, ey, ez, t.x, t.y, t.z));
        cam.put("Focus", new CFrame(t.x, t.y, t.z, 1, 0, 0, 0, 1, 0, 0, 0, 1));
    }

    /// The yaw that puts the eye behind a part, looking where it faces.
    static double behind(CFrame f) {
        // LookVector is -Z; the eye sits along +Z, so yaw is the angle of +Z.
        return Math.atan2(f.r02, f.r22);
    }

    /// How far the eye can sit along the unit direction before something
    /// solid is between it and the subject: the camera pulls in rather than
    /// end up inside a wall or a tree, the way players expect.
    private double clearDistance(Engine e, Instance character, Vector3 t, double dx, double dy, double dz) {
        Engine.PhysicsWorld w = e.getPhysics();
        if (w == null) {
            return distance;
        }
        Object[] hit = w.raycast(e, t, new Vector3(dx * distance, dy * distance, dz * distance),
                new NotSelf(character));
        if (hit == null) {
            return distance;
        }
        if (!(hit[1] instanceof Vector3)) {
            return distance;
        }
        Vector3 at = (Vector3) hit[1];
        double hx = at.x - t.x;
        double hy = at.y - t.y;
        double hz = at.z - t.z;
        return Math.min(distance, Math.sqrt(hx * hx + hy * hy + hz * hz) - 0.6);
    }

    /// Solid, visible parts that are not the character itself.
    private static final class NotSelf implements Engine.RaycastFilter {
        private final Instance character;
        private final Instance model;

        NotSelf(Instance character) {
            this.character = character;
            this.model = character.getParent();
        }

        @Override
        public boolean accepts(Instance p) {
            return p != character && (model == null || !p.isDescendantOf(model)) // NOPMD CompareObjectsWithEquals - tree identity
                    && p.bool("CanCollide") && p.num("Transparency") < 0.5;
        }
    }

    private static Instance rootOf(Object subject) {
        Instance s = (Instance) subject;
        if (s.isA("Humanoid")) {
            Instance m = s.getParent();
            Instance r = m == null ? null : m.findFirstChild("HumanoidRootPart");
            return r == null ? s : r;
        }
        return s;
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
