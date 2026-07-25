/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
        this.options = options == null ? new VisionOptions() : options;
    }

    public final boolean isSupported() {
        VisionImpl impl = implementation();
        return impl != null && impl.isSupported(feature, options.getBackend().getId());
    }

    public final AsyncResource<T> process(VisionImage image) {
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

    public final void close() {
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
