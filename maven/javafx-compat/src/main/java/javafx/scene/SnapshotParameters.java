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
package javafx.scene;

import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Paint;
import javafx.scene.transform.Transform;

/// How a node is rendered into an image by
/// [Node#snapshot(SnapshotParameters, javafx.scene.image.WritableImage)]:
/// what the image is filled with first, a transform applied on top of
/// the node's own, and the part of the result to keep.
public class SnapshotParameters {

    private boolean depthBuffer;
    private Transform transform;
    private Paint fill;
    private Rectangle2D viewport;

    /// Creates parameters with no transform, no viewport and a white
    /// fill.
    public SnapshotParameters() {
    }

    /// Returns whether a depth buffer was asked for; recorded only.
    public boolean isDepthBuffer() {
        return depthBuffer;
    }

    /// Asks for a depth buffer; recorded only, the layer draws in two
    /// dimensions.
    public void setDepthBuffer(boolean depthBuffer) {
        this.depthBuffer = depthBuffer;
    }

    /// Returns the transform applied on top of the node's own, or `null`.
    public Transform getTransform() {
        return transform;
    }

    /// Sets the transform applied on top of the node's own.
    public void setTransform(Transform transform) {
        this.transform = transform;
    }

    /// Returns what the image is filled with first; `null` is white.
    public Paint getFill() {
        return fill;
    }

    /// Sets what the image is filled with first.
    public void setFill(Paint fill) {
        this.fill = fill;
    }

    /// Returns the part of the parent's coordinates that is rendered, or
    /// `null` for the bounds of the node.
    public Rectangle2D getViewport() {
        return viewport;
    }

    /// Sets the part of the parent's coordinates that is rendered.
    public void setViewport(Rectangle2D viewport) {
        this.viewport = viewport;
    }
}
