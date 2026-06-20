/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.maps;

/// Common base for objects placed on a map ([Marker], [Polyline],
/// [Polygon], [Circle]). Holds the bookkeeping the map surface and the
/// native providers need to track the object across the Java/native
/// boundary without the public API exposing it.
public abstract class MapObject {

    private static int idCounter = 1;

    private final int id;

    /// Opaque handle owned by whichever backend (vector engine or native
    /// provider) currently renders this object. For native providers it
    /// typically holds the `long` element key; for the vector engine it is
    /// unused. Package visible by design.
    Object providerKey;

    /// True once the object has been removed from its surface.
    boolean removed;

    MapObject() {
        synchronized (MapObject.class) {
            id = idCounter++;
        }
    }

    /// A process-unique identifier for this object.
    public int getId() {
        return id;
    }

    /// {@inheritDoc}
    public int hashCode() {
        return id;
    }

    /// {@inheritDoc}
    public boolean equals(Object o) {
        return o instanceof MapObject && ((MapObject) o).id == id;
    }
}
