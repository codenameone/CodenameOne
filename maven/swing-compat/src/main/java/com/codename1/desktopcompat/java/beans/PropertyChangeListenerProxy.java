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
package com.codename1.desktopcompat.java.beans;

import java.util.EventListenerProxy;

/// A listener bound to one property name, wrapping the listener that
/// receives that property's changes.
///
/// The device class library's `EventListenerProxy` is not generic, so this
/// extends it as a raw type and `getListener()` answers the wrapped listener
/// as a plain `EventListener`.
public class PropertyChangeListenerProxy extends EventListenerProxy implements PropertyChangeListener {

    private final String propertyName;

    public PropertyChangeListenerProxy(String propertyName, PropertyChangeListener listener) {
        super(listener);
        this.propertyName = propertyName;
    }

    public void propertyChange(PropertyChangeEvent evt) {
        PropertyChangeListener l = (PropertyChangeListener) getListener();
        l.propertyChange(evt);
    }

    public String getPropertyName() {
        return propertyName;
    }
}
