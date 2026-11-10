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

import com.codename1.orm.Ref;
import com.codename1.orm.session.Identifier;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/// The relationship holders of a record entity, as generated models and the
/// session handle them: a `Ref` for a to-one, a `LazyList` or `LazySet` for a
/// to-many. Each holder is mutable inside, so the session can read a value
/// into the record it belongs to without building a copy of the record.
///
/// Internal ORM runtime; not an application API.
/// @hidden
@com.codename1.impl.SharedWithBackend
public final class Refs {
    private Refs() {
    }

    /// A to-one holder whose value the session has yet to read.
    public static Ref unloadedRef() {
        Ref ref = Ref.empty();
        link(ref).loaded = false;
        return ref;
    }

    /// A to-many list whose contents the session has yet to read.
    public static List unloadedList() {
        LazyList list = new LazyList();
        list.link.loaded = false;
        return list;
    }

    /// A to-many set whose contents the session has yet to read.
    public static Set unloadedSet() {
        LazySet set = new LazySet();
        set.link.loaded = false;
        return set;
    }

    /// A read list holding `values`.
    public static List list(Object values) {
        LazyList list = new LazyList();
        list.fill((Collection) values);
        return list;
    }

    /// A read set holding `values`.
    public static Set set(Object values) {
        LazySet set = new LazySet();
        set.fill((Collection) values);
        return set;
    }

    /// The value a holder carries for the session: the target of a read `Ref`,
    /// a read collection itself, or null while it is unread. Anything else,
    /// a list the application built, is its own value.
    public static Object value(Object holder) {
        if (holder instanceof Holder) {
            Holder ref = (Holder) holder;
            return ref.link.loaded ? ref.value : null;
        }
        if (holder instanceof Optional) {
            return ((Optional) holder).orElse(null);
        }
        Link link = link(holder);
        if (link != null && !link.loaded) {
            return null;
        }
        return holder;
    }

    /// Puts `value` into `holder` in place. False when the holder cannot take
    /// it -- null, an `Optional`, a collection the application built -- and the
    /// record has to be copied with a new holder instead.
    public static boolean fill(Object holder, Object value) {
        if (holder instanceof Holder) {
            Holder ref = (Holder) holder;
            ref.value = value;
            ref.id = null;
            ref.link.loaded = true;
            return true;
        }
        if (holder instanceof LazyList) {
            ((LazyList) holder).fill((Collection) value);
            return true;
        }
        if (holder instanceof LazySet) {
            ((LazySet) holder).fill((Collection) value);
            return true;
        }
        return false;
    }

    /// Whether the holder's value has been read; true for anything that is not
    /// a lazy holder.
    public static boolean loaded(Object holder) {
        Link link = link(holder);
        return link == null || link.loaded;
    }

    /// Ties a holder to the session state of the record that carries it. A
    /// holder still unread takes the stored key, so a `Ref` answers its id
    /// without a query; an unread `Ref` with no key is simply empty. The inverse
    /// side of a to-one, `keyless`, has no stored key to take, so it stays
    /// unread and reads its target when it is first asked anything.
    static void bind(Object holder, EntityState state, int index, Class target, Object key, boolean keyless) {
        Link link = link(holder);
        if (link == null) {
            return;
        }
        link.bind(state, index, target);
        link.keyless = keyless;
        if (holder instanceof Holder && !link.loaded && !keyless) {
            Holder ref = (Holder) holder;
            ref.id = key;
            if (key == null) {
                ref.value = null;
                link.loaded = true;
            }
        }
    }

    /// Readies a holder that another record still carries, unread, for a new
    /// record the session is about to bind it to: a copy built from a found
    /// record shares its holders. Binding re-points the lazy state at the new
    /// record, so a collection is read through its owner first, while that
    /// owner can still read it; a `Ref` keeps the key it already holds. The
    /// inverse side of a to-one is left unread: nothing points at a row that is
    /// not inserted yet, so for the new record it reads back empty.
    static void share(Object holder, EntityState state) {
        Link link = link(holder);
        if (link == null || link.loaded || link.state == null || link.state == state //NOPMD CompareObjectsWithEquals - identity is the point
                || holder instanceof Holder || !link.state.attached || link.state.session == null) {
            return;
        }
        link.load();
    }

    /// The stored key of an unread `Ref`.
    static Object storedKey(Object holder) {
        return holder instanceof Holder ? ((Holder) holder).id : null;
    }

    /// The foreign key a record's to-one holder stands for, or one component of
    /// a composite one. Read without loading anything.
    public static Object key(Object holder, int component) {
        Object key;
        if (holder instanceof Holder) {
            key = ((Holder) holder).identifier();
        } else if (holder instanceof Optional) {
            key = Models.identifierOf(((Optional) holder).orElse(null));
        } else {
            key = null;
        }
        if (key == null) {
            return null;
        }
        return key instanceof Identifier ? ((Identifier) key).values()[component] : key;
    }

    private static Link link(Object holder) {
        if (holder instanceof Holder) {
            return ((Holder) holder).link;
        }
        if (holder instanceof LazyList) {
            return ((LazyList) holder).link;
        }
        if (holder instanceof LazySet) {
            return ((LazySet) holder).link;
        }
        return null;
    }
}
