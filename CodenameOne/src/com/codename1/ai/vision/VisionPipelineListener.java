/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.ai.vision;

/** Receives live vision results and recoverable analysis failures on the EDT. */
public interface VisionPipelineListener<T> {
    void result(T value, VisionImage source);
    void error(Throwable error);
}
