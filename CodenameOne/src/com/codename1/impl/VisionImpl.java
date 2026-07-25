/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.impl;

import com.codename1.ai.vision.VisionFeature;
import com.codename1.ai.vision.VisionImage;
import com.codename1.ai.vision.VisionOptions;
import com.codename1.util.AsyncResource;

/** Port contract behind {@code com.codename1.ai.vision}. @hidden */
public abstract class VisionImpl {
    public abstract boolean isSupported(VisionFeature feature, String backendId);
    public abstract <T> AsyncResource<T> analyze(VisionFeature feature, String backendId,
                                                  VisionImage image, VisionOptions options);
    public abstract void close();
}
