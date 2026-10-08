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
package android.util;

/// A named, typed value on a host object, used by `ObjectAnimator` to
/// animate it without looking setters up by name.
public abstract class Property<T, V> {

    private final String mName;
    private final Class<V> mType;

    /// AOSP builds a reflective property for any host class; Codename One
    /// has no reflection, so this answers the `View` properties
    /// ([android.view.View#ALPHA] and the rest) and throws
    /// [NoSuchPropertyException] for any other name.
    @SuppressWarnings("unchecked")
    public static <T, V> Property<T, V> of(Class<T> hostType, Class<V> valueType, String name) {
        Property<android.view.View, ?> p = android.view.View.propertyNamed(name);
        if (p != null && android.view.View.class.isAssignableFrom(hostType)
                && (valueType == p.getType() || valueType.isAssignableFrom(p.getType()))) {
            return (Property<T, V>) p;
        }
        throw new NoSuchPropertyException("No accessor method or field found for property with name " + name);
    }

    public Property(Class<V> type, String name) {
        mName = name;
        mType = type;
    }

    public boolean isReadOnly() {
        return false;
    }

    public void set(T object, V value) {
        throw new UnsupportedOperationException("Property " + getName() + " is read-only");
    }

    public abstract V get(T object);

    public String getName() {
        return mName;
    }

    public Class<V> getType() {
        return mType;
    }
}
