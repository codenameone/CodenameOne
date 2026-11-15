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

import com.codename1.luau.Interp;
import com.codename1.luau.LuaError;

/// What `WorldRoot:Raycast` answers.
public final class RaycastResult extends RbxType {
    public final Instance instance;
    public final Vector3 position;
    public final Vector3 normal;
    public final Double distance;

    public RaycastResult(Instance instance, Vector3 position, Vector3 normal, Double distance) {
        this.instance = instance;
        this.position = position;
        this.normal = normal;
        this.distance = distance;
    }

    @Override
    public String typeName() {
        return "RaycastResult";
    }

    @Override
    public Object get(Interp in, String k) {
        switch (k) {
            case "Instance":
                return instance;
            case "Position":
                return position;
            case "Normal":
                return normal;
            case "Distance":
                return distance;
            case "Material":
                return instance.prop("Material");
            default:
                throw LuaError.rt(k + " is not a valid member of RaycastResult");
        }
    }

    @Override
    public String str() {
        return "RaycastResult{" + instance.getName() + " @ " + position.str() + "}";
    }
}
