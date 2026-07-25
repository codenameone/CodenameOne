/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.ai.vision;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Portable face observation.
 *
 * <p>Euler angles are expressed in degrees. Bounds and landmark points use
 * the normalized, top-left-origin coordinate space defined by
 * {@link VisionRect} and {@link VisionPoint}.</p>
 */
public final class Face {
    private final VisionRect bounds;
    private final Map<String, VisionPoint> landmarks;
    private final float yaw;
    private final float pitch;
    private final float roll;
    private final float smilingProbability;
    private final int trackingId;
    private final VisionMetadata metadata;

    public Face(VisionRect bounds, Map<String, VisionPoint> landmarks,
                float yaw, float pitch, float roll,
                float smilingProbability, int trackingId) {
        this(bounds, landmarks, yaw, pitch, roll, smilingProbability,
                trackingId, null);
    }

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

    public VisionRect getBounds() {
        return bounds;
    }

    public Map<String, VisionPoint> getLandmarks() {
        return landmarks;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public float getRoll() {
        return roll;
    }

    public float getSmilingProbability() {
        return smilingProbability;
    }

    public int getTrackingId() {
        return trackingId;
    }

    public VisionMetadata getMetadata() {
        return metadata;
    }
}
