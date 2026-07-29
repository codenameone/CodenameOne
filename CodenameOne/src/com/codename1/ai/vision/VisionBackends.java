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

/// Vision backend selectors. {@link #auto()} chooses Apple Vision on iOS and
/// ML Kit on Android. Feature-specific ML Kit methods are separate so one
/// selector never adds unrelated OCR, barcode, face, pose, labeling, or
/// segmentation pods.
///
/// Each ML Kit selector call is also a build-time dependency marker. The
/// methods intentionally return the same runtime backend id but must remain
/// distinct so the builder can include only the selected feature. Pass the
/// selector that corresponds to the analyzer being constructed; for example,
/// the face-detection selector cannot satisfy a text recognizer and that
/// mismatched analyzer reports unsupported.
public final class VisionBackends {
    private static final VisionBackend AUTO = new NamedBackend("auto");
    private static final VisionBackend APPLE = new NamedBackend("apple-vision");
    private static final VisionBackend ML_KIT = new NamedBackend("ml-kit");

    private VisionBackends() {
    }

    /// @return the dependency-minimal platform default
    public static VisionBackend auto() {
        return AUTO;
    }

    /// Selects Apple Vision/Core Image without adding a third-party dependency.
    /// @return Apple-native backend selector
    public static VisionBackend appleVision() {
        return APPLE;
    }

    /// Selects ML Kit for text recognition on iOS. Android already uses ML
    /// Kit for the automatic backend.
    ///
    /// @return the ML Kit backend selector
    public static VisionBackend mlKitTextRecognition() {
        return ML_KIT;
    }

    /// Selects ML Kit for barcode scanning on iOS.
    /// @return the ML Kit backend selector
    public static VisionBackend mlKitBarcodeScanning() {
        return ML_KIT;
    }

    /// Selects ML Kit for face detection on iOS.
    /// @return the ML Kit backend selector
    public static VisionBackend mlKitFaceDetection() {
        return ML_KIT;
    }

    /// Selects ML Kit for image labeling on iOS.
    /// @return the ML Kit backend selector
    public static VisionBackend mlKitImageLabeling() {
        return ML_KIT;
    }

    /// Selects ML Kit for pose detection on iOS.
    /// @return the ML Kit backend selector
    public static VisionBackend mlKitPoseDetection() {
        return ML_KIT;
    }

    /// Selects ML Kit for selfie segmentation on iOS.
    /// @return the ML Kit backend selector
    public static VisionBackend mlKitSelfieSegmentation() {
        return ML_KIT;
    }

    private static final class NamedBackend implements VisionBackend {
        private final String id;

        private NamedBackend(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String toString() {
            return id;
        }
    }
}
