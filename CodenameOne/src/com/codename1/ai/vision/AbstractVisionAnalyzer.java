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
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

abstract class AbstractVisionAnalyzer<T> implements VisionAnalyzer<T> {
    private final VisionFeature feature;
    private final VisionOptions options;
    private VisionImpl implementation;
    private boolean closed;

    AbstractVisionAnalyzer(VisionFeature feature, VisionOptions options) {
        this.feature = feature;
        this.options = (options == null
                ? new VisionOptions() : options).snapshot();
    }

    /// @return whether this analyzer's feature/backend is available and open
    @Override
    public final synchronized boolean isSupported() {
        VisionImpl impl = implementation();
        return impl != null && impl.isSupported(feature, options.getBackend().getId());
    }

    /// Starts one analysis using the retained native backend.
    /// @param image immutable encoded or raw input
    /// @return asynchronous typed result
    @Override
    public final synchronized AsyncResource<T> process(VisionImage image) {
        if (image == null) {
            throw new NullPointerException("image");
        }
        if (closed) {
            throw new IllegalStateException("Analyzer is closed");
        }
        VisionImpl impl = implementation();
        if (impl == null || !impl.isSupported(feature, options.getBackend().getId())) {
            AsyncResource<T> out = new AsyncResource<T>();
            out.error(new VisionException(VisionException.UNSUPPORTED,
                    feature + " is not supported by " + options.getBackend().getId()));
            return out;
        }
        return impl.analyze(feature, options.getBackend().getId(), image, options);
    }

    /// Idempotently releases the retained native backend.
    @Override
    public final synchronized void close() {
        closed = true;
        if (implementation != null) {
            implementation.close();
            implementation = null;
        }
    }

    private VisionImpl implementation() {
        if (!closed && implementation == null) {
            implementation = Display.getInstance().getVisionBackend();
        }
        return implementation;
    }
}
