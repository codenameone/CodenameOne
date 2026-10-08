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
package javafx.beans.property;

/// A writable property that hands out a read-only view of itself: the owner
/// keeps the wrapper and writes to it, and publishes
/// [#getReadOnlyProperty()], which always shows the same value.
public class ReadOnlyFloatWrapper extends SimpleFloatProperty {

    private ReadOnlyPropertyImpl readOnlyProperty;

    /// Creates a wrapper with no bean, an empty name and the default value.
    public ReadOnlyFloatWrapper() {
    }

    /// Creates a wrapper with no bean and an empty name.
    public ReadOnlyFloatWrapper(float initialValue) {
        super(initialValue);
    }

    /// Creates a wrapper of a bean with the default value.
    public ReadOnlyFloatWrapper(Object bean, String name) {
        super(bean, name);
    }

    /// Creates a wrapper of a bean.
    public ReadOnlyFloatWrapper(Object bean, String name, float initialValue) {
        super(bean, name, initialValue);
    }

    /// Returns the read-only view; the same object on every call.
    public ReadOnlyFloatProperty getReadOnlyProperty() {
        if (readOnlyProperty == null) {
            readOnlyProperty = new ReadOnlyPropertyImpl();
        }
        return readOnlyProperty;
    }

    @Override
    protected void fireValueChangedEvent() {
        super.fireValueChangedEvent();
        if (readOnlyProperty != null) {
            readOnlyProperty.fireValueChangedEvent();
        }
    }

    private final class ReadOnlyPropertyImpl extends ReadOnlyFloatPropertyBase {

        @Override
        public float get() {
            return ReadOnlyFloatWrapper.this.get();
        }

        @Override
        public Object getBean() {
            return ReadOnlyFloatWrapper.this.getBean();
        }

        @Override
        public String getName() {
            return ReadOnlyFloatWrapper.this.getName();
        }
    }
}
