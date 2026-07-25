/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.ai.vision;

/** Portable body-pose result. */
public final class Pose {
    private final Landmark[] landmarks;
    private final VisionMetadata metadata;

    public Pose(Landmark[] landmarks) {
        this(landmarks, null);
    }

    public Pose(Landmark[] landmarks, VisionMetadata metadata) {
        if (landmarks == null) {
            this.landmarks = new Landmark[0];
        } else {
            this.landmarks = new Landmark[landmarks.length];
            System.arraycopy(landmarks, 0, this.landmarks, 0, landmarks.length);
        }
        this.metadata = metadata;
    }

    public Landmark[] getLandmarks() {
        Landmark[] out = new Landmark[landmarks.length];
        System.arraycopy(landmarks, 0, out, 0, landmarks.length);
        return out;
    }

    public VisionMetadata getMetadata() {
        return metadata;
    }

    public static final class Landmark {
        private final String name;
        private final VisionPoint position;
        private final float confidence;

        public Landmark(String name, VisionPoint position, float confidence) {
            this.name = name;
            this.position = position;
            this.confidence = confidence;
        }

        public String getName() {
            return name;
        }

        public VisionPoint getPosition() {
            return position;
        }

        public float getConfidence() {
            return confidence;
        }
    }
}
