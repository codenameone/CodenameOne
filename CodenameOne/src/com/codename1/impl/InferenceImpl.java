/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.impl;

import com.codename1.ai.inference.InferenceOptions;
import com.codename1.ai.inference.ModelSource;
import com.codename1.ai.inference.Tensor;
import com.codename1.ai.inference.TensorInfo;
import com.codename1.util.AsyncResource;

/** Port contract behind the built-in LiteRT inference API. @hidden */
public abstract class InferenceImpl {
    public abstract boolean isSupported();
    public abstract AsyncResource<Object> open(ModelSource source, InferenceOptions options);
    public abstract TensorInfo[] getInputs(Object handle);
    public abstract TensorInfo[] getOutputs(Object handle);
    public abstract AsyncResource<Tensor[]> run(Object handle, Tensor[] inputs);
    public abstract void resizeInput(Object handle, String name, int[] shape);
    public abstract void close(Object handle);
}
