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
package com.codename1.unitycompat.unityengine.ui;

import com.codename1.unitycompat.unityengine.Component;
import com.codename1.unitycompat.unityengine.eventsystems.UIBehaviour;

/// `UnityEngine.UI.GraphicRaycaster`: what makes the graphics of a canvas
/// something a pointer can hit. A canvas without one is drawn and never
/// clicked, which is how Unity lets a heads-up display sit over a game
/// without swallowing its input.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public class GraphicRaycaster extends UIBehaviour {
    @Override
    public Component $new() {
        return new GraphicRaycaster();
    }
}
