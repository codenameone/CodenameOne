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
package com.codename1.impl;

import com.codename1.ai.inference.InferenceOptions;
import com.codename1.ai.inference.ModelSource;
import com.codename1.ai.inference.Tensor;
import com.codename1.ai.inference.TensorInfo;
import com.codename1.util.AsyncResource;

/// Port contract behind the built-in LiteRT inference API. @hidden
public abstract class InferenceImpl {
    public abstract boolean isSupported();
    public abstract AsyncResource<Object> open(ModelSource source, InferenceOptions options);
    public abstract TensorInfo[] getInputs(Object handle);
    public abstract TensorInfo[] getOutputs(Object handle);
    public abstract AsyncResource<Tensor[]> run(Object handle, Tensor[] inputs);
    public abstract void resizeInput(Object handle, String name, int[] shape);
    public abstract void close(Object handle);
}
