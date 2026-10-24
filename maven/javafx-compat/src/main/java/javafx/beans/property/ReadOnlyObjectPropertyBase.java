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

import com.codename1.fxcompat.runtime.ValueListeners;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;

/// Base class for a read-only property holding an object: it keeps the listeners
/// and leaves the value to the subclass, which calls
/// [#fireValueChangedEvent()] whenever that value may have changed.
public abstract class ReadOnlyObjectPropertyBase<T> extends ReadOnlyObjectProperty<T> {

    private ValueListeners<T> helper;

    /// Creates the property.
    public ReadOnlyObjectPropertyBase() {
    }

    @Override
    public void addListener(InvalidationListener listener) {
        helper = ValueListeners.add(helper, this, listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        helper = ValueListeners.remove(helper, listener);
    }

    @Override
    public void addListener(ChangeListener<? super T> listener) {
        helper = ValueListeners.add(helper, this, listener);
    }

    @Override
    public void removeListener(ChangeListener<? super T> listener) {
        helper = ValueListeners.remove(helper, listener);
    }

    /// Tells the listeners the value may have changed.
    protected void fireValueChangedEvent() {
        ValueListeners.fire(helper);
    }
}
