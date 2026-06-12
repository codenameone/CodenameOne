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

/// 2D rigid body physics for Codename One games.
///
/// This package provides an idiomatic Codename One wrapper -- `PhysicsWorld`,
/// `PhysicsBody`, `ContactListener` and friends -- around a 2D rigid body
/// simulation. Bodies are linked to `com.codename1.gaming.Sprite`s through
/// `PhysicsLinkable`, and the world is stepped from a
/// `com.codename1.gaming.GameView` update loop. Everything is expressed in screen
/// pixels; the conversion to the simulation's meter based, y-up coordinate system
/// is handled internally (see `PhysicsWorld#setPixelsPerMeter(float)`).
///
/// The simulation engine in the sub package `box2d` is a derived work of **JBox2D**
/// (a Java port of Erin Catto's Box2D), used under the BSD 2-Clause license. The
/// original copyright and license notices are retained in those source files; see
/// the project NOTICE for attribution. Being pure Java, the engine runs on every
/// Codename One platform -- including iOS, where it is translated to C by ParparVM
/// -- with no native code.
package com.codename1.gaming.physics;
