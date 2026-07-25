/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.ai.vision;

/** Creates reusable body-pose analyzers. */
public final class PoseDetector extends AbstractVisionAnalyzer<Pose[]> {
    public PoseDetector() {
        this(null);
    }

    public PoseDetector(VisionOptions options) {
        super(VisionFeature.POSE_DETECTION, options);
    }
}
