/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.ai.vision;

/** Common analyzer options; feature-specific options may extend this class. */
public class VisionOptions {
    private VisionBackend backend = VisionBackends.auto();
    private float minimumConfidence;
    private int maximumResults;

    public VisionOptions backend(VisionBackend value) {
        backend = value == null ? VisionBackends.auto() : value;
        return this;
    }

    public VisionOptions minimumConfidence(float value) {
        minimumConfidence = Math.max(0, Math.min(1, value));
        return this;
    }

    public VisionOptions maximumResults(int value) {
        maximumResults = Math.max(0, value);
        return this;
    }

    public VisionBackend getBackend() {
        return backend;
    }

    public float getMinimumConfidence() {
        return minimumConfidence;
    }

    public int getMaximumResults() {
        return maximumResults;
    }
}
