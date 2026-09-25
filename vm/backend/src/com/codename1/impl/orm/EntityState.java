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
package com.codename1.impl.orm;

import com.codename1.orm.session.LazyInitializationException;

/// Per-instance lazy state inserted into an entity by the enhancer.
///
/// Internal ORM runtime; not an application API.
/// @hidden
public final class EntityState {
    SessionImpl session;
    final Object entity;
    final boolean[] loaded;
    final boolean[] fetching;
    final Object[] keys;
    boolean attached = true;
    EntityState(SessionImpl session, Object entity, int relationships) {
        this.session = session;
        this.entity = entity;
        loaded = new boolean[relationships];
        fetching = new boolean[relationships];
        keys = new Object[relationships];
    }
    /// Initializes a relationship before an enhanced field read.
    public static void beforeRead(ManagedEntity entity, int index) {
        EntityState state = entity.__cn1OrmState();
        if (state != null) {
            state.read(index);
        }
    }
    /// Rejects serializing an unloaded relationship instead of fetching implicitly.
    public static void beforeSerialization(ManagedEntity entity, int index) {
        EntityState state = entity.__cn1OrmState();
        if (state != null && !state.loaded[index]) {
            throw new LazyInitializationException("Initialize the relationship before serialization");
        }
    }
    /// Captures relationship state before an enhanced field assignment.
    public static void beforeWrite(ManagedEntity entity, int index) {
        EntityState state = entity.__cn1OrmState();
        if (state != null) {
            if (state.attached && !state.loaded[index]) {
                state.session.beforeAssignment(entity, index);
            }
            state.assigned(index);
        }
    }
    public void read(int index) {
        if (loaded[index]) {
            return;
        }
        if (!attached) {
            throw new LazyInitializationException("Entity is detached");
        }
        session.initializeForAccess(entity, index);
    }
    public void assigned(int index) {
        loaded[index] = true;
    }
    /// Returns the stored key without triggering an association query.
    public Object key(int index) {
        return keys[index];
    }
    public boolean isLoaded(int index) {
        return loaded[index];
    }
}
