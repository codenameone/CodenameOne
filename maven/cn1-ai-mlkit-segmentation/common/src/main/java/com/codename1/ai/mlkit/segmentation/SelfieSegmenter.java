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

package com.codename1.ai.mlkit.segmentation;

import com.codename1.ai.LlmException;
import com.codename1.system.NativeLookup;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

/// ML Kit Selfie Segmentation.
///
/// Returns a per-pixel mask separating a person in the foreground from the background.
///
public final class SelfieSegmenter {
    private SelfieSegmenter() { }

    /// True only when the running platform has a native bridge wired up.
    public static boolean isSupported() {
        NativeSelfieSegmenter bridge = NativeLookup.create(NativeSelfieSegmenter.class);
        return bridge != null && bridge.isSupported();
    }

    /// Returns a per-pixel mask separating foreground (person) from
    /// background as `byte[width * height]` (0=background, 255=foreground).
    public static AsyncResource<byte[]> segment(final byte[] imageBytes) {
        final AsyncResource<byte[]> out = new AsyncResource<byte[]>();
        final NativeSelfieSegmenter bridge = NativeLookup.create(NativeSelfieSegmenter.class);
        if (bridge == null || !bridge.isSupported()) {
            out.error(new LlmException("SelfieSegmenter.segment is not supported on this platform.",
                    -1, null, null, null, LlmException.ErrorType.UNKNOWN));
            return out;
        }
        Display.getInstance().scheduleBackgroundTask(new Runnable() {
            @Override public void run() {
                try {
                    final byte[] r = bridge.segment(imageBytes);
                    Display.getInstance().callSerially(new Runnable() {
                        @Override public void run() { out.complete(r == null ? new byte[0] : r); }
                    });
                } catch (final Throwable t) {
                    Display.getInstance().callSerially(new Runnable() {
                        @Override public void run() {
                            out.error(new LlmException("SelfieSegmenter.segment failed: " + t.getMessage(),
                                    -1, null, null, t, LlmException.ErrorType.UNKNOWN));
                        }
                    });
                }
            }
        });
        return out;
    }
}
