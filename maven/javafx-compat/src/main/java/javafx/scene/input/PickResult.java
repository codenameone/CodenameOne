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
package javafx.scene.input;

import javafx.beans.NamedArg;
import javafx.event.EventTarget;
import javafx.scene.Node;

/// What lay under the pointer when an event happened. This layer is two
/// dimensional: the result names the node and nothing about depth.
public class PickResult {

    private final Node node;
    private final double sceneX;
    private final double sceneY;

    /// Creates the result of a pick at a scene position.
    public PickResult(@NamedArg("target") EventTarget target, @NamedArg("sceneX") double sceneX,
            @NamedArg("sceneY") double sceneY) {
        this.node = target instanceof Node ? (Node) target : null;
        this.sceneX = sceneX;
        this.sceneY = sceneY;
    }

    /// Returns the node under the pointer; `null` when it was the scene.
    public final Node getIntersectedNode() {
        return node;
    }

    /// Returns the x of the pick in scene coordinates.
    public double cn1SceneX() {
        return sceneX;
    }

    /// Returns the y of the pick in scene coordinates.
    public double cn1SceneY() {
        return sceneY;
    }

    @Override
    public String toString() {
        return "PickResult [node = " + node + "]";
    }
}
