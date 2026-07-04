/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.ar;

/// A fixed position and orientation in the real world that the AR session
/// keeps tracking as its understanding of the environment improves. Anchors
/// are the attachment points for virtual content: create one with
/// `ARSession#createAnchor(ARPose)` or `ARHitResult#createAnchor()` and hang
/// an `ARNode` on it with `#setNode(ARNode)`.
///
/// The session refines anchor poses over time; updates are delivered through
/// the session's `ARAnchorListener` and reflected by `#getPose()`.
public class ARAnchor {
    private final String id;
    private ARPose pose;
    private ARTrackingState trackingState = ARTrackingState.TRACKING;
    private ARNode node;
    private boolean detached;
    ARSession session;

    /// Creates an anchor. Intended for platform implementations and tests;
    /// applications create anchors through the session.
    ///
    /// #### Parameters
    ///
    /// - `id`: the stable identifier of this anchor
    ///
    /// - `pose`: the initial world pose
    public ARAnchor(String id, ARPose pose) {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("id is required");
        }
        this.id = id;
        this.pose = pose == null ? ARPose.IDENTITY : pose;
    }

    /// The stable identifier of this anchor.
    public String getId() {
        return id;
    }

    /// The current world pose. Updated on the EDT as tracking refines.
    public ARPose getPose() {
        return pose;
    }

    /// The tracking quality of this anchor.
    public ARTrackingState getTrackingState() {
        return trackingState;
    }

    /// The content root rendered at this anchor, or null when none is
    /// attached.
    public ARNode getNode() {
        return node;
    }

    /// Attaches (or replaces) the content root rendered at this anchor. Pass
    /// null to remove the content while keeping the anchor.
    ///
    /// #### Parameters
    ///
    /// - `node`: the root node to render at this anchor, or null
    public void setNode(ARNode node) {
        if (detached) {
            throw new IllegalStateException("anchor is detached");
        }
        if (node != null && node.getParent() != null) {
            throw new IllegalArgumentException("the anchor content root may not have a parent node");
        }
        ARNode old = this.node;
        if (old != null && old != node) {
            old.session = null;
            old.anchorId = null;
        }
        this.node = node;
        if (node != null) {
            node.session = session;
            node.anchorId = id;
        }
        if (session != null) {
            session.anchorNodeChangedInternal(this, node);
        }
    }

    /// Removes this anchor (and any attached content) from the session.
    /// No-op when already detached.
    public void detach() {
        if (detached) {
            return;
        }
        detached = true;
        if (node != null) {
            node.session = null;
            node.anchorId = null;
        }
        if (session != null) {
            session.anchorDetachedInternal(this);
        }
    }

    /// True once `#detach()` has been called or the platform removed the
    /// anchor.
    public boolean isDetached() {
        return detached;
    }

    /// Updates the pose and tracking state from the platform. Called by the
    /// session on the EDT.
    void update(ARPose pose, ARTrackingState state) {
        if (pose != null) {
            this.pose = pose;
        }
        if (state != null) {
            this.trackingState = state;
        }
    }

    /// Marks the anchor detached without calling back into the session. Used
    /// when the platform removes the anchor.
    void markDetached() {
        detached = true;
    }
}
