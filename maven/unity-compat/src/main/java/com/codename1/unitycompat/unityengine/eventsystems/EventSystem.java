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
package com.codename1.unitycompat.unityengine.eventsystems;

import com.codename1.unitycompat.unityengine.Component;
import com.codename1.unitycompat.unityengine.GameObject;
import com.codename1.unitycompat.unityengine.UnityRuntime;

/// `UnityEngine.EventSystems.EventSystem`: what turns pointer input into
/// clicks on user interface controls.
///
/// A scene needs one for its buttons to work, exactly as in Unity -- the
/// editor adds it with the first canvas -- and the runtime routes the
/// pointer only while one is active. The routing itself is done by the
/// runtime, once a frame before any script's `Update`: the topmost
/// graphic under the pointer that is a raycast target is found, and the
/// event goes to the nearest control at or above it. Unity leaves that
/// part to an input module beside the event system; here the event system
/// stands for both.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public class EventSystem extends UIBehaviour {
    @Override
    public Component $new() {
        return new EventSystem();
    }

    /// The event system in charge: the first active one of the scene.
    public static EventSystem get_current() {
        return UnityRuntime.$eventSystem();
    }

    /// Whether the pointer is over any graphic that stops it. A game asks
    /// this to tell a click on its interface from a click on its world.
    public boolean IsPointerOverGameObject() {
        return UnityRuntime.$pointerOverInterface();
    }

    /// The object of the control last pressed, or null.
    public GameObject get_currentSelectedGameObject() {
        return UnityRuntime.$selectedObject();
    }

    /// Makes the control on `selected` the selected one, or with null
    /// none.
    public void SetSelectedGameObject(GameObject selected) {
        UnityRuntime.$select(selected);
    }
}
