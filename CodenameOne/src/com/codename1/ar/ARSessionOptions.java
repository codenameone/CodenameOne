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

/// Configuration for an `ARSession`. Fluent builder.
///
/// The defaults open a world tracking session that detects horizontal planes
/// and estimates lighting, which suits the common place-content-on-a-surface
/// use case.
public final class ARSessionOptions {
    private ARTrackingMode trackingMode = ARTrackingMode.WORLD;
    private ARPlaneDetection planeDetection = ARPlaneDetection.HORIZONTAL;
    private boolean lightEstimation = true;
    private ARReferenceImage[] referenceImages = new ARReferenceImage[0];

    /// The tracking configuration to run. Default `ARTrackingMode#WORLD`.
    public ARSessionOptions trackingMode(ARTrackingMode mode) {
        this.trackingMode = mode == null ? ARTrackingMode.WORLD : mode;
        return this;
    }

    /// Which surface orientations to detect. Default
    /// `ARPlaneDetection#HORIZONTAL`. Ignored in face tracking sessions.
    public ARSessionOptions planeDetection(ARPlaneDetection detection) {
        this.planeDetection = detection == null ? ARPlaneDetection.HORIZONTAL : detection;
        return this;
    }

    /// Whether to estimate real-world lighting. Default true.
    public ARSessionOptions lightEstimation(boolean on) {
        this.lightEstimation = on;
        return this;
    }

    /// Reference images the session should detect. When any are registered,
    /// detected images are delivered as `ARImageAnchor`s through the anchor
    /// listener. Default none.
    public ARSessionOptions referenceImages(ARReferenceImage[] images) {
        if (images == null) {
            this.referenceImages = new ARReferenceImage[0];
        } else {
            ARReferenceImage[] copy = new ARReferenceImage[images.length];
            System.arraycopy(images, 0, copy, 0, images.length);
            this.referenceImages = copy;
        }
        return this;
    }

    /// The tracking configuration to run.
    public ARTrackingMode getTrackingMode() {
        return trackingMode;
    }

    /// Which surface orientations the session detects.
    public ARPlaneDetection getPlaneDetection() {
        return planeDetection;
    }

    /// True when the session estimates real-world lighting.
    public boolean isLightEstimation() {
        return lightEstimation;
    }

    /// The registered reference images as a newly allocated array.
    public ARReferenceImage[] getReferenceImages() {
        ARReferenceImage[] copy = new ARReferenceImage[referenceImages.length];
        System.arraycopy(referenceImages, 0, copy, 0, referenceImages.length);
        return copy;
    }
}
