/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.ai.vision;

/**
 * Vision backend selectors. {@code auto()} chooses Apple Vision on iOS and
 * ML Kit on Android. Unsupported ports report that through the analyzer.
 */
public final class VisionBackends {
    private static final VisionBackend AUTO = new NamedBackend("auto");
    private static final VisionBackend APPLE = new NamedBackend("apple-vision");
    private static final VisionBackend ML_KIT = new NamedBackend("ml-kit");

    private VisionBackends() {
    }

    public static VisionBackend auto() {
        return AUTO;
    }

    public static VisionBackend appleVision() {
        return APPLE;
    }

    public static VisionBackend mlKit() {
        return ML_KIT;
    }

    private static final class NamedBackend implements VisionBackend {
        private final String id;

        private NamedBackend(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public String toString() {
            return id;
        }
    }
}
