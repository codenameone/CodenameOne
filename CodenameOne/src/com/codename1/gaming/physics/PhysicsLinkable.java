/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.gaming.physics;

/// Implemented by anything a `PhysicsBody` can drive -- typically a
/// `com.codename1.gaming.Sprite`. After `PhysicsWorld#step(float)` integrates the
/// simulation, the world pushes each body's transform into its linked object
/// through this interface, converting from physics meters to screen pixels and
/// flipping the y axis so the linked object stays in screen coordinates.
///
/// Keeping the binding to this minimal interface (rather than a concrete `Sprite`)
/// lets the physics package stay independent of the sprite/rendering layer.
public interface PhysicsLinkable {
    /// Sets the object's position from the body center, in pixels.
    void setPhysicsPosition(float xPx, float yPx);

    /// Sets the object's rotation from the body, in radians (clockwise positive in
    /// screen space).
    void setPhysicsRotation(float radians);
}
