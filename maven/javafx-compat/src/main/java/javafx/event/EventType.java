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
package javafx.event;

import java.io.Serializable;

/// The kind of an event. Types form a tree under [#ROOT]: a handler
/// registered for a type also receives the events of its sub types.
public final class EventType<T extends Event> implements Serializable {

    private static final long serialVersionUID = 1L;

    /// The type every other type descends from.
    public static final EventType<Event> ROOT = new EventType<Event>("EVENT", null);

    private final EventType<? super T> superType;
    private final String name;

    /// Creates an unnamed type directly under [#ROOT].
    ///
    /// #### Deprecated
    ///
    /// A type without a name cannot be told apart in a log; name it.
    @Deprecated
    public EventType() {
        this(ROOT, null);
    }

    /// Creates a named type directly under [#ROOT].
    public EventType(final String name) {
        this(ROOT, name);
    }

    /// Creates an unnamed sub type.
    public EventType(final EventType<? super T> superType) {
        this(superType, null);
    }

    /// Creates a named sub type.
    public EventType(final EventType<? super T> superType, final String name) {
        if (superType == null) {
            throw new NullPointerException("Event super type must not be null!");
        }
        this.superType = superType;
        this.name = name;
    }

    private EventType(final String name, final EventType<? super T> superType) {
        this.superType = superType;
        this.name = name;
    }

    /// Returns the type this one is a sub type of; `null` for [#ROOT].
    public final EventType<? super T> getSuperType() {
        return superType;
    }

    /// Returns the name; `null` for an unnamed type.
    public final String getName() {
        return name;
    }

    /// Returns the name, or the default text for an unnamed type.
    @Override
    public String toString() {
        return (name != null) ? name : super.toString();
    }
}
