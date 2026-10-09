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

import UnityEngine.Vector2;

/// `UnityEngine.CapsuleCollider2D`: a box with a half circle at each end,
/// upright or lying down.
///
/// On the body it is a rectangle and two circles, which together are the
/// capsule exactly, so contacts and queries see its true outline. Its
/// rotational inertia is taken from those three overlapping pieces and so
/// is a little more than a capsule's own.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class CapsuleCollider2D extends Collider2D {
    public float $width = 0.5f;
    public float $height = 1f;
    /// `CapsuleDirection2D`: 0 upright, 1 lying down.
    public int $direction;

    public Vector2 get_size(Vector2 ret) {
        ret.x = $width;
        ret.y = $height;
        return ret;
    }

    public void set_size(Vector2 value) {
        $width = value.x;
        $height = value.y;
        PhysicsWorld.changed(gameObject);
    }

    public int get_direction() {
        return $direction;
    }

    public void set_direction(int value) {
        if ($direction != value) {
            $direction = value;
            PhysicsWorld.changed(gameObject);
        }
    }

    @Override
    void shape(PhysicsWorld.Shapes out) {
        out.capsule($offsetX, $offsetY, $width, $height, $direction);
    }

    @Override
    public Component $new() {
        return new CapsuleCollider2D();
    }

    @Override
    public void $copyFrom(Component source) {
        super.$copyFrom(source);
        CapsuleCollider2D c = (CapsuleCollider2D) source;
        $width = c.$width;
        $height = c.$height;
        $direction = c.$direction;
    }
}
