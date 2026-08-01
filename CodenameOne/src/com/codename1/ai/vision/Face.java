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
package com.codename1.ai.vision;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/// Portable face observation.
///
/// <p>Euler angles are expressed in degrees. Bounds and landmark points use
/// the normalized, top-left-origin coordinate space defined by
/// {@link VisionRect} and {@link VisionPoint}.</p>
public final class Face {
    private final VisionRect bounds;
    private final Map<String, VisionPoint> landmarks;
    private final float yaw;
    private final float pitch;
    private final float roll;
    private final float smilingProbability;
    private final int trackingId;
    private final VisionMetadata metadata;

    /// Creates a detected face without backend metadata.
    /// @param bounds face bounds in the oriented image coordinate space
    /// @param landmarks named feature points, defensively copied
    /// @param yaw horizontal Euler angle in degrees
    /// @param pitch vertical Euler angle in degrees
    /// @param roll in-plane Euler angle in degrees
    /// @param smilingProbability smile confidence, or a negative value when unavailable
    /// @param trackingId stable streaming id, or a negative value when unavailable
    public Face(VisionRect bounds, Map<String, VisionPoint> landmarks,
                float yaw, float pitch, float roll,
                float smilingProbability, int trackingId) {
        this(bounds, landmarks, yaw, pitch, roll, smilingProbability,
                trackingId, null);
    }

    /// Creates a detected face with backend diagnostics.
    /// @param bounds face bounds in the oriented image coordinate space
    /// @param landmarks named feature points, defensively copied
    /// @param yaw horizontal Euler angle in degrees
    /// @param pitch vertical Euler angle in degrees
    /// @param roll in-plane Euler angle in degrees
    /// @param smilingProbability smile confidence, or a negative value when unavailable
    /// @param trackingId stable streaming id, or a negative value when unavailable
    /// @param metadata backend details, or {@code null}
    public Face(VisionRect bounds, Map<String, VisionPoint> landmarks,
                float yaw, float pitch, float roll,
                float smilingProbability, int trackingId,
                VisionMetadata metadata) {
        this.bounds = bounds == null ? VisionRect.EMPTY : bounds;
        this.landmarks = landmarks == null
                ? Collections.<String, VisionPoint>emptyMap()
                : Collections.unmodifiableMap(new HashMap<String, VisionPoint>(landmarks));
        this.yaw = yaw;
        this.pitch = pitch;
        this.roll = roll;
        this.smilingProbability = smilingProbability;
        this.trackingId = trackingId;
        this.metadata = metadata;
    }

    /// @return normalized top-left-origin face bounds
    public VisionRect getBounds() {
        return bounds;
    }

    /// @return immutable named landmark map; possibly empty
    public Map<String, VisionPoint> getLandmarks() {
        return landmarks;
    }

    /// @return left/right head rotation in degrees, or 0 if unavailable
    public float getYaw() {
        return yaw;
    }

    /// @return up/down head rotation in degrees, or 0 if unavailable
    public float getPitch() {
        return pitch;
    }

    /// @return in-plane head rotation in degrees, or 0 if unavailable
    public float getRoll() {
        return roll;
    }

    /// @return smile probability in 0..1, or -1 when unavailable
    public float getSmilingProbability() {
        return smilingProbability;
    }

    /// @return stream tracking identifier, or -1 when unavailable
    public int getTrackingId() {
        return trackingId;
    }

    /// @return backend metadata, or {@code null} when manually constructed
    public VisionMetadata getMetadata() {
        return metadata;
    }
}
