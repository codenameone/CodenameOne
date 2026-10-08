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
package com.codename1.fxcompat.runtime;

import java.lang.ref.WeakReference;
import java.util.function.Function;

import javafx.beans.WeakListener;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

/// Keeps two properties equal: a change of either is written to the other,
/// through a conversion when their types differ.
///
/// The binding is the change listener of both properties and holds both
/// weakly, so neither property keeps the other alive; when one is collected
/// the binding removes itself from the survivor. Two bindings of the same
/// pair are equal whatever the order, which is how unbinding finds the
/// listener to remove.
public final class BidirectionalBinding<A, B> implements ChangeListener<Object>, WeakListener {

    private final WeakReference first;
    private final WeakReference second;
    private final Function<? super B, ? extends A> toFirst;
    private final Function<? super A, ? extends B> toSecond;
    private final int hash;
    private boolean updating;

    private BidirectionalBinding(Object first, Object second, Function<? super B, ? extends A> toFirst,
            Function<? super A, ? extends B> toSecond) {
        this.first = new WeakReference(first);
        this.second = new WeakReference(second);
        this.toFirst = toFirst;
        this.toSecond = toSecond;
        this.hash = System.identityHashCode(first) * System.identityHashCode(second);
    }

    private static void check(Object first, Object second) {
        if (first == null || second == null) {
            throw new NullPointerException("Both properties must be specified.");
        }
        if (first == second) {
            throw new IllegalArgumentException("Cannot bind property to itself");
        }
    }

    /// Binds two properties of the same type; the first takes the value of
    /// the second.
    public static <T> void bind(Property<T> first, Property<T> second) {
        bind(first, second, new Function<T, T>() {
            @Override
            public T apply(T value) {
                return value;
            }
        }, new Function<T, T>() {
            @Override
            public T apply(T value) {
                return value;
            }
        });
    }

    /// Binds two properties through a pair of conversions; the first takes
    /// the converted value of the second.
    @SuppressWarnings("unchecked")
    public static <A, B> void bind(Property<A> first, Property<B> second, Function<? super B, ? extends A> toFirst,
            Function<? super A, ? extends B> toSecond) {
        check(first, second);
        BidirectionalBinding<A, B> binding = new BidirectionalBinding<A, B>(first, second, toFirst, toSecond);
        first.setValue(toFirst.apply(second.getValue()));
        ((ObservableValue<Object>) first).addListener(binding);
        ((ObservableValue<Object>) second).addListener(binding);
    }

    /// Removes the binding of two objects, when both are properties and one
    /// exists.
    @SuppressWarnings("unchecked")
    public static void unbind(Object first, Object second) {
        check(first, second);
        if (first instanceof ObservableValue && second instanceof ObservableValue) {
            BidirectionalBinding<Object, Object> probe = new BidirectionalBinding<Object, Object>(first, second, null,
                    null);
            ((ObservableValue<Object>) first).removeListener(probe);
            ((ObservableValue<Object>) second).removeListener(probe);
        }
    }

    @Override
    public boolean wasGarbageCollected() {
        return first.get() == null || second.get() == null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void changed(ObservableValue<? extends Object> source, Object oldValue, Object newValue) {
        if (updating) {
            return;
        }
        Object a = first.get();
        Object b = second.get();
        if (!(a instanceof Property) || !(b instanceof Property)) {
            if (a instanceof Property) {
                ((Property<Object>) a).removeListener(this);
            }
            if (b instanceof Property) {
                ((Property<Object>) b).removeListener(this);
            }
            return;
        }
        Property<A> one = (Property<A>) a;
        Property<B> two = (Property<B>) b;
        updating = true;
        try {
            if (one == source) {
                two.setValue(toSecond.apply((A) newValue));
            } else {
                one.setValue(toFirst.apply((B) newValue));
            }
        } catch (RuntimeException failure) {
            restore(one, two, source, oldValue);
            throw new RuntimeException("Bidirectional binding failed, setting to the previous value", failure);
        } finally {
            updating = false;
        }
    }

    /// Puts the changed property back to its old value and drops the binding,
    /// so the two properties are not left disagreeing while bound.
    @SuppressWarnings("unchecked")
    private void restore(Property<A> one, Property<B> two, ObservableValue<?> source, Object oldValue) {
        one.removeListener(this);
        two.removeListener(this);
        if (one == source) {
            one.setValue((A) oldValue);
        } else {
            two.setValue((B) oldValue);
        }
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BidirectionalBinding)) {
            return false;
        }
        Object a = first.get();
        Object b = second.get();
        if (a == null || b == null) {
            return false;
        }
        BidirectionalBinding<?, ?> that = (BidirectionalBinding<?, ?>) other;
        Object otherA = that.first.get();
        Object otherB = that.second.get();
        return (a == otherA && b == otherB) || (a == otherB && b == otherA);
    }
}
