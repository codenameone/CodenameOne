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
package com.codename1.impl;

import com.codename1.ar.ARAnchor;
import com.codename1.ar.ARCapabilities;
import com.codename1.ar.ARHitResult;
import com.codename1.ar.ARLightEstimate;
import com.codename1.ar.ARNode;
import com.codename1.ar.ARPlane;
import com.codename1.ar.ARPose;
import com.codename1.ar.ARSessionOptions;
import com.codename1.ar.ARTrackingFailureReason;
import com.codename1.ar.ARTrackingState;
import com.codename1.ui.PeerComponent;
import com.codename1.util.AsyncResource;

import java.io.IOException;

/// Per-session platform contract behind `com.codename1.ar.ARSession`.
///
/// **Not part of the public API.** Each port subclasses this with a concrete
/// implementation; `CodenameOneImplementation#createARImpl()` is the factory
/// and returns null on platforms without AR support. One `ARImpl` instance
/// backs exactly one `ARSession`.
///
/// The core installs an `EventSink` before calling
/// `#open(ARSessionOptions)`. Implementations may invoke the sink from any
/// thread; the core marshals events to the EDT, coalescing high-frequency
/// updates.
///
/// @hidden
public abstract class ARImpl {

    /// Receives platform AR events. Installed by the core session before
    /// `ARImpl#open(ARSessionOptions)`; implementations may call it from any
    /// thread.
    public interface EventSink {
        /// The session's overall tracking quality changed.
        void onTrackingStateChanged(ARTrackingState state, ARTrackingFailureReason reason);

        /// A new plane was detected. The implementation constructs the
        /// `ARPlane` snapshot.
        void onPlaneAdded(ARPlane plane);

        /// A known plane was refined. `plane` is a new snapshot carrying the
        /// same id.
        void onPlaneUpdated(ARPlane plane);

        /// A plane was removed, for example merged into another plane.
        void onPlaneRemoved(String planeId);

        /// An anchor appeared - either confirmed by the platform after
        /// `ARImpl#createAnchor(ARPose)` for ids the core does not know yet,
        /// or recognized spontaneously (construct `ARImageAnchor` /
        /// `ARFaceAnchor` subtypes for images and faces).
        void onAnchorAdded(ARAnchor anchor);

        /// An anchor's pose or tracking state was refined.
        void onAnchorUpdated(String anchorId, ARPose pose, ARTrackingState state);

        /// A face anchor's geometry was refined. Arrays may be null when the
        /// platform does not supply that data; `regionPoses` is indexed by
        /// `com.codename1.ar.ARFaceRegion` ordinal.
        void onFaceAnchorUpdated(String anchorId, ARPose pose, ARTrackingState state,
                                 ARPose[] regionPoses, float[] meshVertices,
                                 int[] meshTriangles);

        /// An anchor was removed by the platform, for example a tracked image
        /// or face that left the camera view permanently.
        void onAnchorRemoved(String anchorId);

        /// A new light estimate is available. The core caches the latest
        /// value for polling; no per-frame events reach the application.
        void onLightEstimate(ARLightEstimate estimate);

        /// A new device pose is available. The core caches the latest value
        /// for polling; no per-frame events reach the application.
        void onCameraPose(ARPose pose);
    }

    /// Returns what this device supports. Callable before
    /// `#open(ARSessionOptions)`; used by `com.codename1.ar.AR#getCapabilities()`
    /// to probe without starting a session.
    public abstract ARCapabilities getCapabilities();

    /// Installs the event sink. Called exactly once, before
    /// `#open(ARSessionOptions)`.
    public abstract void setEventSink(EventSink sink);

    /// Starts the platform AR session with the supplied configuration. Throws
    /// `IOException` when the session cannot start, for example when the
    /// camera permission is denied or the mode is unsupported.
    public abstract void open(ARSessionOptions opts) throws IOException;

    /// Creates the component that renders the camera image composited with
    /// the anchored content. Called at most once per session.
    public abstract PeerComponent createViewPeer();

    /// Performs a hit test from the normalized view coordinate (`0.0` top
    /// left to `1.0` bottom right) into the world. Resolve `result` on the
    /// EDT with the intersections ordered nearest first; resolve with an
    /// empty array when nothing was hit.
    public abstract void hitTest(float xNorm, float yNorm, AsyncResource<ARHitResult[]> result);

    /// Creates a world anchor at the supplied pose and returns its stable id.
    public abstract String createAnchor(ARPose pose);

    /// Creates an anchor from a hit result, letting the platform anchor to
    /// the exact native raycast when `nativeHandle` is non-null; otherwise
    /// behaves like `#createAnchor(ARPose)`.
    public abstract String createAnchorFromHit(Object nativeHandle, ARPose pose);

    /// Removes the anchor (and any attached content) from the platform
    /// session.
    public abstract void removeAnchor(String anchorId);

    /// Attaches, replaces or removes (`node` is null) the content root
    /// rendered at the anchor.
    public abstract void setAnchorNode(String anchorId, ARNode node);

    /// The attached node subtree was mutated (transform, visibility or
    /// children); re-sync the platform renderer.
    public abstract void nodeChanged(String anchorId, ARNode node);

    /// Requests the camera permission needed for AR. Resolve `result` on the
    /// EDT with true when granted.
    public abstract void requestPermissions(AsyncResource<Boolean> result);

    /// Suspends tracking and the camera while keeping the session object
    /// usable. Pair with `#resume()`.
    public abstract void pause();

    /// Re-acquires the camera and resumes tracking after `#pause()`.
    public abstract void resume();

    /// Releases all native resources for this session. Subsequent calls
    /// become no-ops.
    public abstract void close();
}
