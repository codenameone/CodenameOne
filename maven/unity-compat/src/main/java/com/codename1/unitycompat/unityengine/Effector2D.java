/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.unitycompat.unityengine;

/// `UnityEngine.Effector2D`: changes how the colliders beside it that are
/// marked as used by an effector meet other bodies.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public class Effector2D extends Behaviour {
    boolean useColliderMask = true;
    int colliderMask = -1;

    public boolean get_useColliderMask() {
        return useColliderMask;
    }

    public void set_useColliderMask(boolean value) {
        useColliderMask = value;
    }

    public int get_colliderMask() {
        return colliderMask;
    }

    public void set_colliderMask(int value) {
        colliderMask = value;
    }

    @Override
    public void $copyFrom(Component source) {
        super.$copyFrom(source);
        Effector2D e = (Effector2D) source;
        useColliderMask = e.useColliderMask;
        colliderMask = e.colliderMask;
    }
}
