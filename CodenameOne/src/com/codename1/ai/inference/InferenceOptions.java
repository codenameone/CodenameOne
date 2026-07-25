/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.ai.inference;

/** LiteRT session configuration. */
public final class InferenceOptions {
    public enum Accelerator {
        AUTO, CPU, GPU, NPU, CORE_ML
    }

    private Accelerator accelerator = Accelerator.AUTO;
    private int threads;
    private boolean allowFallback = true;

    public InferenceOptions accelerator(Accelerator value) {
        accelerator = value == null ? Accelerator.AUTO : value;
        return this;
    }

    public InferenceOptions threads(int value) {
        threads = Math.max(0, value);
        return this;
    }

    public InferenceOptions allowFallback(boolean value) {
        allowFallback = value;
        return this;
    }

    public Accelerator getAccelerator() {
        return accelerator;
    }

    public int getThreads() {
        return threads;
    }

    public boolean isFallbackAllowed() {
        return allowFallback;
    }
}
