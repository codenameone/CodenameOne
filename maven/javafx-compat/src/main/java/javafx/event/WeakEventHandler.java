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

import java.lang.ref.WeakReference;

/// An [EventHandler] that forwards to another one without keeping it alive.
/// Whoever creates it must hold the wrapped handler.
public final class WeakEventHandler<T extends Event> implements EventHandler<T> {

    private final WeakReference ref;

    /// Wraps a handler.
    public WeakEventHandler(final @javafx.beans.NamedArg("eventHandler") EventHandler<T> eventHandler) {
        this.ref = new WeakReference(eventHandler);
    }

    /// Returns whether the wrapped handler has been garbage collected.
    public boolean wasGarbageCollected() {
        return ref.get() == null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handle(final T event) {
        Object handler = ref.get();
        if (handler instanceof EventHandler) {
            ((EventHandler<T>) handler).handle(event);
        }
    }
}
