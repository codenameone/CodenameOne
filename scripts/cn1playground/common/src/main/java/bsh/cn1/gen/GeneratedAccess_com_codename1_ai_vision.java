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

package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_ai_vision {
    private GeneratedAccess_com_codename1_ai_vision() {
    }

    public static Class<?> findClass(String name) {
        if (name == null) {
            return null;
        }
        int dot = name.lastIndexOf('.');
        int dollar = name.lastIndexOf('$');
        int sep = dot > dollar ? dot : dollar;
        if (sep < 0 || sep == name.length() - 1) {
            return null;
        }
        return findClassBySimpleName(name.substring(sep + 1));
    }

    public static Class<?> findClassBySimpleName(String simpleName) {
        Class<?> found0 = findClassChunk0(simpleName);
        if (found0 != null) {
            return found0;
        }
        return null;
    }


    private static Class<?> findClassChunk0(String simpleName) {
        if ("Barcode".equals(simpleName)) {
            return com.codename1.ai.vision.Barcode.class;
        }
        if ("BarcodeFormat".equals(simpleName)) {
            return com.codename1.ai.vision.BarcodeFormat.class;
        }
        if ("BarcodeScanner".equals(simpleName)) {
            return com.codename1.ai.vision.BarcodeScanner.class;
        }
        if ("CodeScanner".equals(simpleName)) {
            return com.codename1.ai.vision.CodeScanner.class;
        }
        if ("CodeScannerOptions".equals(simpleName)) {
            return com.codename1.ai.vision.CodeScannerOptions.class;
        }
        if ("DocumentScanResult".equals(simpleName)) {
            return com.codename1.ai.vision.DocumentScanResult.class;
        }
        if ("DocumentScanner".equals(simpleName)) {
            return com.codename1.ai.vision.DocumentScanner.class;
        }
        if ("Face".equals(simpleName)) {
            return com.codename1.ai.vision.Face.class;
        }
        if ("FaceDetector".equals(simpleName)) {
            return com.codename1.ai.vision.FaceDetector.class;
        }
        if ("FaceLandmarks".equals(simpleName)) {
            return com.codename1.ai.vision.FaceLandmarks.class;
        }
        if ("ImageLabel".equals(simpleName)) {
            return com.codename1.ai.vision.ImageLabel.class;
        }
        if ("ImageLabeler".equals(simpleName)) {
            return com.codename1.ai.vision.ImageLabeler.class;
        }
        if ("Pose".equals(simpleName)) {
            return com.codename1.ai.vision.Pose.class;
        }
        if ("Landmark".equals(simpleName)) {
            return com.codename1.ai.vision.Pose.Landmark.class;
        }
        if ("PoseDetector".equals(simpleName)) {
            return com.codename1.ai.vision.PoseDetector.class;
        }
        if ("PoseLandmarks".equals(simpleName)) {
            return com.codename1.ai.vision.PoseLandmarks.class;
        }
        if ("SegmentationMask".equals(simpleName)) {
            return com.codename1.ai.vision.SegmentationMask.class;
        }
        if ("SelfieSegmenter".equals(simpleName)) {
            return com.codename1.ai.vision.SelfieSegmenter.class;
        }
        if ("TextRecognitionResult".equals(simpleName)) {
            return com.codename1.ai.vision.TextRecognitionResult.class;
        }
        if ("TextBlock".equals(simpleName)) {
            return com.codename1.ai.vision.TextRecognitionResult.TextBlock.class;
        }
        if ("TextRecognizer".equals(simpleName)) {
            return com.codename1.ai.vision.TextRecognizer.class;
        }
        if ("TextScript".equals(simpleName)) {
            return com.codename1.ai.vision.TextScript.class;
        }
        if ("VisionAnalyzer".equals(simpleName)) {
            return com.codename1.ai.vision.VisionAnalyzer.class;
        }
        if ("VisionBackend".equals(simpleName)) {
            return com.codename1.ai.vision.VisionBackend.class;
        }
        if ("VisionBackends".equals(simpleName)) {
            return com.codename1.ai.vision.VisionBackends.class;
        }
        if ("VisionCameraView".equals(simpleName)) {
            return com.codename1.ai.vision.VisionCameraView.class;
        }
        if ("VisionException".equals(simpleName)) {
            return com.codename1.ai.vision.VisionException.class;
        }
        if ("VisionFeature".equals(simpleName)) {
            return com.codename1.ai.vision.VisionFeature.class;
        }
        if ("VisionImage".equals(simpleName)) {
            return com.codename1.ai.vision.VisionImage.class;
        }
        if ("VisionMetadata".equals(simpleName)) {
            return com.codename1.ai.vision.VisionMetadata.class;
        }
        if ("VisionOptions".equals(simpleName)) {
            return com.codename1.ai.vision.VisionOptions.class;
        }
        if ("VisionPipeline".equals(simpleName)) {
            return com.codename1.ai.vision.VisionPipeline.class;
        }
        if ("VisionPipelineListener".equals(simpleName)) {
            return com.codename1.ai.vision.VisionPipelineListener.class;
        }
        if ("VisionPoint".equals(simpleName)) {
            return com.codename1.ai.vision.VisionPoint.class;
        }
        if ("VisionRect".equals(simpleName)) {
            return com.codename1.ai.vision.VisionRect.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.ai.vision.Barcode.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, byte[].class, com.codename1.ai.vision.VisionRect.class, com.codename1.ai.vision.VisionPoint[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, byte[].class, com.codename1.ai.vision.VisionRect.class, com.codename1.ai.vision.VisionPoint[].class}, false);
                return new com.codename1.ai.vision.Barcode((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (byte[]) adaptedArgs[2], (com.codename1.ai.vision.VisionRect) adaptedArgs[3], (com.codename1.ai.vision.VisionPoint[]) adaptedArgs[4]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, byte[].class, com.codename1.ai.vision.VisionRect.class, com.codename1.ai.vision.VisionPoint[].class, com.codename1.ai.vision.VisionMetadata.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, byte[].class, com.codename1.ai.vision.VisionRect.class, com.codename1.ai.vision.VisionPoint[].class, com.codename1.ai.vision.VisionMetadata.class}, false);
                return new com.codename1.ai.vision.Barcode((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (byte[]) adaptedArgs[2], (com.codename1.ai.vision.VisionRect) adaptedArgs[3], (com.codename1.ai.vision.VisionPoint[]) adaptedArgs[4], (com.codename1.ai.vision.VisionMetadata) adaptedArgs[5]);
            }
        }
        if (type == com.codename1.ai.vision.BarcodeScanner.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.ai.vision.BarcodeScanner();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ai.vision.VisionOptions.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ai.vision.VisionOptions.class}, false);
                return new com.codename1.ai.vision.BarcodeScanner((com.codename1.ai.vision.VisionOptions) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.ai.vision.CodeScannerOptions.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.ai.vision.CodeScannerOptions();
            }
        }
        if (type == com.codename1.ai.vision.DocumentScanResult.class) {
            if (matches(safeArgs, new Class<?>[]{byte[][].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[][].class}, false);
                return new com.codename1.ai.vision.DocumentScanResult((byte[][]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{byte[][].class, com.codename1.ai.vision.VisionMetadata.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[][].class, com.codename1.ai.vision.VisionMetadata.class}, false);
                return new com.codename1.ai.vision.DocumentScanResult((byte[][]) adaptedArgs[0], (com.codename1.ai.vision.VisionMetadata) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.ai.vision.DocumentScanner.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.ai.vision.DocumentScanner();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ai.vision.VisionOptions.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ai.vision.VisionOptions.class}, false);
                return new com.codename1.ai.vision.DocumentScanner((com.codename1.ai.vision.VisionOptions) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.ai.vision.Face.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ai.vision.VisionRect.class, java.util.Map.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ai.vision.VisionRect.class, java.util.Map.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class}, false);
                return new com.codename1.ai.vision.Face((com.codename1.ai.vision.VisionRect) adaptedArgs[0], (java.util.Map) adaptedArgs[1], ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), ((Number) adaptedArgs[4]).floatValue(), ((Number) adaptedArgs[5]).floatValue(), toIntValue(adaptedArgs[6]));
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ai.vision.VisionRect.class, java.util.Map.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.ai.vision.VisionMetadata.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ai.vision.VisionRect.class, java.util.Map.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.ai.vision.VisionMetadata.class}, false);
                return new com.codename1.ai.vision.Face((com.codename1.ai.vision.VisionRect) adaptedArgs[0], (java.util.Map) adaptedArgs[1], ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), ((Number) adaptedArgs[4]).floatValue(), ((Number) adaptedArgs[5]).floatValue(), toIntValue(adaptedArgs[6]), (com.codename1.ai.vision.VisionMetadata) adaptedArgs[7]);
            }
        }
        if (type == com.codename1.ai.vision.FaceDetector.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.ai.vision.FaceDetector();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ai.vision.VisionOptions.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ai.vision.VisionOptions.class}, false);
                return new com.codename1.ai.vision.FaceDetector((com.codename1.ai.vision.VisionOptions) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.ai.vision.ImageLabel.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Float.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Float.class, java.lang.Integer.class}, false);
                return new com.codename1.ai.vision.ImageLabel((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue(), toIntValue(adaptedArgs[2]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.ai.vision.VisionMetadata.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Float.class, java.lang.Integer.class, com.codename1.ai.vision.VisionMetadata.class}, false);
                return new com.codename1.ai.vision.ImageLabel((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue(), toIntValue(adaptedArgs[2]), (com.codename1.ai.vision.VisionMetadata) adaptedArgs[3]);
            }
        }
        if (type == com.codename1.ai.vision.ImageLabeler.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.ai.vision.ImageLabeler();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ai.vision.VisionOptions.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ai.vision.VisionOptions.class}, false);
                return new com.codename1.ai.vision.ImageLabeler((com.codename1.ai.vision.VisionOptions) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.ai.vision.Pose.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ai.vision.Pose.Landmark[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ai.vision.Pose.Landmark[].class}, false);
                return new com.codename1.ai.vision.Pose((com.codename1.ai.vision.Pose.Landmark[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ai.vision.Pose.Landmark[].class, com.codename1.ai.vision.VisionMetadata.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ai.vision.Pose.Landmark[].class, com.codename1.ai.vision.VisionMetadata.class}, false);
                return new com.codename1.ai.vision.Pose((com.codename1.ai.vision.Pose.Landmark[]) adaptedArgs[0], (com.codename1.ai.vision.VisionMetadata) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.ai.vision.Pose.Landmark.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ai.vision.VisionPoint.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ai.vision.VisionPoint.class, java.lang.Float.class}, false);
                return new com.codename1.ai.vision.Pose.Landmark((java.lang.String) adaptedArgs[0], (com.codename1.ai.vision.VisionPoint) adaptedArgs[1], ((Number) adaptedArgs[2]).floatValue());
            }
        }
        if (type == com.codename1.ai.vision.PoseDetector.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.ai.vision.PoseDetector();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ai.vision.VisionOptions.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ai.vision.VisionOptions.class}, false);
                return new com.codename1.ai.vision.PoseDetector((com.codename1.ai.vision.VisionOptions) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.ai.vision.SegmentationMask.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, float[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, float[].class}, false);
                return new com.codename1.ai.vision.SegmentationMask(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), (float[]) adaptedArgs[2]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, float[].class, com.codename1.ai.vision.VisionMetadata.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, float[].class, com.codename1.ai.vision.VisionMetadata.class}, false);
                return new com.codename1.ai.vision.SegmentationMask(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), (float[]) adaptedArgs[2], (com.codename1.ai.vision.VisionMetadata) adaptedArgs[3]);
            }
        }
        if (type == com.codename1.ai.vision.SelfieSegmenter.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.ai.vision.SelfieSegmenter();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ai.vision.VisionOptions.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ai.vision.VisionOptions.class}, false);
                return new com.codename1.ai.vision.SelfieSegmenter((com.codename1.ai.vision.VisionOptions) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.ai.vision.TextRecognitionResult.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ai.vision.TextRecognitionResult.TextBlock[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ai.vision.TextRecognitionResult.TextBlock[].class}, false);
                return new com.codename1.ai.vision.TextRecognitionResult((java.lang.String) adaptedArgs[0], (com.codename1.ai.vision.TextRecognitionResult.TextBlock[]) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ai.vision.TextRecognitionResult.TextBlock[].class, com.codename1.ai.vision.VisionMetadata.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ai.vision.TextRecognitionResult.TextBlock[].class, com.codename1.ai.vision.VisionMetadata.class}, false);
                return new com.codename1.ai.vision.TextRecognitionResult((java.lang.String) adaptedArgs[0], (com.codename1.ai.vision.TextRecognitionResult.TextBlock[]) adaptedArgs[1], (com.codename1.ai.vision.VisionMetadata) adaptedArgs[2]);
            }
        }
        if (type == com.codename1.ai.vision.TextRecognitionResult.TextBlock.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Float.class, com.codename1.ai.vision.VisionRect.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Float.class, com.codename1.ai.vision.VisionRect.class, java.lang.String.class}, false);
                return new com.codename1.ai.vision.TextRecognitionResult.TextBlock((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue(), (com.codename1.ai.vision.VisionRect) adaptedArgs[2], (java.lang.String) adaptedArgs[3]);
            }
        }
        if (type == com.codename1.ai.vision.TextRecognizer.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.ai.vision.TextRecognizer();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ai.vision.VisionOptions.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ai.vision.VisionOptions.class}, false);
                return new com.codename1.ai.vision.TextRecognizer((com.codename1.ai.vision.VisionOptions) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.ai.vision.VisionCameraView.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ai.vision.VisionAnalyzer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ai.vision.VisionAnalyzer.class}, false);
                return new com.codename1.ai.vision.VisionCameraView((com.codename1.ai.vision.VisionAnalyzer) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.ai.vision.VisionException.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class}, false);
                return new com.codename1.ai.vision.VisionException(toIntValue(adaptedArgs[0]), (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class, java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class, java.lang.Throwable.class}, false);
                return new com.codename1.ai.vision.VisionException(toIntValue(adaptedArgs[0]), (java.lang.String) adaptedArgs[1], (java.lang.Throwable) adaptedArgs[2]);
            }
        }
        if (type == com.codename1.ai.vision.VisionMetadata.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.ai.vision.VisionMetadata((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Map.class}, false);
                return new com.codename1.ai.vision.VisionMetadata((java.lang.String) adaptedArgs[0], (java.util.Map) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.ai.vision.VisionOptions.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.ai.vision.VisionOptions();
            }
        }
        if (type == com.codename1.ai.vision.VisionPipeline.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.camera.CameraSession.class, com.codename1.ai.vision.VisionAnalyzer.class, com.codename1.ai.vision.VisionPipelineListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.camera.CameraSession.class, com.codename1.ai.vision.VisionAnalyzer.class, com.codename1.ai.vision.VisionPipelineListener.class}, false);
                return new com.codename1.ai.vision.VisionPipeline((com.codename1.camera.CameraSession) adaptedArgs[0], (com.codename1.ai.vision.VisionAnalyzer) adaptedArgs[1], (com.codename1.ai.vision.VisionPipelineListener) adaptedArgs[2]);
            }
        }
        if (type == com.codename1.ai.vision.VisionPoint.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                return new com.codename1.ai.vision.VisionPoint(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue());
            }
        }
        if (type == com.codename1.ai.vision.VisionRect.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                return new com.codename1.ai.vision.VisionRect(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue());
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.ai.vision.BarcodeFormat.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.ai.vision.CodeScanner.class) return invokeStatic1(name, safeArgs);
        if (type == com.codename1.ai.vision.TextScript.class) return invokeStatic2(name, safeArgs);
        if (type == com.codename1.ai.vision.VisionBackends.class) return invokeStatic3(name, safeArgs);
        if (type == com.codename1.ai.vision.VisionImage.class) return invokeStatic4(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("matches".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ai.vision.Barcode.class, java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ai.vision.Barcode.class, java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.String) adaptedArgs[i];
                }
                return com.codename1.ai.vision.BarcodeFormat.matches((com.codename1.ai.vision.Barcode) adaptedArgs[0], varArgs);
            }
        }
        throw unsupportedStatic(com.codename1.ai.vision.BarcodeFormat.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("isSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ai.vision.CodeScanner.isSupported();
            }
        }
        if ("scan".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ai.vision.CodeScanner.scan();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ai.vision.CodeScannerOptions.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ai.vision.CodeScannerOptions.class}, false);
                return com.codename1.ai.vision.CodeScanner.scan((com.codename1.ai.vision.CodeScannerOptions) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.ai.vision.CodeScanner.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("chinese".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ai.vision.TextScript.chinese();
            }
        }
        if ("devanagari".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ai.vision.TextScript.devanagari();
            }
        }
        if ("japanese".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ai.vision.TextScript.japanese();
            }
        }
        if ("korean".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ai.vision.TextScript.korean();
            }
        }
        if ("latin".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ai.vision.TextScript.latin();
            }
        }
        throw unsupportedStatic(com.codename1.ai.vision.TextScript.class, name, safeArgs);
    }

    private static Object invokeStatic3(String name, Object[] safeArgs) throws Exception {
        if ("appleVision".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ai.vision.VisionBackends.appleVision();
            }
        }
        if ("auto".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ai.vision.VisionBackends.auto();
            }
        }
        if ("mlKitBarcodeScanning".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ai.vision.VisionBackends.mlKitBarcodeScanning();
            }
        }
        if ("mlKitFaceDetection".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ai.vision.VisionBackends.mlKitFaceDetection();
            }
        }
        if ("mlKitImageLabeling".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ai.vision.VisionBackends.mlKitImageLabeling();
            }
        }
        if ("mlKitPoseDetection".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ai.vision.VisionBackends.mlKitPoseDetection();
            }
        }
        if ("mlKitSelfieSegmentation".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ai.vision.VisionBackends.mlKitSelfieSegmentation();
            }
        }
        if ("mlKitTextRecognition".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ai.vision.VisionBackends.mlKitTextRecognition();
            }
        }
        throw unsupportedStatic(com.codename1.ai.vision.VisionBackends.class, name, safeArgs);
    }

    private static Object invokeStatic4(String name, Object[] safeArgs) throws Exception {
        if ("encoded".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return com.codename1.ai.vision.VisionImage.encoded((byte[]) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class}, false);
                return com.codename1.ai.vision.VisionImage.encoded((byte[]) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        if ("fromCameraFrame".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.camera.CameraFrame.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.camera.CameraFrame.class}, false);
                return com.codename1.ai.vision.VisionImage.fromCameraFrame((com.codename1.camera.CameraFrame) adaptedArgs[0]);
            }
        }
        if ("fromFile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.ai.vision.VisionImage.fromFile((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("fromImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                return com.codename1.ai.vision.VisionImage.fromImage((com.codename1.ui.Image) adaptedArgs[0]);
            }
        }
        if ("pixels".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.camera.FrameFormat.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.camera.FrameFormat.class, java.lang.Integer.class}, false);
                return com.codename1.ai.vision.VisionImage.pixels((byte[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), (com.codename1.camera.FrameFormat) adaptedArgs[3], toIntValue(adaptedArgs[4]));
            }
        }
        throw unsupportedStatic(com.codename1.ai.vision.VisionImage.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.ai.vision.Barcode) {
            try {
                return invoke0((com.codename1.ai.vision.Barcode) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.vision.CodeScannerOptions) {
            try {
                return invoke1((com.codename1.ai.vision.CodeScannerOptions) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.vision.DocumentScanResult) {
            try {
                return invoke2((com.codename1.ai.vision.DocumentScanResult) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.vision.Face) {
            try {
                return invoke3((com.codename1.ai.vision.Face) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.vision.ImageLabel) {
            try {
                return invoke4((com.codename1.ai.vision.ImageLabel) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.vision.Pose) {
            try {
                return invoke5((com.codename1.ai.vision.Pose) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.vision.Pose.Landmark) {
            try {
                return invoke6((com.codename1.ai.vision.Pose.Landmark) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.vision.SegmentationMask) {
            try {
                return invoke7((com.codename1.ai.vision.SegmentationMask) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.vision.TextRecognitionResult) {
            try {
                return invoke8((com.codename1.ai.vision.TextRecognitionResult) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.vision.TextRecognitionResult.TextBlock) {
            try {
                return invoke9((com.codename1.ai.vision.TextRecognitionResult.TextBlock) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.vision.TextScript) {
            try {
                return invoke10((com.codename1.ai.vision.TextScript) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.vision.VisionCameraView) {
            try {
                return invoke11((com.codename1.ai.vision.VisionCameraView) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.vision.VisionException) {
            try {
                return invoke12((com.codename1.ai.vision.VisionException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.vision.VisionImage) {
            try {
                return invoke13((com.codename1.ai.vision.VisionImage) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.vision.VisionMetadata) {
            try {
                return invoke14((com.codename1.ai.vision.VisionMetadata) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.vision.VisionOptions) {
            try {
                return invoke15((com.codename1.ai.vision.VisionOptions) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.vision.VisionPipeline) {
            try {
                return invoke16((com.codename1.ai.vision.VisionPipeline) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.vision.VisionPoint) {
            try {
                return invoke17((com.codename1.ai.vision.VisionPoint) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.vision.VisionRect) {
            try {
                return invoke18((com.codename1.ai.vision.VisionRect) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.vision.VisionAnalyzer) {
            try {
                return invoke19((com.codename1.ai.vision.VisionAnalyzer) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.vision.VisionBackend) {
            try {
                return invoke20((com.codename1.ai.vision.VisionBackend) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.vision.VisionPipelineListener) {
            try {
                return invoke21((com.codename1.ai.vision.VisionPipelineListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.ai.vision.Barcode typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getBounds".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBounds();
            }
        }
        if ("getCorners".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCorners();
            }
        }
        if ("getFormat".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFormat();
            }
        }
        if ("getMetadata".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMetadata();
            }
        }
        if ("getRawBytes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRawBytes();
            }
        }
        if ("getValue".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValue();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.ai.vision.CodeScannerOptions typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("facing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.camera.CameraFacing.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.camera.CameraFacing.class}, false);
                return typedTarget.facing((com.codename1.camera.CameraFacing) adaptedArgs[0]);
            }
        }
        if ("formats".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.String) adaptedArgs[i];
                }
                return typedTarget.formats(varArgs);
            }
        }
        if ("getFacing".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFacing();
            }
        }
        if ("getFormats".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFormats();
            }
        }
        if ("getHint".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHint();
            }
        }
        if ("getTitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTitle();
            }
        }
        if ("getVisionOptions".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVisionOptions();
            }
        }
        if ("hint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.hint((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("isTorchButton".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTorchButton();
            }
        }
        if ("title".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.title((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("torchButton".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.torchButton(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("visionOptions".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ai.vision.VisionOptions.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ai.vision.VisionOptions.class}, false);
                return typedTarget.visionOptions((com.codename1.ai.vision.VisionOptions) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.ai.vision.DocumentScanResult typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getMetadata".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMetadata();
            }
        }
        if ("getPage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getPage(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getPageCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPageCount();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.ai.vision.Face typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getBounds".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBounds();
            }
        }
        if ("getLandmark".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getLandmark((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getLandmarks".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLandmarks();
            }
        }
        if ("getMetadata".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMetadata();
            }
        }
        if ("getPitch".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPitch();
            }
        }
        if ("getRoll".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRoll();
            }
        }
        if ("getSmilingProbability".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSmilingProbability();
            }
        }
        if ("getTrackingId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTrackingId();
            }
        }
        if ("getYaw".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getYaw();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.ai.vision.ImageLabel typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getConfidence".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getConfidence();
            }
        }
        if ("getIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIndex();
            }
        }
        if ("getMetadata".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMetadata();
            }
        }
        if ("getText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getText();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.ai.vision.Pose typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getLandmark".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getLandmark((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getLandmarks".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLandmarks();
            }
        }
        if ("getMetadata".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMetadata();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.ai.vision.Pose.Landmark typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getConfidence".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getConfidence();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getPosition".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPosition();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.ai.vision.SegmentationMask typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("cutOut".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, java.lang.Float.class}, false);
                return typedTarget.cutOut((com.codename1.ui.Image) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue());
            }
        }
        if ("getConfidence".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getConfidence();
            }
        }
        if ("getConfidenceAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getConfidenceAt(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeight();
            }
        }
        if ("getMetadata".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMetadata();
            }
        }
        if ("getWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidth();
            }
        }
        if ("toMaskImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.toMaskImage(toIntValue(adaptedArgs[0]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.ai.vision.TextRecognitionResult typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getBlocks".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBlocks();
            }
        }
        if ("getMetadata".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMetadata();
            }
        }
        if ("getText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getText();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.ai.vision.TextRecognitionResult.TextBlock typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getBounds".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBounds();
            }
        }
        if ("getConfidence".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getConfidence();
            }
        }
        if ("getLanguageTag".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLanguageTag();
            }
        }
        if ("getText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getText();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.ai.vision.TextScript typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke11(com.codename1.ai.vision.VisionCameraView typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("accessibilityChanged".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.accessibilityChanged(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.accessibilityChanged(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.add((com.codename1.ui.Component) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                return typedTarget.add((com.codename1.ui.Image) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.add((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Image.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0], (com.codename1.ui.Image) adaptedArgs[1]);
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return typedTarget.addAll(varArgs);
            }
        }
        if ("addComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.addComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.ui.Component.class}, false);
                typedTarget.addComponent(toIntValue(adaptedArgs[0]), (com.codename1.ui.Component) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class, com.codename1.ui.Component.class}, false);
                typedTarget.addComponent(toIntValue(adaptedArgs[0]), (java.lang.Object) adaptedArgs[1], (com.codename1.ui.Component) adaptedArgs[2]); return null;
            }
        }
        if ("addContextMenuListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addContextMenuListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addDragFinishedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addDragFinishedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addDragOverListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addDragOverListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addDropListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addDropListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addFocusListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false);
                typedTarget.addFocusListener((com.codename1.ui.events.FocusListener) adaptedArgs[0]); return null;
            }
        }
        if ("addLongPressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addLongPressListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addMouseWheelListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addMouseWheelListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPointerDraggedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addPointerDraggedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPointerPressedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addPointerPressedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPointerReleasedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addPointerReleasedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPullToRefresh".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                typedTarget.addPullToRefresh((java.lang.Runnable) adaptedArgs[0]); return null;
            }
        }
        if ("addScrollListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false);
                typedTarget.addScrollListener((com.codename1.ui.events.ScrollListener) adaptedArgs[0]); return null;
            }
        }
        if ("addStateChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addStateChangeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addStylusListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addStylusListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("animate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.animate();
            }
        }
        if ("animateHierarchy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateHierarchy(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("animateHierarchyAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateHierarchyAndWait(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("animateHierarchyFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateHierarchyFade(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("animateHierarchyFadeAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateHierarchyFadeAndWait(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("animateLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateLayout(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("animateLayoutAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateLayoutAndWait(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("animateLayoutFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateLayoutFade(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("animateLayoutFadeAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateLayoutFadeAndWait(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("animateUnlayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false);
                typedTarget.animateUnlayout(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), (java.lang.Runnable) adaptedArgs[2]); return null;
            }
        }
        if ("animateUnlayoutAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateUnlayoutAndWait(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("announceForAccessibility".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.announceForAccessibility((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("applyRTL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.applyRTL(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("bindProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false);
                typedTarget.bindProperty((java.lang.String) adaptedArgs[0], (com.codename1.cloud.BindTarget) adaptedArgs[1]); return null;
            }
        }
        if ("blocksSideSwipe".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.blocksSideSwipe();
            }
        }
        if ("clearClientProperties".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearClientProperties(); return null;
            }
        }
        if ("close".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.close(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.contains((com.codename1.ui.Component) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.contains(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("containsOrOwns".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.containsOrOwns(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("createAnimateHierarchy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.createAnimateHierarchy(toIntValue(adaptedArgs[0]));
            }
        }
        if ("createAnimateHierarchyFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.createAnimateHierarchyFade(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("createAnimateLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.createAnimateLayout(toIntValue(adaptedArgs[0]));
            }
        }
        if ("createAnimateLayoutFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.createAnimateLayoutFade(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("createAnimateLayoutFadeAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.createAnimateLayoutFadeAndWait(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("createAnimateUnlayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false);
                return typedTarget.createAnimateUnlayout(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), (java.lang.Runnable) adaptedArgs[2]);
            }
        }
        if ("createReplaceTransition".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false);
                return typedTarget.createReplaceTransition((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2]);
            }
        }
        if ("createStyleAnimation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return typedTarget.createStyleAnimation((java.lang.String) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        if ("drop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.drop((com.codename1.ui.Component) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2])); return null;
            }
        }
        if ("findDropTargetAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.findDropTargetAt(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("findFirstFocusable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.findFirstFocusable();
            }
        }
        if ("flushReplace".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flushReplace(); return null;
            }
        }
        if ("forceRevalidate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.forceRevalidate(); return null;
            }
        }
        if ("getAbsoluteX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAbsoluteX();
            }
        }
        if ("getAbsoluteY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAbsoluteY();
            }
        }
        if ("getAccessibilityNode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAccessibilityNode();
            }
        }
        if ("getAccessibilityText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAccessibilityText();
            }
        }
        if ("getAllStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAllStyles();
            }
        }
        if ("getAnimationManager".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAnimationManager();
            }
        }
        if ("getBaseline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getBaseline(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getBaselineResizeBehavior".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBaselineResizeBehavior();
            }
        }
        if ("getBindablePropertyNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBindablePropertyNames();
            }
        }
        if ("getBindablePropertyTypes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBindablePropertyTypes();
            }
        }
        if ("getBottomGap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBottomGap();
            }
        }
        if ("getBoundPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getBoundPropertyValue((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                return typedTarget.getBounds((com.codename1.ui.geom.Rectangle) adaptedArgs[0]);
            }
        }
        if ("getChildrenAsList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.getChildrenAsList(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("getClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getClientProperty((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getClosestComponentTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getClosestComponentTo(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getCloudBoundProperty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCloudBoundProperty();
            }
        }
        if ("getCloudDestinationProperty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCloudDestinationProperty();
            }
        }
        if ("getComponentAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getComponentAt(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getComponentAt(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getComponentCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponentCount();
            }
        }
        if ("getComponentForm".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponentForm();
            }
        }
        if ("getComponentIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.getComponentIndex((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("getComponentState".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponentState();
            }
        }
        if ("getCursor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCursor();
            }
        }
        if ("getDirtyRegion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDirtyRegion();
            }
        }
        if ("getDisabledStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDisabledStyle();
            }
        }
        if ("getDragTransparency".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDragTransparency();
            }
        }
        if ("getDraggedx".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDraggedx();
            }
        }
        if ("getDraggedy".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDraggedy();
            }
        }
        if ("getEditingDelegate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEditingDelegate();
            }
        }
        if ("getFacing".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFacing();
            }
        }
        if ("getHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeight();
            }
        }
        if ("getInlineAllStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineAllStyles();
            }
        }
        if ("getInlineDisabledStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineDisabledStyles();
            }
        }
        if ("getInlinePressedStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlinePressedStyles();
            }
        }
        if ("getInlineSelectedStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineSelectedStyles();
            }
        }
        if ("getInlineStylesTheme".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineStylesTheme();
            }
        }
        if ("getInlineUnselectedStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineUnselectedStyles();
            }
        }
        if ("getInnerHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerHeight();
            }
        }
        if ("getInnerPreferredH".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerPreferredH();
            }
        }
        if ("getInnerPreferredW".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerPreferredW();
            }
        }
        if ("getInnerWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerWidth();
            }
        }
        if ("getInnerX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerX();
            }
        }
        if ("getInnerY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerY();
            }
        }
        if ("getLabelForComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLabelForComponent();
            }
        }
        if ("getLayout".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayout();
            }
        }
        if ("getLayoutHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayoutHeight();
            }
        }
        if ("getLayoutWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayoutWidth();
            }
        }
        if ("getLeadComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLeadComponent();
            }
        }
        if ("getLeadParent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLeadParent();
            }
        }
        if ("getListener".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getListener();
            }
        }
        if ("getMaxFps".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxFps();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getNativeOverlay".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNativeOverlay();
            }
        }
        if ("getNextFocusDown".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusDown();
            }
        }
        if ("getNextFocusLeft".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusLeft();
            }
        }
        if ("getNextFocusRight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusRight();
            }
        }
        if ("getNextFocusUp".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusUp();
            }
        }
        if ("getOuterHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterHeight();
            }
        }
        if ("getOuterPreferredH".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterPreferredH();
            }
        }
        if ("getOuterPreferredW".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterPreferredW();
            }
        }
        if ("getOuterWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterWidth();
            }
        }
        if ("getOuterX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterX();
            }
        }
        if ("getOuterY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterY();
            }
        }
        if ("getOwner".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOwner();
            }
        }
        if ("getParent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getParent();
            }
        }
        if ("getPreferredH".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredH();
            }
        }
        if ("getPreferredSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredSize();
            }
        }
        if ("getPreferredSizeStr".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredSizeStr();
            }
        }
        if ("getPreferredTabIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredTabIndex();
            }
        }
        if ("getPreferredW".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredW();
            }
        }
        if ("getPressedStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPressedStyle();
            }
        }
        if ("getPropertyNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPropertyNames();
            }
        }
        if ("getPropertyTypeNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPropertyTypeNames();
            }
        }
        if ("getPropertyTypes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPropertyTypes();
            }
        }
        if ("getPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getPropertyValue((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getResponderAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getResponderAt(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getSafeAreaRoot".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSafeAreaRoot();
            }
        }
        if ("getSameHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSameHeight();
            }
        }
        if ("getSameWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSameWidth();
            }
        }
        if ("getScaleType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScaleType();
            }
        }
        if ("getScrollAnimationSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollAnimationSpeed();
            }
        }
        if ("getScrollDimension".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollDimension();
            }
        }
        if ("getScrollIncrement".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollIncrement();
            }
        }
        if ("getScrollOpacity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollOpacity();
            }
        }
        if ("getScrollOpacityChangeSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollOpacityChangeSpeed();
            }
        }
        if ("getScrollX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollX();
            }
        }
        if ("getScrollY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollY();
            }
        }
        if ("getScrollable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollable();
            }
        }
        if ("getSelectCommandText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectCommandText();
            }
        }
        if ("getSelectedRect".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectedRect();
            }
        }
        if ("getSelectedStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectedStyle();
            }
        }
        if ("getSemantics".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSemantics();
            }
        }
        if ("getSession".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSession();
            }
        }
        if ("getSideGap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSideGap();
            }
        }
        if ("getStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStyle();
            }
        }
        if ("getTabIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTabIndex();
            }
        }
        if ("getTensileLength".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTensileLength();
            }
        }
        if ("getTextSelectionSupport".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTextSelectionSupport();
            }
        }
        if ("getTooltip".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTooltip();
            }
        }
        if ("getTopLevelContainer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTopLevelContainer();
            }
        }
        if ("getUIID".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUIID();
            }
        }
        if ("getUIManager".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUIManager();
            }
        }
        if ("getUnselectedStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUnselectedStyle();
            }
        }
        if ("getVisibleBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                return typedTarget.getVisibleBounds((com.codename1.ui.geom.Rectangle) adaptedArgs[0]);
            }
        }
        if ("getWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidth();
            }
        }
        if ("getX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getX();
            }
        }
        if ("getY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getY();
            }
        }
        if ("growShrink".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.growShrink(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("handlesInput".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.handlesInput();
            }
        }
        if ("hasFixedPreferredSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasFixedPreferredSize();
            }
        }
        if ("hasFocus".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasFocus();
            }
        }
        if ("invalidate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.invalidate(); return null;
            }
        }
        if ("isAlwaysTensile".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAlwaysTensile();
            }
        }
        if ("isBlockLead".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBlockLead();
            }
        }
        if ("isCellRenderer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCellRenderer();
            }
        }
        if ("isChildOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.isChildOf((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("isDraggable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDraggable();
            }
        }
        if ("isDropTarget".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDropTarget();
            }
        }
        if ("isEditable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEditable();
            }
        }
        if ("isEditing".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEditing();
            }
        }
        if ("isEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEnabled();
            }
        }
        if ("isFlatten".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFlatten();
            }
        }
        if ("isFocusable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFocusable();
            }
        }
        if ("isGrabsPointerEvents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isGrabsPointerEvents();
            }
        }
        if ("isHScrollThumbGrabbed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHScrollThumbGrabbed();
            }
        }
        if ("isHScrollThumbHover".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHScrollThumbHover();
            }
        }
        if ("isHidden".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHidden();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.isHidden(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("isHideInLandscape".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHideInLandscape();
            }
        }
        if ("isHideInPortrait".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHideInPortrait();
            }
        }
        if ("isIgnorePointerEvents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isIgnorePointerEvents();
            }
        }
        if ("isOpaque".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isOpaque();
            }
        }
        if ("isOwnedBy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.isOwnedBy((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("isPinchBlocksDragAndDrop".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPinchBlocksDragAndDrop();
            }
        }
        if ("isRTL".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRTL();
            }
        }
        if ("isRippleEffect".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRippleEffect();
            }
        }
        if ("isRunning".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRunning();
            }
        }
        if ("isSafeArea".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSafeArea();
            }
        }
        if ("isSafeAreaRoot".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSafeAreaRoot();
            }
        }
        if ("isScrollVisible".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollVisible();
            }
        }
        if ("isScrollableX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollableX();
            }
        }
        if ("isScrollableY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollableY();
            }
        }
        if ("isSmoothScrolling".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSmoothScrolling();
            }
        }
        if ("isSnapToGrid".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSnapToGrid();
            }
        }
        if ("isSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSupported();
            }
        }
        if ("isSurface".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSurface();
            }
        }
        if ("isTactileTouch".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTactileTouch();
            }
        }
        if ("isTensileDragEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTensileDragEnabled();
            }
        }
        if ("isTraversable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTraversable();
            }
        }
        if ("isVScrollThumbGrabbed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVScrollThumbGrabbed();
            }
        }
        if ("isVScrollThumbHover".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVScrollThumbHover();
            }
        }
        if ("isVisible".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVisible();
            }
        }
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.iterator(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("keyPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.keyPressed(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("keyReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.keyReleased(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("keyRepeated".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.keyRepeated(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("layoutContainer".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.layoutContainer(); return null;
            }
        }
        if ("longPointerPress".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.longPointerPress(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("morph".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Runnable.class}, false);
                typedTarget.morph((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], toIntValue(adaptedArgs[2]), (java.lang.Runnable) adaptedArgs[3]); return null;
            }
        }
        if ("morphAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class}, false);
                typedTarget.morphAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], toIntValue(adaptedArgs[2])); return null;
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintBackgrounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintBackgrounds((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintComponent((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Boolean.class}, false);
                typedTarget.paintComponent((com.codename1.ui.Graphics) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("paintComponentBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintComponentBackground((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintIntersectingComponentsAbove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintIntersectingComponentsAbove((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintLock".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.paintLock(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("paintLockRelease".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.paintLockRelease(); return null;
            }
        }
        if ("paintRippleOverlay".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.paintRippleOverlay((com.codename1.ui.Graphics) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3])); return null;
            }
        }
        if ("paintShadows".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.paintShadows((com.codename1.ui.Graphics) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2])); return null;
            }
        }
        if ("pointerDragged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.pointerDragged(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerDragged((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerHover".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerHover((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerHoverPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerHoverPressed((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerHoverReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerHoverReleased((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.pointerPressed(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerPressed((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.pointerReleased(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerReleased((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("putClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.putClientProperty((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("refreshTheme".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.refreshTheme(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.refreshTheme(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("remove".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.remove(); return null;
            }
        }
        if ("removeAll".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.removeAll(); return null;
            }
        }
        if ("removeComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.removeComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("removeContextMenuListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeContextMenuListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeDragFinishedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeDragFinishedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeDragOverListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeDragOverListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeDropListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeDropListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeFocusListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false);
                typedTarget.removeFocusListener((com.codename1.ui.events.FocusListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeLongPressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeLongPressListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeMouseWheelListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeMouseWheelListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePointerDraggedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removePointerDraggedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePointerPressedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removePointerPressedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePointerReleasedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removePointerReleasedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeScrollListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false);
                typedTarget.removeScrollListener((com.codename1.ui.events.ScrollListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeStateChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeStateChangeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeStylusListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeStylusListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("repaint".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.repaint(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.repaint(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3])); return null;
            }
        }
        if ("replace".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false);
                typedTarget.replace((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Runnable.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Runnable.class, java.lang.Integer.class}, false);
                typedTarget.replace((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2], (java.lang.Runnable) adaptedArgs[3], toIntValue(adaptedArgs[4])); return null;
            }
        }
        if ("replaceAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false);
                typedTarget.replaceAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Integer.class}, false);
                typedTarget.replaceAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2], toIntValue(adaptedArgs[3])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Boolean.class}, false);
                typedTarget.replaceAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue()); return null;
            }
        }
        if ("requestFocus".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.requestFocus(); return null;
            }
        }
        if ("respondsToPointerEvents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.respondsToPointerEvents();
            }
        }
        if ("revalidate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.revalidate(); return null;
            }
        }
        if ("revalidateLater".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.revalidateLater(); return null;
            }
        }
        if ("revalidateWithAnimationSafety".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.revalidateWithAnimationSafety(); return null;
            }
        }
        if ("scrollComponentToVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.scrollComponentToVisible((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("scrollRectToVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.Component.class}, false);
                typedTarget.scrollRectToVisible(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), (com.codename1.ui.Component) adaptedArgs[4]); return null;
            }
        }
        if ("setAccessibilityText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setAccessibilityText((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setAlwaysTensile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setAlwaysTensile(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setBlockLead".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setBlockLead(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setBoundPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.setBoundPropertyValue((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("setCellRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setCellRenderer(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setCloudBoundProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setCloudBoundProperty((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setCloudDestinationProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setCloudDestinationProperty((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setComponentState".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setComponentState((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("setCursor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setCursor(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setDirtyRegion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                typedTarget.setDirtyRegion((com.codename1.ui.geom.Rectangle) adaptedArgs[0]); return null;
            }
        }
        if ("setDisabledStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setDisabledStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setDragTransparency".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class}, false);
                typedTarget.setDragTransparency((byte) toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setDraggable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDraggable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDropTarget".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDropTarget(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setEditingDelegate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Editable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Editable.class}, false);
                typedTarget.setEditingDelegate((com.codename1.ui.Editable) adaptedArgs[0]); return null;
            }
        }
        if ("setEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFacing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.camera.CameraFacing.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.camera.CameraFacing.class}, false);
                typedTarget.setFacing((com.codename1.camera.CameraFacing) adaptedArgs[0]); return null;
            }
        }
        if ("setFlatten".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFlatten(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFocus".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFocus(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFocusable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFocusable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setGrabsPointerEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setGrabsPointerEvents(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHandlesInput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHandlesInput(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setHeight(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setHidden".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHidden(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false);
                typedTarget.setHidden(((Boolean) adaptedArgs[0]).booleanValue(), ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setHideInLandscape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHideInLandscape(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHideInPortrait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHideInPortrait(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHorizontalScrollBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.setHorizontalScrollBounds(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]), toIntValue(adaptedArgs[5]), toIntValue(adaptedArgs[6]), toIntValue(adaptedArgs[7])); return null;
            }
        }
        if ("setIgnorePointerEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setIgnorePointerEvents(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setInlineAllStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineAllStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineDisabledStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineDisabledStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlinePressedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlinePressedStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineSelectedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineSelectedStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineStylesTheme".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class}, false);
                typedTarget.setInlineStylesTheme((com.codename1.ui.util.Resources) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineUnselectedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineUnselectedStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setIsScrollVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setIsScrollVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setLabelForComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Label.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Label.class}, false);
                typedTarget.setLabelForComponent((com.codename1.ui.Label) adaptedArgs[0]); return null;
            }
        }
        if ("setLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.Layout.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.Layout.class}, false);
                typedTarget.setLayout((com.codename1.ui.layouts.Layout) adaptedArgs[0]); return null;
            }
        }
        if ("setLeadComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setLeadComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ai.vision.VisionPipelineListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ai.vision.VisionPipelineListener.class}, false);
                typedTarget.setListener((com.codename1.ai.vision.VisionPipelineListener) adaptedArgs[0]); return null;
            }
        }
        if ("setMaxFps".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setMaxFps(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setName((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusDown".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusDown((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusLeft".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusLeft((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusRight((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusUp".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusUp((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setOpaque".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setOpaque(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setOwner".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setOwner((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setPinchBlocksDragAndDrop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setPinchBlocksDragAndDrop(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPreferredH".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredH(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.setPreferredSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("setPreferredSizeStr".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setPreferredSizeStr((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setPreferredTabIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredTabIndex(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setPreferredW".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredW(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setPressedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setPressedStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                return typedTarget.setPropertyValue((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("setPullToRefresh".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                typedTarget.setPullToRefresh((java.lang.Runnable) adaptedArgs[0]); return null;
            }
        }
        if ("setRTL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setRTL(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setRippleEffect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setRippleEffect(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSafeArea".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSafeArea(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSafeAreaRoot".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSafeAreaRoot(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScaleType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.camera.ScaleType.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.camera.ScaleType.class}, false);
                typedTarget.setScaleType((com.codename1.camera.ScaleType) adaptedArgs[0]); return null;
            }
        }
        if ("setScrollAnimationSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setScrollAnimationSpeed(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setScrollIncrement".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setScrollIncrement(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setScrollOpacityChangeSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setScrollOpacityChangeSpeed(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setScrollSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.setScrollSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("setScrollVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollableX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollableX(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollableY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollableY(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSelectCommandText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setSelectCommandText((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setSelectedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setSelectedStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setShouldCalcPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShouldCalcPreferredSize(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.setSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("setSmoothScrolling".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSmoothScrolling(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSnapToGrid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSnapToGrid(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTabIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTabIndex(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setTactileTouch".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTactileTouch(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTensileDragEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTensileDragEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTensileLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTensileLength(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setTooltip".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setTooltip((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setTorchEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTorchEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTraversable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTraversable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setUIID".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setUIID((java.lang.String) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.setUIID((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("setUIManager".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.UIManager.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.UIManager.class}, false);
                typedTarget.setUIManager((com.codename1.ui.plaf.UIManager) adaptedArgs[0]); return null;
            }
        }
        if ("setUnselectedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setUnselectedStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setVerticalScrollBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.setVerticalScrollBounds(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]), toIntValue(adaptedArgs[5]), toIntValue(adaptedArgs[6]), toIntValue(adaptedArgs[7])); return null;
            }
        }
        if ("setVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setWidth(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setX(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setY(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("start".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.start(); return null;
            }
        }
        if ("startEditingAsync".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.startEditingAsync(); return null;
            }
        }
        if ("stop".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.stop(); return null;
            }
        }
        if ("stopEditing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                typedTarget.stopEditing((java.lang.Runnable) adaptedArgs[0]); return null;
            }
        }
        if ("stripMarginAndPadding".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.stripMarginAndPadding();
            }
        }
        if ("styleChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.plaf.Style.class}, false);
                typedTarget.styleChanged((java.lang.String) adaptedArgs[0], (com.codename1.ui.plaf.Style) adaptedArgs[1]); return null;
            }
        }
        if ("toImage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toImage();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("unbindProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false);
                typedTarget.unbindProperty((java.lang.String) adaptedArgs[0], (com.codename1.cloud.BindTarget) adaptedArgs[1]); return null;
            }
        }
        if ("updateTabIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.updateTabIndices(toIntValue(adaptedArgs[0]));
            }
        }
        if ("visibleBoundsContains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.visibleBoundsContains(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke12(com.codename1.ai.vision.VisionException typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCode();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke13(com.codename1.ai.vision.VisionImage typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getEncodedBytes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEncodedBytes();
            }
        }
        if ("getEncodedBytesUnsafe".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEncodedBytesUnsafe();
            }
        }
        if ("getFormat".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFormat();
            }
        }
        if ("getHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeight();
            }
        }
        if ("getPixels".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPixels();
            }
        }
        if ("getPixelsUnsafe".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPixelsUnsafe();
            }
        }
        if ("getRotationDegrees".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRotationDegrees();
            }
        }
        if ("getTimestampNanos".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTimestampNanos();
            }
        }
        if ("getWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidth();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke14(com.codename1.ai.vision.VisionMetadata typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.get((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getBackendId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBackendId();
            }
        }
        if ("getValues".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getValues();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke15(com.codename1.ai.vision.VisionOptions typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("backend".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ai.vision.VisionBackend.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ai.vision.VisionBackend.class}, false);
                return typedTarget.backend((com.codename1.ai.vision.VisionBackend) adaptedArgs[0]);
            }
        }
        if ("getBackend".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBackend();
            }
        }
        if ("getMaximumResults".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaximumResults();
            }
        }
        if ("getMinimumConfidence".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMinimumConfidence();
            }
        }
        if ("getTextScript".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTextScript();
            }
        }
        if ("maximumResults".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.maximumResults(toIntValue(adaptedArgs[0]));
            }
        }
        if ("minimumConfidence".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.minimumConfidence(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("textScript".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ai.vision.TextScript.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ai.vision.TextScript.class}, false);
                return typedTarget.textScript((com.codename1.ai.vision.TextScript) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke16(com.codename1.ai.vision.VisionPipeline typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("close".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.close(); return null;
            }
        }
        if ("isBusy".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBusy();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke17(com.codename1.ai.vision.VisionPoint typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getX();
            }
        }
        if ("getY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getY();
            }
        }
        if ("toPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.toPoint((com.codename1.ui.Component) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.toPoint(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke18(com.codename1.ai.vision.VisionRect typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeight();
            }
        }
        if ("getWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidth();
            }
        }
        if ("getX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getX();
            }
        }
        if ("getY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getY();
            }
        }
        if ("isEmpty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEmpty();
            }
        }
        if ("toBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.toBounds((com.codename1.ui.Component) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.toBounds(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke19(com.codename1.ai.vision.VisionAnalyzer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("close".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.close(); return null;
            }
        }
        if ("isSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSupported();
            }
        }
        if ("process".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ai.vision.VisionImage.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ai.vision.VisionImage.class}, false);
                return typedTarget.process((com.codename1.ai.vision.VisionImage) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke20(com.codename1.ai.vision.VisionBackend typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke21(com.codename1.ai.vision.VisionPipelineListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("error".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false);
                typedTarget.error((java.lang.Throwable) adaptedArgs[0]); return null;
            }
        }
        if ("result".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ai.vision.VisionImage.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ai.vision.VisionImage.class}, false);
                typedTarget.result((java.lang.Object) adaptedArgs[0], (com.codename1.ai.vision.VisionImage) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.ai.vision.BarcodeFormat.class) return getStaticField0(name);
        if (type == com.codename1.ai.vision.FaceLandmarks.class) return getStaticField1(name);
        if (type == com.codename1.ai.vision.PoseLandmarks.class) return getStaticField2(name);
        if (type == com.codename1.ai.vision.TextRecognitionResult.class) return getStaticField3(name);
        if (type == com.codename1.ai.vision.VisionCameraView.class) return getStaticField4(name);
        if (type == com.codename1.ai.vision.VisionException.class) return getStaticField5(name);
        if (type == com.codename1.ai.vision.VisionFeature.class) return getStaticField6(name);
        if (type == com.codename1.ai.vision.VisionRect.class) return getStaticField7(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("AZTEC".equals(name)) return com.codename1.ai.vision.BarcodeFormat.AZTEC;
        if ("CODABAR".equals(name)) return com.codename1.ai.vision.BarcodeFormat.CODABAR;
        if ("CODE_128".equals(name)) return com.codename1.ai.vision.BarcodeFormat.CODE_128;
        if ("CODE_39".equals(name)) return com.codename1.ai.vision.BarcodeFormat.CODE_39;
        if ("CODE_93".equals(name)) return com.codename1.ai.vision.BarcodeFormat.CODE_93;
        if ("DATA_MATRIX".equals(name)) return com.codename1.ai.vision.BarcodeFormat.DATA_MATRIX;
        if ("EAN_13".equals(name)) return com.codename1.ai.vision.BarcodeFormat.EAN_13;
        if ("EAN_8".equals(name)) return com.codename1.ai.vision.BarcodeFormat.EAN_8;
        if ("ITF".equals(name)) return com.codename1.ai.vision.BarcodeFormat.ITF;
        if ("PDF417".equals(name)) return com.codename1.ai.vision.BarcodeFormat.PDF417;
        if ("QR_CODE".equals(name)) return com.codename1.ai.vision.BarcodeFormat.QR_CODE;
        if ("UNKNOWN".equals(name)) return com.codename1.ai.vision.BarcodeFormat.UNKNOWN;
        if ("UPC_A".equals(name)) return com.codename1.ai.vision.BarcodeFormat.UPC_A;
        if ("UPC_E".equals(name)) return com.codename1.ai.vision.BarcodeFormat.UPC_E;
        throw unsupportedStaticField(com.codename1.ai.vision.BarcodeFormat.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("LEFT_EYE".equals(name)) return com.codename1.ai.vision.FaceLandmarks.LEFT_EYE;
        if ("MOUTH_LEFT".equals(name)) return com.codename1.ai.vision.FaceLandmarks.MOUTH_LEFT;
        if ("MOUTH_RIGHT".equals(name)) return com.codename1.ai.vision.FaceLandmarks.MOUTH_RIGHT;
        if ("NOSE_BASE".equals(name)) return com.codename1.ai.vision.FaceLandmarks.NOSE_BASE;
        if ("RIGHT_EYE".equals(name)) return com.codename1.ai.vision.FaceLandmarks.RIGHT_EYE;
        throw unsupportedStaticField(com.codename1.ai.vision.FaceLandmarks.class, name);
    }

    private static Object getStaticField2(String name) throws Exception {
        if ("LEFT_ANKLE".equals(name)) return com.codename1.ai.vision.PoseLandmarks.LEFT_ANKLE;
        if ("LEFT_EAR".equals(name)) return com.codename1.ai.vision.PoseLandmarks.LEFT_EAR;
        if ("LEFT_ELBOW".equals(name)) return com.codename1.ai.vision.PoseLandmarks.LEFT_ELBOW;
        if ("LEFT_EYE".equals(name)) return com.codename1.ai.vision.PoseLandmarks.LEFT_EYE;
        if ("LEFT_EYE_INNER".equals(name)) return com.codename1.ai.vision.PoseLandmarks.LEFT_EYE_INNER;
        if ("LEFT_EYE_OUTER".equals(name)) return com.codename1.ai.vision.PoseLandmarks.LEFT_EYE_OUTER;
        if ("LEFT_FOOT_INDEX".equals(name)) return com.codename1.ai.vision.PoseLandmarks.LEFT_FOOT_INDEX;
        if ("LEFT_HEEL".equals(name)) return com.codename1.ai.vision.PoseLandmarks.LEFT_HEEL;
        if ("LEFT_HIP".equals(name)) return com.codename1.ai.vision.PoseLandmarks.LEFT_HIP;
        if ("LEFT_INDEX".equals(name)) return com.codename1.ai.vision.PoseLandmarks.LEFT_INDEX;
        if ("LEFT_KNEE".equals(name)) return com.codename1.ai.vision.PoseLandmarks.LEFT_KNEE;
        if ("LEFT_MOUTH".equals(name)) return com.codename1.ai.vision.PoseLandmarks.LEFT_MOUTH;
        if ("LEFT_PINKY".equals(name)) return com.codename1.ai.vision.PoseLandmarks.LEFT_PINKY;
        if ("LEFT_SHOULDER".equals(name)) return com.codename1.ai.vision.PoseLandmarks.LEFT_SHOULDER;
        if ("LEFT_THUMB".equals(name)) return com.codename1.ai.vision.PoseLandmarks.LEFT_THUMB;
        if ("LEFT_WRIST".equals(name)) return com.codename1.ai.vision.PoseLandmarks.LEFT_WRIST;
        if ("NECK".equals(name)) return com.codename1.ai.vision.PoseLandmarks.NECK;
        if ("NOSE".equals(name)) return com.codename1.ai.vision.PoseLandmarks.NOSE;
        if ("RIGHT_ANKLE".equals(name)) return com.codename1.ai.vision.PoseLandmarks.RIGHT_ANKLE;
        if ("RIGHT_EAR".equals(name)) return com.codename1.ai.vision.PoseLandmarks.RIGHT_EAR;
        if ("RIGHT_ELBOW".equals(name)) return com.codename1.ai.vision.PoseLandmarks.RIGHT_ELBOW;
        if ("RIGHT_EYE".equals(name)) return com.codename1.ai.vision.PoseLandmarks.RIGHT_EYE;
        if ("RIGHT_EYE_INNER".equals(name)) return com.codename1.ai.vision.PoseLandmarks.RIGHT_EYE_INNER;
        if ("RIGHT_EYE_OUTER".equals(name)) return com.codename1.ai.vision.PoseLandmarks.RIGHT_EYE_OUTER;
        if ("RIGHT_FOOT_INDEX".equals(name)) return com.codename1.ai.vision.PoseLandmarks.RIGHT_FOOT_INDEX;
        if ("RIGHT_HEEL".equals(name)) return com.codename1.ai.vision.PoseLandmarks.RIGHT_HEEL;
        if ("RIGHT_HIP".equals(name)) return com.codename1.ai.vision.PoseLandmarks.RIGHT_HIP;
        if ("RIGHT_INDEX".equals(name)) return com.codename1.ai.vision.PoseLandmarks.RIGHT_INDEX;
        if ("RIGHT_KNEE".equals(name)) return com.codename1.ai.vision.PoseLandmarks.RIGHT_KNEE;
        if ("RIGHT_MOUTH".equals(name)) return com.codename1.ai.vision.PoseLandmarks.RIGHT_MOUTH;
        if ("RIGHT_PINKY".equals(name)) return com.codename1.ai.vision.PoseLandmarks.RIGHT_PINKY;
        if ("RIGHT_SHOULDER".equals(name)) return com.codename1.ai.vision.PoseLandmarks.RIGHT_SHOULDER;
        if ("RIGHT_THUMB".equals(name)) return com.codename1.ai.vision.PoseLandmarks.RIGHT_THUMB;
        if ("RIGHT_WRIST".equals(name)) return com.codename1.ai.vision.PoseLandmarks.RIGHT_WRIST;
        if ("ROOT".equals(name)) return com.codename1.ai.vision.PoseLandmarks.ROOT;
        if ("UNKNOWN".equals(name)) return com.codename1.ai.vision.PoseLandmarks.UNKNOWN;
        throw unsupportedStaticField(com.codename1.ai.vision.PoseLandmarks.class, name);
    }

    private static Object getStaticField3(String name) throws Exception {
        if ("EMPTY".equals(name)) return com.codename1.ai.vision.TextRecognitionResult.EMPTY;
        throw unsupportedStaticField(com.codename1.ai.vision.TextRecognitionResult.class, name);
    }

    private static Object getStaticField4(String name) throws Exception {
        if ("BASELINE".equals(name)) return com.codename1.ai.vision.VisionCameraView.BASELINE;
        if ("BOTTOM".equals(name)) return com.codename1.ai.vision.VisionCameraView.BOTTOM;
        if ("BRB_CENTER_OFFSET".equals(name)) return com.codename1.ai.vision.VisionCameraView.BRB_CENTER_OFFSET;
        if ("BRB_CONSTANT_ASCENT".equals(name)) return com.codename1.ai.vision.VisionCameraView.BRB_CONSTANT_ASCENT;
        if ("BRB_CONSTANT_DESCENT".equals(name)) return com.codename1.ai.vision.VisionCameraView.BRB_CONSTANT_DESCENT;
        if ("BRB_OTHER".equals(name)) return com.codename1.ai.vision.VisionCameraView.BRB_OTHER;
        if ("CENTER".equals(name)) return com.codename1.ai.vision.VisionCameraView.CENTER;
        if ("CROSSHAIR_CURSOR".equals(name)) return com.codename1.ai.vision.VisionCameraView.CROSSHAIR_CURSOR;
        if ("DEFAULT_CURSOR".equals(name)) return com.codename1.ai.vision.VisionCameraView.DEFAULT_CURSOR;
        if ("DRAG_REGION_IMMEDIATELY_DRAG_X".equals(name)) return com.codename1.ai.vision.VisionCameraView.DRAG_REGION_IMMEDIATELY_DRAG_X;
        if ("DRAG_REGION_IMMEDIATELY_DRAG_XY".equals(name)) return com.codename1.ai.vision.VisionCameraView.DRAG_REGION_IMMEDIATELY_DRAG_XY;
        if ("DRAG_REGION_IMMEDIATELY_DRAG_Y".equals(name)) return com.codename1.ai.vision.VisionCameraView.DRAG_REGION_IMMEDIATELY_DRAG_Y;
        if ("DRAG_REGION_LIKELY_DRAG_X".equals(name)) return com.codename1.ai.vision.VisionCameraView.DRAG_REGION_LIKELY_DRAG_X;
        if ("DRAG_REGION_LIKELY_DRAG_XY".equals(name)) return com.codename1.ai.vision.VisionCameraView.DRAG_REGION_LIKELY_DRAG_XY;
        if ("DRAG_REGION_LIKELY_DRAG_Y".equals(name)) return com.codename1.ai.vision.VisionCameraView.DRAG_REGION_LIKELY_DRAG_Y;
        if ("DRAG_REGION_NOT_DRAGGABLE".equals(name)) return com.codename1.ai.vision.VisionCameraView.DRAG_REGION_NOT_DRAGGABLE;
        if ("DRAG_REGION_POSSIBLE_DRAG_X".equals(name)) return com.codename1.ai.vision.VisionCameraView.DRAG_REGION_POSSIBLE_DRAG_X;
        if ("DRAG_REGION_POSSIBLE_DRAG_XY".equals(name)) return com.codename1.ai.vision.VisionCameraView.DRAG_REGION_POSSIBLE_DRAG_XY;
        if ("DRAG_REGION_POSSIBLE_DRAG_Y".equals(name)) return com.codename1.ai.vision.VisionCameraView.DRAG_REGION_POSSIBLE_DRAG_Y;
        if ("E_RESIZE_CURSOR".equals(name)) return com.codename1.ai.vision.VisionCameraView.E_RESIZE_CURSOR;
        if ("HAND_CURSOR".equals(name)) return com.codename1.ai.vision.VisionCameraView.HAND_CURSOR;
        if ("LEFT".equals(name)) return com.codename1.ai.vision.VisionCameraView.LEFT;
        if ("MOVE_CURSOR".equals(name)) return com.codename1.ai.vision.VisionCameraView.MOVE_CURSOR;
        if ("NE_RESIZE_CURSOR".equals(name)) return com.codename1.ai.vision.VisionCameraView.NE_RESIZE_CURSOR;
        if ("NW_RESIZE_CURSOR".equals(name)) return com.codename1.ai.vision.VisionCameraView.NW_RESIZE_CURSOR;
        if ("N_RESIZE_CURSOR".equals(name)) return com.codename1.ai.vision.VisionCameraView.N_RESIZE_CURSOR;
        if ("RIGHT".equals(name)) return com.codename1.ai.vision.VisionCameraView.RIGHT;
        if ("SE_RESIZE_CURSOR".equals(name)) return com.codename1.ai.vision.VisionCameraView.SE_RESIZE_CURSOR;
        if ("SW_RESIZE_CURSOR".equals(name)) return com.codename1.ai.vision.VisionCameraView.SW_RESIZE_CURSOR;
        if ("S_RESIZE_CURSOR".equals(name)) return com.codename1.ai.vision.VisionCameraView.S_RESIZE_CURSOR;
        if ("TEXT_CURSOR".equals(name)) return com.codename1.ai.vision.VisionCameraView.TEXT_CURSOR;
        if ("TOP".equals(name)) return com.codename1.ai.vision.VisionCameraView.TOP;
        if ("WAIT_CURSOR".equals(name)) return com.codename1.ai.vision.VisionCameraView.WAIT_CURSOR;
        if ("W_RESIZE_CURSOR".equals(name)) return com.codename1.ai.vision.VisionCameraView.W_RESIZE_CURSOR;
        throw unsupportedStaticField(com.codename1.ai.vision.VisionCameraView.class, name);
    }

    private static Object getStaticField5(String name) throws Exception {
        if ("BACKEND_ERROR".equals(name)) return com.codename1.ai.vision.VisionException.BACKEND_ERROR;
        if ("CANCELLED".equals(name)) return com.codename1.ai.vision.VisionException.CANCELLED;
        if ("INVALID_IMAGE".equals(name)) return com.codename1.ai.vision.VisionException.INVALID_IMAGE;
        if ("MODEL_UNAVAILABLE".equals(name)) return com.codename1.ai.vision.VisionException.MODEL_UNAVAILABLE;
        if ("UNSUPPORTED".equals(name)) return com.codename1.ai.vision.VisionException.UNSUPPORTED;
        throw unsupportedStaticField(com.codename1.ai.vision.VisionException.class, name);
    }

    private static Object getStaticField6(String name) throws Exception {
        if ("BARCODE_SCANNING".equals(name)) return com.codename1.ai.vision.VisionFeature.BARCODE_SCANNING;
        if ("DOCUMENT_SCANNING".equals(name)) return com.codename1.ai.vision.VisionFeature.DOCUMENT_SCANNING;
        if ("FACE_DETECTION".equals(name)) return com.codename1.ai.vision.VisionFeature.FACE_DETECTION;
        if ("IMAGE_LABELING".equals(name)) return com.codename1.ai.vision.VisionFeature.IMAGE_LABELING;
        if ("POSE_DETECTION".equals(name)) return com.codename1.ai.vision.VisionFeature.POSE_DETECTION;
        if ("SELFIE_SEGMENTATION".equals(name)) return com.codename1.ai.vision.VisionFeature.SELFIE_SEGMENTATION;
        if ("TEXT_RECOGNITION".equals(name)) return com.codename1.ai.vision.VisionFeature.TEXT_RECOGNITION;
        throw unsupportedStaticField(com.codename1.ai.vision.VisionFeature.class, name);
    }

    private static Object getStaticField7(String name) throws Exception {
        if ("EMPTY".equals(name)) return com.codename1.ai.vision.VisionRect.EMPTY;
        throw unsupportedStaticField(com.codename1.ai.vision.VisionRect.class, name);
    }

    public static Object getField(Object target, String name) throws Exception {
        throw unsupportedField(target, name);
    }

    public static void setStaticField(Class<?> type, String name, Object value) throws Exception {
        throw unsupportedStaticFieldWrite(type, name, value);
    }

    public static void setField(Object target, String name, Object value) throws Exception {
        throw unsupportedFieldWrite(target, name, value);
    }

    private static Object[] safeArgs(Object[] args) {
        return args == null ? new Object[0] : args;
    }

    private static Object[] adaptArgs(Object[] args, Class<?>[] paramTypes, boolean varArgs) {
        if (args == null || args.length == 0) {
            return args == null ? new Object[0] : args;
        }
        Object[] adapted = args.clone();
        if (!varArgs) {
            for (int i = 0; i < Math.min(adapted.length, paramTypes.length); i++) {
                adapted[i] = adaptValue(adapted[i], paramTypes[i]);
            }
            return adapted;
        }
        if (paramTypes.length == 0) {
            return adapted;
        }
        int fixedCount = paramTypes.length - 1;
        for (int i = 0; i < Math.min(fixedCount, adapted.length); i++) {
            adapted[i] = adaptValue(adapted[i], paramTypes[i]);
        }
        Class<?> componentType = paramTypes[paramTypes.length - 1].getComponentType();
        for (int i = fixedCount; i < adapted.length; i++) {
            adapted[i] = adaptValue(adapted[i], componentType);
        }
        return adapted;
    }

    private static boolean isSamInterface(Class<?> type) {
        if (type == com.codename1.util.OnComplete.class) {
            return true;
        }
        if (type == com.codename1.util.SuccessCallback.class) {
            return true;
        }
        if (type == com.codename1.util.FailureCallback.class) {
            return true;
        }
        if (type == com.codename1.ui.events.ActionListener.class) {
            return true;
        }
        if (type == java.lang.Runnable.class) {
            return true;
        }
        if (type == com.codename1.ui.events.DataChangedListener.class) {
            return true;
        }
        if (type == com.codename1.ui.events.SelectionListener.class) {
            return true;
        }
        if (type == com.codename1.printing.PrintResultListener.class) {
            return true;
        }
        return false;
    }

    private static Object adaptLambdaValue(final bsh.cn1.CN1LambdaSupport.LambdaValue lambda, Class<?> type) {
        if (type == com.codename1.util.OnComplete.class) {
            return new com.codename1.util.OnComplete() {
                public void completed(java.lang.Object arg0) {
                    try {
                        lambda.invoke(new Object[]{arg0});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.util.SuccessCallback.class) {
            return new com.codename1.util.SuccessCallback() {
                public void onSucess(java.lang.Object arg0) {
                    try {
                        lambda.invoke(new Object[]{arg0});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.util.FailureCallback.class) {
            return new com.codename1.util.FailureCallback() {
                public void onError(java.lang.Object arg0, java.lang.Throwable arg1, int arg2, java.lang.String arg3) {
                    try {
                        lambda.invoke(new Object[]{arg0, arg1, arg2, arg3});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.ui.events.ActionListener.class) {
            return new com.codename1.ui.events.ActionListener() {
                public void actionPerformed(com.codename1.ui.events.ActionEvent arg0) {
                    try {
                        lambda.invoke(new Object[]{arg0});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == java.lang.Runnable.class) {
            return new java.lang.Runnable() {
                public void run() {
                    try {
                        lambda.invoke(new Object[0]);
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.ui.events.DataChangedListener.class) {
            return new com.codename1.ui.events.DataChangedListener() {
                public void dataChanged(int arg0, int arg1) {
                    try {
                        lambda.invoke(new Object[]{arg0, arg1});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.ui.events.SelectionListener.class) {
            return new com.codename1.ui.events.SelectionListener() {
                public void selectionChanged(int arg0, int arg1) {
                    try {
                        lambda.invoke(new Object[]{arg0, arg1});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.printing.PrintResultListener.class) {
            return new com.codename1.printing.PrintResultListener() {
                public void onResult(com.codename1.printing.PrintResult arg0) {
                    try {
                        lambda.invoke(new Object[]{arg0});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        return lambda;
    }

    private static Object adaptValue(Object value, Class<?> type) {
        if (!(value instanceof bsh.cn1.CN1LambdaSupport.LambdaValue)) {
            return value;
        }
        // Direct fit when LambdaValue already implements the target SAM
        // (Runnable, Function, Comparator, ...).
        if (type.isInstance(value)) {
            return value;
        }
        return adaptLambdaValue((bsh.cn1.CN1LambdaSupport.LambdaValue) value, type);
    }

    private static int toIntValue(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof Character) return (int) ((Character) value).charValue();
        throw new ClassCastException("Cannot coerce "
            + (value == null ? "null" : value.getClass().getName()) + " to int");
    }

    private static boolean matches(Object[] args, Class<?>[] paramTypes, boolean varArgs) {
        if (!varArgs) {
            if (args.length != paramTypes.length) {
                return false;
            }
            for (int i = 0; i < paramTypes.length; i++) {
                if (!matchesType(args[i], paramTypes[i])) {
                    return false;
                }
            }
            return true;
        }
        if (paramTypes.length == 0) {
            return true;
        }
        int fixedCount = paramTypes.length - 1;
        if (args.length < fixedCount) {
            return false;
        }
        for (int i = 0; i < fixedCount; i++) {
            if (!matchesType(args[i], paramTypes[i])) {
                return false;
            }
        }
        Class<?> componentType = paramTypes[paramTypes.length - 1].getComponentType();
        for (int i = fixedCount; i < args.length; i++) {
            if (!matchesType(args[i], componentType)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesType(Object value, Class<?> type) {
        if (type == Object.class) {
            return true;
        }
        if (value == null) {
            return !type.isPrimitive();
        }
        if (type.isArray()) {
            return type.isInstance(value);
        }
        if ("boolean".equals(type.getName()) || type == Boolean.class) {
            return value instanceof Boolean;
        }
        if ("char".equals(type.getName()) || type == Character.class) {
            return value instanceof Character;
        }
        if ("byte".equals(type.getName()) || type == Byte.class || "short".equals(type.getName()) || type == Short.class
                || "int".equals(type.getName()) || type == Integer.class || "long".equals(type.getName()) || type == Long.class
                || "float".equals(type.getName()) || type == Float.class || "double".equals(type.getName()) || type == Double.class) {
            // Java widens char to int implicitly, so accept Character
            // for any int-or-larger numeric slot.
            return value instanceof Number || value instanceof Character;
        }
        if (value instanceof bsh.cn1.CN1LambdaSupport.LambdaValue) {
            // LambdaValue implements common SAMs directly (Runnable,
            // Function, Predicate, Comparator, ...). Also accept any
            // CN1 SAM the listener-bridge knows how to wrap.
            return type.isInstance(value) || isSamInterface(type);
        }
        return type.isInstance(value);
    }

    private static CN1AccessException unsupportedConstruct(Class<?> type, Object[] args) {
        return new CN1AccessException("Generated constructor dispatch not implemented for " + type.getName() + describeArgs(args));
    }

    private static CN1AccessException unsupportedStatic(Class<?> type, String name, Object[] args) {
        return new CN1AccessException("Generated static dispatch not implemented for " + type.getName() + "." + name + describeArgs(args));
    }

    private static CN1AccessException unsupportedInstance(Object target, String name, Object[] args) {
        return new CN1AccessException("Generated instance dispatch not implemented for " + target.getClass().getName() + "." + name + describeArgs(args));
    }

    private static CN1AccessException unsupportedStaticField(Class<?> type, String name) {
        return new CN1AccessException("Generated static field access not implemented for " + type.getName() + "." + name);
    }

    private static CN1AccessException unsupportedField(Object target, String name) {
        return new CN1AccessException("Generated field access not implemented for " + target.getClass().getName() + "." + name);
    }

    private static CN1AccessException unsupportedStaticFieldWrite(Class<?> type, String name, Object value) {
        return new CN1AccessException("Generated static field write not implemented for " + type.getName() + "." + name + " value=" + describeValue(value));
    }

    private static CN1AccessException unsupportedFieldWrite(Object target, String name, Object value) {
        return new CN1AccessException("Generated field write not implemented for " + target.getClass().getName() + "." + name + " value=" + describeValue(value));
    }

    private static String describeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "()";
        }
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(describeValue(args[i]));
        }
        sb.append(')');
        return sb.toString();
    }

    private static String describeValue(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }
}
