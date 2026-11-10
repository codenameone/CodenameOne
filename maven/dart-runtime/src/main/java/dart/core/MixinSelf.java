/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package dart.core;

import dart.collection.IterableMixin;
import dart.collection.ListMixin;
import dart.collection.SetMixin;

/// `this` inside a `mixin M on Iterable<E>` (or `on List<E>`, `on Set<E>`), as the core
/// collection the mixin is applied to. The mixin is a Java interface, and the class that
/// applies it is either a runtime collection (a subclass of DartList, say) or a program
/// class extending ListBase/IterableBase/SetBase, which keeps its own storage and so is
/// not one. The second kind is reached through the same live view a parameter of that
/// type would get, so no cast can fail on either.
public final class MixinSelf {

    private MixinSelf() {
    }

    /// `this` as an Iterable.
    @SuppressWarnings("unchecked")
    public static <E> DartIterable<E> iterable(Object self) {
        if (self instanceof DartIterable) {
            return (DartIterable<E>) self;
        }
        if (self instanceof DartList) {
            return ((DartList<E>) self).asIterable();
        }
        if (self instanceof DartSet) {
            return ((DartSet<E>) self).asIterable();
        }
        if (self instanceof ListMixin) {
            return new ListMixinView<E>((ListMixin<E>) self).asIterable();
        }
        if (self instanceof SetMixin) {
            return new SetMixinView<E>((SetMixin<E>) self).asIterable();
        }
        if (self instanceof IterableMixin) {
            return DartIterable.ofMixin((IterableMixin<E>) self);
        }
        if (self instanceof Iterable) {
            return DartIterable.wrap((Iterable<E>) self);
        }
        throw new IllegalStateException("not an Iterable: " + self);
    }

    /// `this` as a List.
    @SuppressWarnings("unchecked")
    public static <E> DartList<E> list(Object self) {
        if (self instanceof DartList) {
            return (DartList<E>) self;
        }
        if (self instanceof ListMixin) {
            return new ListMixinView<E>((ListMixin<E>) self);
        }
        throw new IllegalStateException("not a List: " + self);
    }

    /// `this` as a Set.
    @SuppressWarnings("unchecked")
    public static <E> DartSet<E> set(Object self) {
        if (self instanceof DartSet) {
            return (DartSet<E>) self;
        }
        if (self instanceof SetMixin) {
            return new SetMixinView<E>((SetMixin<E>) self);
        }
        throw new IllegalStateException("not a Set: " + self);
    }
}
