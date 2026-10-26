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
package com.codename1.desktopcompat.javax.swing;

/// The input map of a component's `WHEN_IN_FOCUSED_WINDOW` bindings; it
/// knows the component it belongs to.
public class ComponentInputMap extends InputMap {

    private final JComponent component;

    public ComponentInputMap(JComponent component) {
        if (component == null) {
            throw new IllegalArgumentException("ComponentInputMaps must be associated with a non-null JComponent");
        }
        this.component = component;
    }

    @Override
    public void setParent(InputMap map) {
        if (map != null && (!(map instanceof ComponentInputMap)
                || ((ComponentInputMap) map).getComponent() != component)) {
            throw new IllegalArgumentException(
                    "ComponentInputMaps must have a parent ComponentInputMap associated with the same component");
        }
        super.setParent(map);
    }

    public JComponent getComponent() {
        return component;
    }
}
