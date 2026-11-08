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
package com.codename1.orm;

import com.codename1.impl.orm.Holder;

/// A to-one relationship of a record entity, read on first use.
///
/// A record cannot be changed after it is built, so it cannot hold a lazy
/// relationship the way an enhanced class does. It holds a `Ref` instead:
///
/// ```java
/// @Entity
/// public record Order(@Id long id, @ManyToOne Ref<Customer> customer) {}
///
/// Order order = orders.find(7L);
/// Object customerId = order.customer().id();   // no query
/// Customer customer = order.customer().get();  // read now, once
/// ```
///
/// A `Ref` loaded by a session reads its target through that session, and only
/// while it is open: `get()` on a `Ref` that was never read, once the session is
/// closed, throws `LazyInitializationException`. `id()` and `isPresent()` read
/// nothing either, with one exception: on the inverse side of a one-to-one,
/// `@OneToOne(mappedBy = ...)`, the record's own row stores no key, so they read
/// the target, and throw the same way once the session is gone.
///
/// To point a record at another entity, build it with `Ref.of(target)`, or
/// `Ref.empty()` for none. Two refs are equal when they point at the same
/// entity: the same target type and the same identifier.
@com.codename1.impl.SharedWithBackend
public final class Ref<T> extends Holder {
    private Ref() {
    }

    /// A ref to `target`, or an empty one when `target` is null.
    public static <T> Ref<T> of(T target) {
        Ref<T> ref = new Ref<T>();
        ref.assign(target);
        return ref;
    }

    /// A ref to nothing.
    public static <T> Ref<T> empty() {
        return of(null);
    }

    /// The target, read through the owning session if it has not been read
    /// yet; null when the ref is empty.
    ///
    /// #### Throws
    ///
    /// - `com.codename1.orm.session.LazyInitializationException`: if the target
    ///   was never read and the session that loaded the ref is closed
    @SuppressWarnings("unchecked")
    public T get() {
        load();
        return (T) current();
    }

    /// True when the ref points at an entity, read or not.
    public boolean isPresent() {
        return loaded() ? current() != null : identifier() != null;
    }

    /// The target's identifier, without reading it unless the ref is the inverse
    /// side of a one-to-one; null for an empty ref, or a target that has not
    /// been saved yet.
    public Object id() {
        return identifier();
    }

    /// True once the target has been read, or for a ref that was built with a
    /// value.
    public boolean isLoaded() {
        return loaded();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Ref)) {
            return false;
        }
        Ref<?> ref = (Ref<?>) other;
        if (unreadable() || ref.unreadable()) {
            // An unread inverse side of a detached record: nothing is known
            // about its target, so it is equal only to itself. Throwing here
            // would keep every such record out of sets and maps.
            return false;
        }
        Object id = identifier();
        Object otherId = ref.identifier();
        if (id == null || otherId == null) {
            // Without identifiers only the same target, or nothing, is equal.
            return id == null && otherId == null && loaded() && ref.loaded() && current() == ref.current();
        }
        Class type = targetType();
        Class otherType = ref.targetType();
        return id.equals(otherId) && (type == null || otherType == null
                || type.isAssignableFrom(otherType) || otherType.isAssignableFrom(type));
    }

    @Override
    public int hashCode() {
        if (unreadable()) {
            return 0;
        }
        Object id = identifier();
        if (id != null) {
            return id.hashCode();
        }
        Object value = loaded() ? current() : null;
        return value == null ? 0 : System.identityHashCode(value);
    }

    @Override
    public String toString() {
        Object id = storedIdentifier();
        return "Ref[" + (id != null ? id : !loaded() ? "unread" : current() != null ? current() : "empty") + "]";
    }
}
