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
/// #### Status
///
/// The Java facade and the [NativePoseDetector] platform
/// interface are in place; the per-platform native bridges
/// (iOS Obj-C calling the underlying SDK / Android Java) land
/// in follow-up commits once device-testable bindings are
/// ready. Today every method returns an `AsyncResource` that
/// completes with an [LlmException] explaining the platform
/// isn't wired up yet -- it does *not* throw synchronously, so
/// app code can adopt the API today and the platform stubs
/// fill in transparently as they ship.
///
/// The build server's `AiDependencyTable` recognises references
/// to this class and auto-injects the matching CocoaPod /
/// Swift Package / Android Gradle dep / `Info.plist` usage
/// strings / Android permissions, so the iOS / Android build
/// is correctly configured the moment an app imports the
/// facade.
public final class PoseDetector {

    private PoseDetector() {
    }

    /// True only when the platform has a native bridge wired
    /// up.
    public static boolean isSupported() {
        NativePoseDetector bridge = NativeLookup.create(NativePoseDetector.class);
        return bridge != null && bridge.isSupported();
    }

    public static AsyncResource<float[][]> detect(byte[] imageBytes) {
        final AsyncResource<float[][]> out = new AsyncResource<float[][]>();
        final NativePoseDetector bridge = NativeLookup.create(NativePoseDetector.class);
        if (bridge == null || !bridge.isSupported()) {
            out.error(new LlmException(
                    "PoseDetector.detect is not supported on this platform yet -- "
                      + "the cn1-ai-mlkit-pose cn1lib's native bridge is pending implementation.",
                    -1, null, null, null, LlmException.ErrorType.UNKNOWN));
            return out;
        }
        Display.getInstance().scheduleBackgroundTask(new Runnable() {
            @Override
            public void run() {
                try {
                    final float[][] result = bridge.detect(imageBytes);
                    Display.getInstance().callSerially(new Runnable() {
                        @Override
                        public void run() {
                            out.complete(result);
                        }
                    });
                } catch (final Throwable t) {
                    Display.getInstance().callSerially(new Runnable() {
                        @Override
                        public void run() {
                            out.error(new LlmException(t.getMessage(), -1, null, null, t,
                                    LlmException.ErrorType.UNKNOWN));
                        }
                    });
                }
            }
        });
        return out;
    }
}
