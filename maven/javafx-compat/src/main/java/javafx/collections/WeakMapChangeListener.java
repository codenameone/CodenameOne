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
package javafx.collections;

import java.lang.ref.WeakReference;

import javafx.beans.WeakListener;

/// A [MapChangeListener] that forwards to another one without keeping it
/// alive. Whoever creates it must hold the wrapped listener; once that is
/// collected, this listener removes itself on the next change.
public final class WeakMapChangeListener<K, V> implements MapChangeListener<K, V>, WeakListener {

    private final WeakReference ref;

    /// Wraps a listener.
    public WeakMapChangeListener(MapChangeListener<K, V> listener) {
        if (listener == null) {
            throw new NullPointerException("Listener must be specified.");
        }
        this.ref = new WeakReference(listener);
    }

    @Override
    public boolean wasGarbageCollected() {
        return ref.get() == null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onChanged(Change<? extends K, ? extends V> change) {
        Object listener = ref.get();
        if (listener instanceof MapChangeListener) {
            ((MapChangeListener<K, V>) listener).onChanged(change);
        } else {
            change.getMap().removeListener(this);
        }
    }
}
