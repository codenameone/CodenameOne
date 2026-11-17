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

import com.codenameone.examples.brickyard.luau.Interp;

/// A half line: an origin and a direction whose length is the reach.
public final class Ray extends RbxType {
    public final Vector3 origin;
    public final Vector3 direction;

    public Ray(Vector3 origin, Vector3 direction) {
        this.origin = origin;
        this.direction = direction;
    }

    @Override
    public String typeName() {
        return "Ray";
    }

    private static final Method CLOSEST = new Method("ClosestPoint", 0);
    private static final Method DIST = new Method("Distance", 1);

    @Override
    public Object get(Interp in, String k) {
        switch (k) {
            case "Origin":
                return origin;
            case "Direction":
                return direction;
            case "Unit":
                return new Ray(origin, direction.unit());
            case "ClosestPoint":
                return CLOSEST;
            case "Distance":
                return DIST;
            default:
                return super.get(in, k);
        }
    }

    public Vector3 closest(Vector3 p) {
        Vector3 u = direction.unit();
        double t = Math.max(0, p.sub(origin).dot(u));
        return origin.add(u.scale(t));
    }

    @Override
    protected Object[] invoke(int id, Interp in, Object[] a) {
        Vector3 p = Args.vec3(a, 1, id == 0 ? "ClosestPoint" : "Distance");
        Vector3 c = closest(p);
        return id == 0 ? ret(c) : ret(Args.n(c.sub(p).magnitude()));
    }

    @Override
    public String str() {
        return "{" + origin.str() + "}, {" + direction.str() + "}";
    }
}
