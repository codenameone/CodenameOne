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

/// Base of `com.codename1.orm.Ref`: the state the session reads and writes
/// without widening the public type.
///
/// Internal ORM runtime; not an application API.
/// @hidden
@com.codename1.impl.SharedWithBackend
public abstract class Holder {
    final Link link = new Link();
    Object value;
    /// The target's identifier while the value has not been read.
    Object id;

    protected Holder() {
    }

    /// Reads the value through the owning session if it has not been read.
    protected final void load() {
        link.load();
    }

    /// The value as it stands, without reading it.
    protected final Object current() {
        return value;
    }

    /// Sets a value that needs no reading.
    protected final void assign(Object target) {
        value = target;
        id = null;
        link.loaded = true;
    }

    protected final boolean loaded() {
        return link.loaded;
    }

    /// The identifier of the target, read or not, or null without one. The
    /// inverse side of a to-one stores no key, so it reads the target first.
    protected final Object identifier() {
        if (!link.loaded && link.keyless) {
            link.load();
        }
        return storedIdentifier();
    }

    /// True for the inverse side of a to-one that was never read and can no
    /// longer be: its identifier is unknown for good.
    protected final boolean unreadable() {
        return !link.loaded && link.keyless && (link.state == null || !link.state.attached);
    }

    /// The identifier as far as it is known, without reading anything.
    protected final Object storedIdentifier() {
        return link.loaded ? Models.identifierOf(value) : id;
    }

    /// The target's type: the bound relationship's, or the value's own.
    protected final Class targetType() {
        return value != null ? value.getClass() : link.type;
    }
}
