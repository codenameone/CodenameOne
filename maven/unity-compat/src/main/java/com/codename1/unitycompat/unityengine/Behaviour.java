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
package com.codename1.unitycompat.unityengine;

/// `UnityEngine.Behaviour`: a component that can be switched off.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public class Behaviour extends Component {
    boolean enabled = true;

    public boolean get_enabled() {
        return enabled;
    }

    public void set_enabled(boolean value) {
        if (enabled == value) {
            return;
        }
        boolean was = live();
        enabled = value;
        if (was != live()) {
            activeChanged(!was);
        }
    }

    public boolean get_isActiveAndEnabled() {
        return live();
    }

    @Override
    boolean live() {
        return enabled && super.live();
    }

    /// Called when [#live] changes, whether because of `enabled` or because
    /// the object or one of its parents was activated or deactivated.
    void activeChanged(boolean nowLive) {
    }

    @Override
    public void $copyFrom(Component source) {
        super.$copyFrom(source);
        enabled = ((Behaviour) source).enabled;
    }
}
