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
package com.codename1.ai.vision;

import com.codename1.impl.VisionImpl;
import com.codename1.util.AsyncResource;

/**
 * Hand-written {@link VisionImpl} double. Completes each analysis immediately
 * with whatever {@link #result} holds, so a test drives the scanner by setting
 * that field and delivering a camera frame.
 */
class FakeVisionImpl extends VisionImpl {
    boolean supported = true;
    Object result;
    Throwable failure;
    int analyzeCount;
    int closeCount;
    VisionFeature lastFeature;

    @Override
    public boolean isSupported(VisionFeature feature, String backendId) {
        return supported;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> AsyncResource<T> analyze(VisionFeature feature, String backendId,
                                        VisionImage image, VisionOptions options) {
        analyzeCount++;
        lastFeature = feature;
        AsyncResource<T> out = new AsyncResource<T>();
        if (failure != null) {
            out.error(failure);
        } else {
            out.complete((T) result);
        }
        return out;
    }

    @Override
    public void close() {
        closeCount++;
    }
}
