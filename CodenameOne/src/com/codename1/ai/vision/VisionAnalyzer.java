/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.ai.vision;

import com.codename1.util.AsyncResource;

/** Reusable, closable analyzer for still images or camera frames. */
public interface VisionAnalyzer<T> extends AutoCloseable {
    boolean isSupported();
    AsyncResource<T> process(VisionImage image);
    void close();
}
