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

package com.codename1.ai.mlkit.pose;

import com.codename1.ai.LlmException;
import com.codename1.system.NativeLookup;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

/// ML Kit Pose Detection.
///
/// Returns skeletal landmarks for human bodies detected in an image.
///
public final class PoseDetector {
    private PoseDetector() { }

    /// True only when the running platform has a native bridge wired up.
    public static boolean isSupported() {
        NativePoseDetector bridge = NativeLookup.create(NativePoseDetector.class);
        return bridge != null && bridge.isSupported();
    }

    /// Returns 33 landmark triples (x,y,confidence) per detected pose
    /// packed as `float[3 * 33]`.
    public static AsyncResource<float[]> detect(final byte[] imageBytes) {
        final AsyncResource<float[]> out = new AsyncResource<float[]>();
        final NativePoseDetector bridge = NativeLookup.create(NativePoseDetector.class);
        if (bridge == null || !bridge.isSupported()) {
            out.error(new LlmException("PoseDetector.detect is not supported on this platform.",
                    -1, null, null, null, LlmException.ErrorType.UNKNOWN));
            return out;
        }
        Display.getInstance().scheduleBackgroundTask(new Runnable() {
            @Override public void run() {
                try {
                    final float[] r = bridge.detect(imageBytes);
                    Display.getInstance().callSerially(new Runnable() {
                        @Override public void run() { out.complete(r == null ? new float[0] : r); }
                    });
                } catch (final Throwable t) {
                    Display.getInstance().callSerially(new Runnable() {
                        @Override public void run() {
                            out.error(new LlmException("PoseDetector.detect failed: " + t.getMessage(),
                                    -1, null, null, t, LlmException.ErrorType.UNKNOWN));
                        }
                    });
                }
            }
        });
        return out;
    }
}
