/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */
package com.codename1.impl.html5;

import java.util.ArrayList;
import java.util.List;

public final class JavaScriptRenderQueueState<T> {
    private final ArrayList<T> ops = new ArrayList<T>();
    private int cropX;
    private int cropY;
    private int cropW;
    private int cropH;

    public boolean hasPendingOps() {
        return !ops.isEmpty();
    }

    public void replace(List<T> nextOps, int x, int y, int width, int height) {
        ops.clear();
        ops.addAll(nextOps);
        cropX = x;
        cropY = y;
        cropW = width;
        cropH = height;
    }

    public FrameSnapshot<T> snapshotAndClear() {
        ArrayList<T> snapshot = new ArrayList<T>(ops);
        ops.clear();
        return new FrameSnapshot<T>(snapshot, cropX, cropY, cropW, cropH);
    }

    public static final class FrameSnapshot<T> {
        private final List<T> ops;
        private final int cropX;
        private final int cropY;
        private final int cropW;
        private final int cropH;

        public FrameSnapshot(List<T> ops, int cropX, int cropY, int cropW, int cropH) {
            this.ops = ops;
            this.cropX = cropX;
            this.cropY = cropY;
            this.cropW = cropW;
            this.cropH = cropH;
        }

        public List<T> getOps() {
            return ops;
        }

        public boolean isEmpty() {
            return ops.isEmpty();
        }

        public int getCropX() {
            return cropX;
        }

        public int getCropY() {
            return cropY;
        }

        public int getCropW() {
            return cropW;
        }

        public int getCropH() {
            return cropH;
        }
    }
}
