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

/// Notified when bodies in a `PhysicsWorld` start and stop touching. Register with
/// `PhysicsWorld#addContactListener(ContactListener)`.
///
/// Callbacks fire from inside `PhysicsWorld#step(float)` -- i.e. on the game loop
/// thread -- so it is safe to read and update game state directly, but you must not
/// create or destroy bodies during the callback (defer that until after `step`
/// returns).
public interface ContactListener {
    /// Called when two fixtures begin touching.
    void beginContact(PhysicsContact contact);

    /// Called when two fixtures stop touching.
    void endContact(PhysicsContact contact);
}
