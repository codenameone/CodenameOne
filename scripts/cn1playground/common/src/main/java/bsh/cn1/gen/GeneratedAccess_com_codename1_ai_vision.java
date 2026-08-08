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
        if ("BarcodeScanner".equals(simpleName)) {
            return com.codename1.ai.vision.BarcodeScanner.class;
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
        if ("VisionAnalyzer".equals(simpleName)) {
            return com.codename1.ai.vision.VisionAnalyzer.class;
        }
        if ("VisionBackend".equals(simpleName)) {
            return com.codename1.ai.vision.VisionBackend.class;
        }
        if ("VisionBackends".equals(simpleName)) {
            return com.codename1.ai.vision.VisionBackends.class;
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
        if (type == com.codename1.ai.vision.VisionBackends.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.ai.vision.VisionImage.class) return invokeStatic1(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
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

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
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
        if (target instanceof com.codename1.ai.vision.DocumentScanResult) {
            try {
                return invoke1((com.codename1.ai.vision.DocumentScanResult) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.vision.Face) {
            try {
                return invoke2((com.codename1.ai.vision.Face) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.vision.ImageLabel) {
            try {
                return invoke3((com.codename1.ai.vision.ImageLabel) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.vision.Pose) {
            try {
                return invoke4((com.codename1.ai.vision.Pose) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.vision.Pose.Landmark) {
            try {
                return invoke5((com.codename1.ai.vision.Pose.Landmark) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.vision.SegmentationMask) {
            try {
                return invoke6((com.codename1.ai.vision.SegmentationMask) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.vision.TextRecognitionResult) {
            try {
                return invoke7((com.codename1.ai.vision.TextRecognitionResult) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.vision.TextRecognitionResult.TextBlock) {
            try {
                return invoke8((com.codename1.ai.vision.TextRecognitionResult.TextBlock) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.vision.VisionException) {
            try {
                return invoke9((com.codename1.ai.vision.VisionException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.vision.VisionImage) {
            try {
                return invoke10((com.codename1.ai.vision.VisionImage) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.vision.VisionMetadata) {
            try {
                return invoke11((com.codename1.ai.vision.VisionMetadata) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.vision.VisionOptions) {
            try {
                return invoke12((com.codename1.ai.vision.VisionOptions) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.vision.VisionPipeline) {
            try {
                return invoke13((com.codename1.ai.vision.VisionPipeline) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.vision.VisionPoint) {
            try {
                return invoke14((com.codename1.ai.vision.VisionPoint) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.vision.VisionRect) {
            try {
                return invoke15((com.codename1.ai.vision.VisionRect) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.vision.VisionAnalyzer) {
            try {
                return invoke16((com.codename1.ai.vision.VisionAnalyzer) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.vision.VisionBackend) {
            try {
                return invoke17((com.codename1.ai.vision.VisionBackend) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.vision.VisionPipelineListener) {
            try {
                return invoke18((com.codename1.ai.vision.VisionPipelineListener) target, name, safeArgs);
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

    private static Object invoke1(com.codename1.ai.vision.DocumentScanResult typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke2(com.codename1.ai.vision.Face typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getBounds".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBounds();
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

    private static Object invoke3(com.codename1.ai.vision.ImageLabel typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke4(com.codename1.ai.vision.Pose typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke5(com.codename1.ai.vision.Pose.Landmark typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke6(com.codename1.ai.vision.SegmentationMask typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getConfidence".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getConfidence();
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
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.ai.vision.TextRecognitionResult typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke8(com.codename1.ai.vision.TextRecognitionResult.TextBlock typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke9(com.codename1.ai.vision.VisionException typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCode();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.ai.vision.VisionImage typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke11(com.codename1.ai.vision.VisionMetadata typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke12(com.codename1.ai.vision.VisionOptions typedTarget, String name, Object[] safeArgs) throws Exception {
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
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke13(com.codename1.ai.vision.VisionPipeline typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke14(com.codename1.ai.vision.VisionPoint typedTarget, String name, Object[] safeArgs) throws Exception {
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
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke15(com.codename1.ai.vision.VisionRect typedTarget, String name, Object[] safeArgs) throws Exception {
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
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke16(com.codename1.ai.vision.VisionAnalyzer typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke17(com.codename1.ai.vision.VisionBackend typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke18(com.codename1.ai.vision.VisionPipelineListener typedTarget, String name, Object[] safeArgs) throws Exception {
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
        if (type == com.codename1.ai.vision.TextRecognitionResult.class) return getStaticField0(name);
        if (type == com.codename1.ai.vision.VisionException.class) return getStaticField1(name);
        if (type == com.codename1.ai.vision.VisionFeature.class) return getStaticField2(name);
        if (type == com.codename1.ai.vision.VisionRect.class) return getStaticField3(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("EMPTY".equals(name)) return com.codename1.ai.vision.TextRecognitionResult.EMPTY;
        throw unsupportedStaticField(com.codename1.ai.vision.TextRecognitionResult.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("BACKEND_ERROR".equals(name)) return com.codename1.ai.vision.VisionException.BACKEND_ERROR;
        if ("CANCELLED".equals(name)) return com.codename1.ai.vision.VisionException.CANCELLED;
        if ("INVALID_IMAGE".equals(name)) return com.codename1.ai.vision.VisionException.INVALID_IMAGE;
        if ("MODEL_UNAVAILABLE".equals(name)) return com.codename1.ai.vision.VisionException.MODEL_UNAVAILABLE;
        if ("UNSUPPORTED".equals(name)) return com.codename1.ai.vision.VisionException.UNSUPPORTED;
        throw unsupportedStaticField(com.codename1.ai.vision.VisionException.class, name);
    }

    private static Object getStaticField2(String name) throws Exception {
        if ("BARCODE_SCANNING".equals(name)) return com.codename1.ai.vision.VisionFeature.BARCODE_SCANNING;
        if ("DOCUMENT_SCANNING".equals(name)) return com.codename1.ai.vision.VisionFeature.DOCUMENT_SCANNING;
        if ("FACE_DETECTION".equals(name)) return com.codename1.ai.vision.VisionFeature.FACE_DETECTION;
        if ("IMAGE_LABELING".equals(name)) return com.codename1.ai.vision.VisionFeature.IMAGE_LABELING;
        if ("POSE_DETECTION".equals(name)) return com.codename1.ai.vision.VisionFeature.POSE_DETECTION;
        if ("SELFIE_SEGMENTATION".equals(name)) return com.codename1.ai.vision.VisionFeature.SELFIE_SEGMENTATION;
        if ("TEXT_RECOGNITION".equals(name)) return com.codename1.ai.vision.VisionFeature.TEXT_RECOGNITION;
        throw unsupportedStaticField(com.codename1.ai.vision.VisionFeature.class, name);
    }

    private static Object getStaticField3(String name) throws Exception {
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
