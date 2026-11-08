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

/// The lazy state of one relationship value held by a record: which session
/// entry and relationship it belongs to, and whether it has been read. A record
/// cannot carry woven state the way an enhanced class does, so the holder it
/// keeps in the component -- a `Ref`, a session list or set -- carries it.
///
/// Internal ORM runtime; not an application API.
@com.codename1.impl.SharedWithBackend
final class Link {
    EntityState state;
    int index;
    boolean loaded = true;
    /// The relationship's target type, known once the session binds the holder.
    Class type;

    void load() {
        if (loaded) {
            return;
        }
        EntityState owner = state;
        if (owner == null || !owner.attached) {
            throw new LazyInitializationException("Relationship is not loaded and the entity is detached");
        }
        owner.read(index);
        // The session installs the value through the owner's model, which marks
        // this link loaded. A holder the owner no longer carries is not reached
        // by that, and is answered as it stands rather than read again.
        loaded = true;
    }

    void bind(EntityState owner, int relationship, Class target) {
        state = owner;
        index = relationship;
        type = target;
    }
}
