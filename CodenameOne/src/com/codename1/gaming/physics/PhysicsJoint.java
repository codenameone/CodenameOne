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

import com.codename1.gaming.physics.box2d.common.Vec2;
import com.codename1.gaming.physics.box2d.dynamics.joints.Joint;
import com.codename1.gaming.physics.box2d.dynamics.joints.MouseJoint;

/// A constraint that ties two `PhysicsBody` objects together (or a body to the world)
/// -- a pin, a slider, a weld, a spring, a drag handle. Create one through the
/// `PhysicsWorld#createRevoluteJoint(PhysicsBody, PhysicsBody, float, float)` family
/// of methods; they take anchor points in pixels and handle the meters/y-flip
/// conversion for you. For joint-type-specific tuning (motors, limits, spring
/// frequency) reach the underlying engine joint with `#getNativeJoint()`.
public class PhysicsJoint {
    private final PhysicsWorld world;
    private final Joint joint;
    private final PhysicsBody bodyA;
    private final PhysicsBody bodyB;

    PhysicsJoint(PhysicsWorld world, Joint joint, PhysicsBody bodyA, PhysicsBody bodyB) {
        this.world = world;
        this.joint = joint;
        this.bodyA = bodyA;
        this.bodyB = bodyB;
    }

    /// The first body the joint connects.
    public PhysicsBody getBodyA() {
        return bodyA;
    }

    /// The second body the joint connects.
    public PhysicsBody getBodyB() {
        return bodyB;
    }

    /// True while the joint is still part of the world (i.e. not yet destroyed).
    public boolean isActive() {
        return joint.isActive();
    }

    /// Moves a mouse joint's target to the given pixel point (e.g. the current touch
    /// position). No effect on other joint types.
    public void setTarget(float xPx, float yPx) {
        if (joint instanceof MouseJoint) {
            ((MouseJoint) joint).setTarget(new Vec2(world.toMeters(xPx), -world.toMeters(yPx)));
        }
    }

    /// Removes the joint from its world. Using the joint afterwards is undefined.
    public void destroy() {
        world.destroyJoint(this);
    }

    /// The underlying Box2D joint, for type-specific control (cast it to
    /// `com.codename1.gaming.physics.box2d.dynamics.joints.RevoluteJoint` etc. to set
    /// motors and limits). Anchors and the y-axis on the native joint are in meters,
    /// y up.
    public Joint getNativeJoint() {
        return joint;
    }
}
