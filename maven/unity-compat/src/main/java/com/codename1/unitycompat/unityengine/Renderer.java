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

/// `UnityEngine.Renderer`: what makes an object visible, and where among
/// the others it is drawn.
///
/// Unity orders 2D drawing by sorting layer first, then by the order within
/// the layer, then by distance from the camera. A sorting layer is known by
/// an id that says nothing about its place; the place is its position in
/// the project's list, which the scene compiler hands to
/// [UnityRuntime#$sortingLayers(int[])].
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public class Renderer extends Component {
    boolean enabled = true;
    int sortingOrder;
    int sortingLayerID;

    public boolean get_enabled() {
        return enabled;
    }

    public void set_enabled(boolean value) {
        enabled = value;
    }

    public int get_sortingOrder() {
        return sortingOrder;
    }

    public void set_sortingOrder(int value) {
        sortingOrder = value;
    }

    public int get_sortingLayerID() {
        return sortingLayerID;
    }

    public void set_sortingLayerID(int value) {
        sortingLayerID = value;
    }

    @Override
    public void $copyFrom(Component source) {
        Renderer r = (Renderer) source;
        enabled = r.enabled;
        sortingOrder = r.sortingOrder;
        sortingLayerID = r.sortingLayerID;
    }
}
