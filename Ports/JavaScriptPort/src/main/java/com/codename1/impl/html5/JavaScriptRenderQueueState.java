/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
