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
#import "CodenameOne_GLViewController.h"
#import "xmlvm.h"

#if defined(INCLUDE_CN1_VISION) && !TARGET_OS_WATCH && !TARGET_OS_TV
#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <Vision/Vision.h>
#import <CoreImage/CoreImage.h>
#import <CoreVideo/CoreVideo.h>
#import <ImageIO/ImageIO.h>
#import <math.h>
#import "java_lang_String.h"

#if __has_include(<MLKitVision/MLKitVision.h>)
#import <MLKitVision/MLKitVision.h>
#define CN1_HAS_MLKIT_VISION 1
#endif
#if __has_include(<MLKitTextRecognition/MLKitTextRecognition.h>)
#import <MLKitTextRecognition/MLKitTextRecognition.h>
#import <MLKitTextRecognitionCommon/MLKitTextRecognitionCommon.h>
#define CN1_HAS_MLKIT_TEXT 1
#endif
#if __has_include(<MLKitBarcodeScanning/MLKitBarcodeScanning.h>)
#import <MLKitBarcodeScanning/MLKitBarcodeScanning.h>
#define CN1_HAS_MLKIT_BARCODE 1
#endif
#if __has_include(<MLKitFaceDetection/MLKitFaceDetection.h>)
#import <MLKitFaceDetection/MLKitFaceDetection.h>
#define CN1_HAS_MLKIT_FACE 1
#endif
#if __has_include(<MLKitImageLabeling/MLKitImageLabeling.h>)
#import <MLKitImageLabeling/MLKitImageLabeling.h>
#import <MLKitImageLabelingCommon/MLKitImageLabelingCommon.h>
#define CN1_HAS_MLKIT_LABEL 1
#endif
#if __has_include(<MLKitPoseDetection/MLKitPoseDetection.h>)
#import <MLKitPoseDetection/MLKitPoseDetection.h>
#import <MLKitPoseDetectionCommon/MLKitPoseDetectionCommon.h>
#define CN1_HAS_MLKIT_POSE 1
#endif
#if __has_include(<MLKitSegmentationSelfie/MLKitSegmentationSelfie.h>)
#import <MLKitSegmentationSelfie/MLKitSegmentationSelfie.h>
#import <MLKitSegmentationCommon/MLKitSegmentationCommon.h>
#define CN1_HAS_MLKIT_SEGMENTATION 1
#endif

static NSDictionary *cn1VisionRect(CGRect rect) {
    return @{
        @"x": @(rect.origin.x),
        @"y": @(1.0 - CGRectGetMaxY(rect)),
        @"width": @(rect.size.width),
        @"height": @(rect.size.height)
    };
}

static NSDictionary *cn1MLKitRect(CGRect rect, CGSize size) {
    if (size.width <= 0 || size.height <= 0) {
        return @{@"x": @0, @"y": @0, @"width": @0, @"height": @0};
    }
    return @{
        @"x": @(rect.origin.x / size.width),
        @"y": @(rect.origin.y / size.height),
        @"width": @(rect.size.width / size.width),
        @"height": @(rect.size.height / size.height)
    };
}

#if defined(CN1_HAS_MLKIT_FACE)
static void cn1MLKitAddFaceLandmark(NSMutableDictionary *landmarks,
        NSString *name, MLKFace *face, MLKFaceLandmarkType type,
        CGSize imageSize) {
    MLKFaceLandmark *landmark = [face landmarkOfType:type];
    if (landmark != nil && imageSize.width > 0 && imageSize.height > 0) {
        landmarks[name] = @{
            @"x": @(landmark.position.x / imageSize.width),
            @"y": @(landmark.position.y / imageSize.height)
        };
    }
}
#endif

#if defined(CN1_HAS_MLKIT_POSE)
static NSString *cn1MLKitPoseLandmarkName(MLKPoseLandmarkType type) {
    if ([type isEqualToString:MLKPoseLandmarkTypeNose]) return @"nose";
    if ([type isEqualToString:MLKPoseLandmarkTypeLeftEyeInner]) return @"leftEyeInner";
    if ([type isEqualToString:MLKPoseLandmarkTypeLeftEye]) return @"leftEye";
    if ([type isEqualToString:MLKPoseLandmarkTypeLeftEyeOuter]) return @"leftEyeOuter";
    if ([type isEqualToString:MLKPoseLandmarkTypeRightEyeInner]) return @"rightEyeInner";
    if ([type isEqualToString:MLKPoseLandmarkTypeRightEye]) return @"rightEye";
    if ([type isEqualToString:MLKPoseLandmarkTypeRightEyeOuter]) return @"rightEyeOuter";
    if ([type isEqualToString:MLKPoseLandmarkTypeLeftEar]) return @"leftEar";
    if ([type isEqualToString:MLKPoseLandmarkTypeRightEar]) return @"rightEar";
    if ([type isEqualToString:MLKPoseLandmarkTypeMouthLeft]) return @"leftMouth";
    if ([type isEqualToString:MLKPoseLandmarkTypeMouthRight]) return @"rightMouth";
    if ([type isEqualToString:MLKPoseLandmarkTypeLeftShoulder]) return @"leftShoulder";
    if ([type isEqualToString:MLKPoseLandmarkTypeRightShoulder]) return @"rightShoulder";
    if ([type isEqualToString:MLKPoseLandmarkTypeLeftElbow]) return @"leftElbow";
    if ([type isEqualToString:MLKPoseLandmarkTypeRightElbow]) return @"rightElbow";
    if ([type isEqualToString:MLKPoseLandmarkTypeLeftWrist]) return @"leftWrist";
    if ([type isEqualToString:MLKPoseLandmarkTypeRightWrist]) return @"rightWrist";
    if ([type isEqualToString:MLKPoseLandmarkTypeLeftPinkyFinger]) return @"leftPinky";
    if ([type isEqualToString:MLKPoseLandmarkTypeRightPinkyFinger]) return @"rightPinky";
    if ([type isEqualToString:MLKPoseLandmarkTypeLeftIndexFinger]) return @"leftIndex";
    if ([type isEqualToString:MLKPoseLandmarkTypeRightIndexFinger]) return @"rightIndex";
    if ([type isEqualToString:MLKPoseLandmarkTypeLeftThumb]) return @"leftThumb";
    if ([type isEqualToString:MLKPoseLandmarkTypeRightThumb]) return @"rightThumb";
    if ([type isEqualToString:MLKPoseLandmarkTypeLeftHip]) return @"leftHip";
    if ([type isEqualToString:MLKPoseLandmarkTypeRightHip]) return @"rightHip";
    if ([type isEqualToString:MLKPoseLandmarkTypeLeftKnee]) return @"leftKnee";
    if ([type isEqualToString:MLKPoseLandmarkTypeRightKnee]) return @"rightKnee";
    if ([type isEqualToString:MLKPoseLandmarkTypeLeftAnkle]) return @"leftAnkle";
    if ([type isEqualToString:MLKPoseLandmarkTypeRightAnkle]) return @"rightAnkle";
    if ([type isEqualToString:MLKPoseLandmarkTypeLeftHeel]) return @"leftHeel";
    if ([type isEqualToString:MLKPoseLandmarkTypeRightHeel]) return @"rightHeel";
    if ([type isEqualToString:MLKPoseLandmarkTypeLeftToe]) return @"leftFootIndex";
    if ([type isEqualToString:MLKPoseLandmarkTypeRightToe]) return @"rightFootIndex";
    return @"unknown";
}
#endif

static NSDictionary *cn1VisionFaceLandmark(
        VNFaceLandmarkRegion2D *region, CGRect faceBounds) {
    if (region == nil || region.pointCount == 0
            || region.normalizedPoints == NULL) {
        return nil;
    }
    double x = 0;
    double y = 0;
    for (NSUInteger i = 0; i < region.pointCount; i++) {
        x += region.normalizedPoints[i].x;
        y += region.normalizedPoints[i].y;
    }
    x = faceBounds.origin.x
            + faceBounds.size.width * x / region.pointCount;
    y = faceBounds.origin.y
            + faceBounds.size.height * y / region.pointCount;
    return @{@"x": @(x), @"y": @(1.0 - y)};
}

static NSDictionary *cn1VisionFaceLandmarkEdge(
        VNFaceLandmarkRegion2D *region, CGRect faceBounds, BOOL left) {
    if (region == nil || region.pointCount == 0
            || region.normalizedPoints == NULL) {
        return nil;
    }
    CGPoint selected = region.normalizedPoints[0];
    for (NSUInteger i = 1; i < region.pointCount; i++) {
        CGPoint candidate = region.normalizedPoints[i];
        if ((left && candidate.x < selected.x)
                || (!left && candidate.x > selected.x)) {
            selected = candidate;
        }
    }
    double x = faceBounds.origin.x
            + faceBounds.size.width * selected.x;
    double y = faceBounds.origin.y
            + faceBounds.size.height * selected.y;
    return @{@"x": @(x), @"y": @(1.0 - y)};
}

static void cn1VisionAddFaceLandmark(NSMutableDictionary *landmarks,
        NSString *name, VNFaceLandmarkRegion2D *region, CGRect faceBounds) {
    NSDictionary *point = cn1VisionFaceLandmark(region, faceBounds);
    if (point != nil) {
        landmarks[name] = point;
    }
}

static NSString *cn1VisionJSON(NSDictionary *value) {
    NSError *error = nil;
    NSData *data = [NSJSONSerialization dataWithJSONObject:value options:0 error:&error];
    if (data == nil) {
        return [NSString stringWithFormat:@"{\"error\":\"%@\"}",
                error.localizedDescription ?: @"Could not encode result"];
    }
    return [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
}

static NSString *cn1VisionError(NSError *error) {
    return cn1VisionJSON(@{
        @"error": error.localizedDescription ?: @"Apple Vision request failed"
    });
}

#if defined(CN1_HAS_MLKIT_VISION)
static UIImageOrientation cn1UIImageOrientation(int rotation) {
    switch (rotation) {
        case 90: return UIImageOrientationRight;
        case 180: return UIImageOrientationDown;
        case 270: return UIImageOrientationLeft;
        default: return UIImageOrientationUp;
    }
}

static MLKVisionImage *cn1MLKitImage(UIImage *image, int rotation) {
    MLKVisionImage *vision = [[MLKVisionImage alloc] initWithImage:image];
    vision.orientation = cn1UIImageOrientation(rotation);
    return vision;
}
#endif

#if defined(CN1_HAS_MLKIT_BARCODE)
static NSString *cn1MLKitBarcodeFormat(MLKBarcodeFormat format) {
    switch (format) {
        case MLKBarcodeFormatAztec: return @"AZTEC";
        case MLKBarcodeFormatCodaBar: return @"CODABAR";
        case MLKBarcodeFormatCode39: return @"CODE_39";
        case MLKBarcodeFormatCode93: return @"CODE_93";
        case MLKBarcodeFormatCode128: return @"CODE_128";
        case MLKBarcodeFormatDataMatrix: return @"DATA_MATRIX";
        case MLKBarcodeFormatEAN8: return @"EAN_8";
        case MLKBarcodeFormatEAN13: return @"EAN_13";
        case MLKBarcodeFormatITF: return @"ITF";
        case MLKBarcodeFormatPDF417: return @"PDF417";
        case MLKBarcodeFormatQRCode: return @"QR_CODE";
        case MLKBarcodeFormatUPCA: return @"UPC_A";
        case MLKBarcodeFormatUPCE: return @"UPC_E";
        default: return @"UNKNOWN";
    }
}
#endif

static NSString *cn1MLKitVisionPerform(NSData *data, CGImageRef rawImage,
                                       int feature, int rotation) {
#if defined(CN1_HAS_MLKIT_VISION)
    UIImage *image = rawImage == NULL ? [UIImage imageWithData:data]
            : [UIImage imageWithCGImage:rawImage];
    if (image == nil) {
        return cn1VisionJSON(@{@"error": @"Could not decode ML Kit image"});
    }
    MLKVisionImage *vision = cn1MLKitImage(image, rotation);
    CGSize resultSize = (rotation == 90 || rotation == 270)
            ? CGSizeMake(image.size.height, image.size.width) : image.size;
    __block NSError *requestError = nil;
    dispatch_semaphore_t semaphore = dispatch_semaphore_create(0);

    if (feature == 0) {
#if defined(CN1_HAS_MLKIT_TEXT)
        MLKTextRecognizer *recognizer = [MLKTextRecognizer textRecognizerWithOptions:
                [[MLKTextRecognizerOptions alloc] init]];
        __block MLKText *result = nil;
        [recognizer processImage:vision completion:^(MLKText *text, NSError *error) {
            result = text;
            requestError = error;
            dispatch_semaphore_signal(semaphore);
        }];
        dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER);
        if (requestError != nil) return cn1VisionError(requestError);
        NSMutableArray *items = [NSMutableArray array];
        for (MLKTextBlock *block in result.blocks ?: @[]) {
            NSMutableDictionary *item = [NSMutableDictionary
                    dictionaryWithDictionary:cn1MLKitRect(block.frame, resultSize)];
            item[@"text"] = block.text ?: @"";
            item[@"confidence"] = @1;
            [items addObject:item];
        }
        return cn1VisionJSON(@{
            @"text": result.text ?: @"",
            @"items": items
        });
#endif
    } else if (feature == 1) {
#if defined(CN1_HAS_MLKIT_BARCODE)
        MLKBarcodeScannerOptions *options = [[MLKBarcodeScannerOptions alloc] init];
        MLKBarcodeScanner *scanner =
                [MLKBarcodeScanner barcodeScannerWithOptions:options];
        __block NSArray<MLKBarcode *> *result = nil;
        [scanner processImage:vision completion:^(NSArray<MLKBarcode *> *barcodes,
                                                  NSError *error) {
            result = barcodes;
            requestError = error;
            dispatch_semaphore_signal(semaphore);
        }];
        dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER);
        if (requestError != nil) return cn1VisionError(requestError);
        NSMutableArray *items = [NSMutableArray array];
        for (MLKBarcode *barcode in result ?: @[]) {
            NSMutableDictionary *item = [NSMutableDictionary
                    dictionaryWithDictionary:cn1MLKitRect(barcode.frame, resultSize)];
            item[@"value"] = barcode.rawValue ?: [NSNull null];
            item[@"format"] = cn1MLKitBarcodeFormat(barcode.format);
            NSMutableArray *corners = [NSMutableArray array];
            for (NSValue *pointValue in barcode.cornerPoints ?: @[]) {
                CGPoint point = pointValue.CGPointValue;
                [corners addObject:@{
                    @"x": @(point.x / resultSize.width),
                    @"y": @(point.y / resultSize.height)
                }];
            }
            item[@"corners"] = corners;
            [items addObject:item];
        }
        return cn1VisionJSON(@{@"items": items});
#endif
    } else if (feature == 2) {
#if defined(CN1_HAS_MLKIT_FACE)
        MLKFaceDetectorOptions *options = [[MLKFaceDetectorOptions alloc] init];
        options.landmarkMode = MLKFaceDetectorLandmarkModeAll;
        options.classificationMode = MLKFaceDetectorClassificationModeAll;
        options.trackingEnabled = YES;
        MLKFaceDetector *detector = [MLKFaceDetector faceDetectorWithOptions:options];
        __block NSArray<MLKFace *> *result = nil;
        [detector processImage:vision completion:^(NSArray<MLKFace *> *faces,
                                                   NSError *error) {
            result = faces;
            requestError = error;
            dispatch_semaphore_signal(semaphore);
        }];
        dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER);
        if (requestError != nil) return cn1VisionError(requestError);
        NSMutableArray *items = [NSMutableArray array];
        for (MLKFace *face in result ?: @[]) {
            NSMutableDictionary *item = [NSMutableDictionary
                    dictionaryWithDictionary:cn1MLKitRect(face.frame, resultSize)];
            NSMutableDictionary *landmarks = [NSMutableDictionary dictionary];
            cn1MLKitAddFaceLandmark(landmarks, @"leftEye", face,
                    MLKFaceLandmarkTypeLeftEye, resultSize);
            cn1MLKitAddFaceLandmark(landmarks, @"rightEye", face,
                    MLKFaceLandmarkTypeRightEye, resultSize);
            cn1MLKitAddFaceLandmark(landmarks, @"noseBase", face,
                    MLKFaceLandmarkTypeNoseBase, resultSize);
            cn1MLKitAddFaceLandmark(landmarks, @"mouthLeft", face,
                    MLKFaceLandmarkTypeMouthLeft, resultSize);
            cn1MLKitAddFaceLandmark(landmarks, @"mouthRight", face,
                    MLKFaceLandmarkTypeMouthRight, resultSize);
            item[@"landmarks"] = landmarks;
            item[@"yaw"] = @(face.headEulerAngleY);
            item[@"pitch"] = @(face.headEulerAngleX);
            item[@"roll"] = @(face.headEulerAngleZ);
            item[@"smilingProbability"] = face.hasSmilingProbability
                    ? @(face.smilingProbability) : @(-1);
            item[@"trackingId"] = face.hasTrackingID
                    ? @(face.trackingID) : @(-1);
            [items addObject:item];
        }
        return cn1VisionJSON(@{@"items": items});
#endif
    } else if (feature == 3) {
#if defined(CN1_HAS_MLKIT_LABEL)
        MLKImageLabelerOptions *options = [[MLKImageLabelerOptions alloc] init];
        MLKImageLabeler *labeler = [MLKImageLabeler imageLabelerWithOptions:options];
        __block NSArray<MLKImageLabel *> *result = nil;
        [labeler processImage:vision completion:^(NSArray<MLKImageLabel *> *labels,
                                                  NSError *error) {
            result = labels;
            requestError = error;
            dispatch_semaphore_signal(semaphore);
        }];
        dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER);
        if (requestError != nil) return cn1VisionError(requestError);
        NSMutableArray *items = [NSMutableArray array];
        for (MLKImageLabel *label in result ?: @[]) {
            [items addObject:@{
                @"text": label.text ?: @"",
                @"confidence": @(label.confidence)
            }];
        }
        return cn1VisionJSON(@{@"items": items});
#endif
    } else if (feature == 4) {
#if defined(CN1_HAS_MLKIT_POSE)
        MLKPoseDetectorOptions *options = [[MLKPoseDetectorOptions alloc] init];
        options.detectorMode = MLKPoseDetectorModeSingleImage;
        MLKPoseDetector *detector = [MLKPoseDetector poseDetectorWithOptions:options];
        __block NSArray<MLKPose *> *result = nil;
        [detector processImage:vision completion:^(NSArray<MLKPose *> *poses,
                                                   NSError *error) {
            result = poses;
            requestError = error;
            dispatch_semaphore_signal(semaphore);
        }];
        dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER);
        if (requestError != nil) return cn1VisionError(requestError);
        NSMutableArray *items = [NSMutableArray array];
        MLKPose *pose = result.firstObject;
        for (MLKPoseLandmark *landmark in pose.landmarks ?: @[]) {
            [items addObject:@{
                @"name": cn1MLKitPoseLandmarkName(landmark.type),
                @"x": @(landmark.position.x / resultSize.width),
                @"y": @(landmark.position.y / resultSize.height),
                @"confidence": @(landmark.inFrameLikelihood)
            }];
        }
        return cn1VisionJSON(@{@"items": items});
#endif
    } else if (feature == 5) {
#if defined(CN1_HAS_MLKIT_SEGMENTATION)
        MLKSelfieSegmenterOptions *options =
                [[MLKSelfieSegmenterOptions alloc] init];
        options.segmenterMode = MLKSegmenterModeSingleImage;
        MLKSegmenter *segmenter = [MLKSegmenter segmenterWithOptions:options];
        __block MLKSegmentationMask *result = nil;
        [segmenter processImage:vision completion:^(MLKSegmentationMask *mask,
                                                     NSError *error) {
            result = mask;
            requestError = error;
            dispatch_semaphore_signal(semaphore);
        }];
        dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER);
        if (requestError != nil) return cn1VisionError(requestError);
        CVPixelBufferRef buffer = result.buffer;
        if (buffer == nil) {
            return cn1VisionJSON(@{@"error": @"No segmentation mask returned"});
        }
        CVPixelBufferLockBaseAddress(buffer, kCVPixelBufferLock_ReadOnly);
        size_t width = CVPixelBufferGetWidth(buffer);
        size_t height = CVPixelBufferGetHeight(buffer);
        size_t stride = CVPixelBufferGetBytesPerRow(buffer);
        const uint8_t *base = CVPixelBufferGetBaseAddress(buffer);
        NSMutableData *packed = [NSMutableData dataWithLength:width * height];
        uint8_t *dest = packed.mutableBytes;
        for (size_t y = 0; y < height; y++) {
            const float *row = (const float *)(base + y * stride);
            for (size_t x = 0; x < width; x++) {
                float confidence = fmaxf(0, fminf(1, row[x]));
                dest[y * width + x] = (uint8_t)lrintf(confidence * 255);
            }
        }
        CVPixelBufferUnlockBaseAddress(buffer, kCVPixelBufferLock_ReadOnly);
        return cn1VisionJSON(@{
            @"width": @(width),
            @"height": @(height),
            @"dataPeer": @((uint64_t)CFBridgingRetain(packed))
        });
#endif
    }
    return cn1VisionJSON(@{
        @"error": @"Requested ML Kit vision component is not linked"
    });
#else
    return cn1VisionJSON(@{@"error": @"ML Kit Vision is not linked"});
#endif
}

static CGImagePropertyOrientation cn1CGOrientation(int rotation) {
    switch (rotation) {
        case 90: return kCGImagePropertyOrientationRight;
        case 180: return kCGImagePropertyOrientationDown;
        case 270: return kCGImagePropertyOrientationLeft;
        default: return kCGImagePropertyOrientationUp;
    }
}

static NSString *cn1AppleBarcodeFormat(NSString *symbology) {
    NSString *value = symbology.uppercaseString;
    if ([value containsString:@"QR"]) return @"QR_CODE";
    if ([value containsString:@"DATAMATRIX"]) return @"DATA_MATRIX";
    if ([value containsString:@"PDF417"]) return @"PDF417";
    if ([value containsString:@"CODE128"]) return @"CODE_128";
    if ([value containsString:@"CODE93"]) return @"CODE_93";
    if ([value containsString:@"CODE39"]) return @"CODE_39";
    if ([value containsString:@"EAN13"]) return @"EAN_13";
    if ([value containsString:@"EAN8"]) return @"EAN_8";
    if ([value containsString:@"UPCE"]) return @"UPC_E";
    if ([value containsString:@"ITF"]) return @"ITF";
    if ([value containsString:@"AZTEC"]) return @"AZTEC";
    return @"UNKNOWN";
}

static NSString *cn1ApplePoseLandmarkName(
        VNHumanBodyPoseObservationJointName name) {
    if ([name isEqual:VNHumanBodyPoseObservationJointNameNose]) return @"nose";
    if ([name isEqual:VNHumanBodyPoseObservationJointNameLeftEye]) return @"leftEye";
    if ([name isEqual:VNHumanBodyPoseObservationJointNameRightEye]) return @"rightEye";
    if ([name isEqual:VNHumanBodyPoseObservationJointNameLeftEar]) return @"leftEar";
    if ([name isEqual:VNHumanBodyPoseObservationJointNameRightEar]) return @"rightEar";
    if ([name isEqual:VNHumanBodyPoseObservationJointNameLeftShoulder]) return @"leftShoulder";
    if ([name isEqual:VNHumanBodyPoseObservationJointNameRightShoulder]) return @"rightShoulder";
    if ([name isEqual:VNHumanBodyPoseObservationJointNameNeck]) return @"neck";
    if ([name isEqual:VNHumanBodyPoseObservationJointNameLeftElbow]) return @"leftElbow";
    if ([name isEqual:VNHumanBodyPoseObservationJointNameRightElbow]) return @"rightElbow";
    if ([name isEqual:VNHumanBodyPoseObservationJointNameLeftWrist]) return @"leftWrist";
    if ([name isEqual:VNHumanBodyPoseObservationJointNameRightWrist]) return @"rightWrist";
    if ([name isEqual:VNHumanBodyPoseObservationJointNameLeftHip]) return @"leftHip";
    if ([name isEqual:VNHumanBodyPoseObservationJointNameRightHip]) return @"rightHip";
    if ([name isEqual:VNHumanBodyPoseObservationJointNameRoot]) return @"root";
    if ([name isEqual:VNHumanBodyPoseObservationJointNameLeftKnee]) return @"leftKnee";
    if ([name isEqual:VNHumanBodyPoseObservationJointNameRightKnee]) return @"rightKnee";
    if ([name isEqual:VNHumanBodyPoseObservationJointNameLeftAnkle]) return @"leftAnkle";
    if ([name isEqual:VNHumanBodyPoseObservationJointNameRightAnkle]) return @"rightAnkle";
    return name ?: @"unknown";
}

static NSString *cn1VisionPerform(NSData *data, CGImageRef rawImage,
                                  int feature, int rotation) {
    NSError *error = nil;
    VNImageRequestHandler *handler = rawImage == NULL
            ? [[VNImageRequestHandler alloc] initWithData:data
                    orientation:cn1CGOrientation(rotation) options:@{}]
            : [[VNImageRequestHandler alloc] initWithCGImage:rawImage
                    orientation:cn1CGOrientation(rotation) options:@{}];

    if (feature == 0) {
        if (@available(iOS 13.0, *)) {
            VNRecognizeTextRequest *request = [[VNRecognizeTextRequest alloc] init];
            request.recognitionLevel = VNRequestTextRecognitionLevelAccurate;
            if (![handler performRequests:@[request] error:&error]) {
                return cn1VisionError(error);
            }
            NSMutableArray *items = [NSMutableArray array];
            NSMutableArray *lines = [NSMutableArray array];
            for (VNRecognizedTextObservation *observation in request.results) {
                VNRecognizedText *candidate = [observation topCandidates:1].firstObject;
                if (candidate == nil) continue;
                NSMutableDictionary *item =
                        [NSMutableDictionary dictionaryWithDictionary:
                                cn1VisionRect(observation.boundingBox)];
                item[@"text"] = candidate.string ?: @"";
                item[@"confidence"] = @(candidate.confidence);
                [items addObject:item];
                [lines addObject:candidate.string ?: @""];
            }
            return cn1VisionJSON(@{
                @"text": [lines componentsJoinedByString:@"\n"],
                @"items": items
            });
        }
    } else if (feature == 1) {
        VNDetectBarcodesRequest *request = [[VNDetectBarcodesRequest alloc] init];
#if TARGET_OS_SIMULATOR
        /*
         * Recent iOS simulator runtimes can successfully perform the default
         * barcode request while returning no observations for valid QR
         * images. Revision 1 avoids that simulator-only Vision regression.
         * Devices retain the latest revision and its additional symbologies.
         */
        request.revision = VNDetectBarcodesRequestRevision1;
#endif
        if (![handler performRequests:@[request] error:&error]) {
            return cn1VisionError(error);
        }
        NSMutableArray *items = [NSMutableArray array];
        for (VNBarcodeObservation *observation in request.results) {
            NSMutableDictionary *item =
                    [NSMutableDictionary dictionaryWithDictionary:
                            cn1VisionRect(observation.boundingBox)];
            item[@"value"] = observation.payloadStringValue ?: [NSNull null];
            item[@"format"] = cn1AppleBarcodeFormat(observation.symbology);
            item[@"corners"] = @[
                @{@"x": @(observation.topLeft.x),
                  @"y": @(1.0 - observation.topLeft.y)},
                @{@"x": @(observation.topRight.x),
                  @"y": @(1.0 - observation.topRight.y)},
                @{@"x": @(observation.bottomRight.x),
                  @"y": @(1.0 - observation.bottomRight.y)},
                @{@"x": @(observation.bottomLeft.x),
                  @"y": @(1.0 - observation.bottomLeft.y)}
            ];
            [items addObject:item];
        }
        return cn1VisionJSON(@{@"items": items});
    } else if (feature == 2) {
        VNDetectFaceLandmarksRequest *request = [[VNDetectFaceLandmarksRequest alloc] init];
        if (![handler performRequests:@[request] error:&error]) {
            return cn1VisionError(error);
        }
        NSMutableArray *items = [NSMutableArray array];
        for (VNFaceObservation *observation in request.results) {
            NSMutableDictionary *item =
                    [NSMutableDictionary dictionaryWithDictionary:
                            cn1VisionRect(observation.boundingBox)];
            NSMutableDictionary *landmarks = [NSMutableDictionary dictionary];
            VNFaceLandmarks2D *faceLandmarks = observation.landmarks;
            cn1VisionAddFaceLandmark(landmarks, @"leftEye",
                    faceLandmarks.leftEye, observation.boundingBox);
            cn1VisionAddFaceLandmark(landmarks, @"rightEye",
                    faceLandmarks.rightEye, observation.boundingBox);
            cn1VisionAddFaceLandmark(landmarks, @"noseBase",
                    faceLandmarks.nose, observation.boundingBox);
            NSDictionary *mouthLeft = cn1VisionFaceLandmarkEdge(
                    faceLandmarks.outerLips, observation.boundingBox, YES);
            NSDictionary *mouthRight = cn1VisionFaceLandmarkEdge(
                    faceLandmarks.outerLips, observation.boundingBox, NO);
            if (mouthLeft != nil) landmarks[@"mouthLeft"] = mouthLeft;
            if (mouthRight != nil) landmarks[@"mouthRight"] = mouthRight;
            item[@"landmarks"] = landmarks;
            item[@"yaw"] = @((observation.yaw ?: @0).doubleValue
                    * 180.0 / M_PI);
            if (@available(iOS 15.0, *)) {
                item[@"pitch"] = @((observation.pitch ?: @0).doubleValue
                        * 180.0 / M_PI);
            } else {
                item[@"pitch"] = @0;
            }
            item[@"roll"] = @((observation.roll ?: @0).doubleValue
                    * 180.0 / M_PI);
            item[@"smilingProbability"] = @(-1);
            item[@"trackingId"] = @(-1);
            [items addObject:item];
        }
        return cn1VisionJSON(@{@"items": items});
    } else if (feature == 3) {
        if (@available(iOS 15.0, *)) {
            VNClassifyImageRequest *request = [[VNClassifyImageRequest alloc] init];
            if (![handler performRequests:@[request] error:&error]) {
                return cn1VisionError(error);
            }
            NSMutableArray *items = [NSMutableArray array];
            for (VNClassificationObservation *observation in request.results) {
                [items addObject:@{
                    @"text": observation.identifier ?: @"",
                    @"confidence": @(observation.confidence)
                }];
            }
            return cn1VisionJSON(@{@"items": items});
        }
    } else if (feature == 4) {
        if (@available(iOS 14.0, *)) {
            VNDetectHumanBodyPoseRequest *request =
                    [[VNDetectHumanBodyPoseRequest alloc] init];
            if (![handler performRequests:@[request] error:&error]) {
                return cn1VisionError(error);
            }
            NSMutableArray *items = [NSMutableArray array];
            VNHumanBodyPoseObservation *pose = request.results.firstObject;
            NSDictionary<VNHumanBodyPoseObservationJointName,
                    VNRecognizedPoint *> *points =
                    [pose recognizedPointsForGroupKey:
                            VNHumanBodyPoseObservationJointsGroupNameAll error:&error];
            if (error != nil) {
                return cn1VisionError(error);
            }
            for (NSString *name in points) {
                VNRecognizedPoint *point = points[name];
                [items addObject:@{
                    @"name": cn1ApplePoseLandmarkName(name),
                    @"x": @(point.location.x),
                    @"y": @(1.0 - point.location.y),
                    @"confidence": @(point.confidence)
                }];
            }
            return cn1VisionJSON(@{@"items": items});
        }
    } else if (feature == 5) {
        if (@available(iOS 15.0, *)) {
            VNGeneratePersonSegmentationRequest *request =
                    [[VNGeneratePersonSegmentationRequest alloc] init];
            request.qualityLevel = VNGeneratePersonSegmentationRequestQualityLevelBalanced;
            request.outputPixelFormat = kCVPixelFormatType_OneComponent8;
            if (![handler performRequests:@[request] error:&error]) {
                return cn1VisionError(error);
            }
            VNPixelBufferObservation *observation = request.results.firstObject;
            CVPixelBufferRef buffer = observation.pixelBuffer;
            if (buffer == nil) {
                return cn1VisionJSON(@{@"error": @"No segmentation mask returned"});
            }
            CVPixelBufferLockBaseAddress(buffer, kCVPixelBufferLock_ReadOnly);
            size_t width = CVPixelBufferGetWidth(buffer);
            size_t height = CVPixelBufferGetHeight(buffer);
            size_t stride = CVPixelBufferGetBytesPerRow(buffer);
            const uint8_t *base = CVPixelBufferGetBaseAddress(buffer);
            NSMutableData *packed = [NSMutableData dataWithLength:width * height];
            uint8_t *dest = packed.mutableBytes;
            for (size_t y = 0; y < height; y++) {
                memcpy(dest + y * width, base + y * stride, width);
            }
            CVPixelBufferUnlockBaseAddress(buffer, kCVPixelBufferLock_ReadOnly);
            return cn1VisionJSON(@{
                @"width": @(width),
                @"height": @(height),
                @"dataPeer": @((uint64_t)CFBridgingRetain(packed))
            });
        }
    } else if (feature == 6) {
        UIImage *image = rawImage == NULL ? [UIImage imageWithData:data]
                : [UIImage imageWithCGImage:rawImage];
        if (image == nil) {
            return cn1VisionJSON(@{@"error": @"Could not decode document image"});
        }
        CIImage *source = [CIImage imageWithCGImage:image.CGImage];
        if (rotation != 0) {
            source = [source imageByApplyingOrientation:
                    cn1CGOrientation(rotation)];
        }
        CIContext *context = [CIContext context];
        CIDetector *detector = [CIDetector detectorOfType:CIDetectorTypeRectangle
                                                 context:context
                                                 options:@{
                    CIDetectorAccuracy: CIDetectorAccuracyHigh
                }];
        NSArray *features = [detector featuresInImage:source];
        CIImage *corrected = source;
        if (features.count > 0) {
            CIRectangleFeature *rectangle = features.firstObject;
            corrected = [source imageByApplyingFilter:@"CIPerspectiveCorrection"
                    withInputParameters:@{
                @"inputTopLeft": [CIVector vectorWithCGPoint:rectangle.topLeft],
                @"inputTopRight": [CIVector vectorWithCGPoint:rectangle.topRight],
                @"inputBottomLeft": [CIVector vectorWithCGPoint:rectangle.bottomLeft],
                @"inputBottomRight": [CIVector vectorWithCGPoint:rectangle.bottomRight]
            }];
        }
        CGImageRef outputImage = [context createCGImage:corrected
                                               fromRect:corrected.extent];
        UIImage *output = [UIImage imageWithCGImage:outputImage];
        CGImageRelease(outputImage);
        NSData *jpeg = UIImageJPEGRepresentation(output, 0.92);
        return cn1VisionJSON(@{
            @"dataPeer": @((uint64_t)CFBridgingRetain(jpeg))
        });
    }
    return cn1VisionJSON(@{@"error": @"Vision feature is unavailable on this OS"});
}
#endif

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_cn1VisionIsSupported___int_boolean_R_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT feature,
        JAVA_BOOLEAN mlKit) {
#if defined(INCLUDE_CN1_VISION) && !TARGET_OS_WATCH && !TARGET_OS_TV
    if (mlKit) {
        switch (feature) {
            case 0:
#if defined(CN1_HAS_MLKIT_TEXT)
                return 1;
#else
                return 0;
#endif
            case 1:
#if defined(CN1_HAS_MLKIT_BARCODE)
                return 1;
#else
                return 0;
#endif
            case 2:
#if defined(CN1_HAS_MLKIT_FACE)
                return 1;
#else
                return 0;
#endif
            case 3:
#if defined(CN1_HAS_MLKIT_LABEL)
                return 1;
#else
                return 0;
#endif
            case 4:
#if defined(CN1_HAS_MLKIT_POSE)
                return 1;
#else
                return 0;
#endif
            case 5:
#if defined(CN1_HAS_MLKIT_SEGMENTATION)
                return 1;
#else
                return 0;
#endif
            default:
                return 0;
        }
    }
    switch (feature) {
        case 0:
            if (@available(iOS 13.0, *)) return 1;
            return 0;
        case 3:
        case 5:
            if (@available(iOS 15.0, *)) return 1;
            return 0;
        case 4:
            if (@available(iOS 14.0, *)) return 1;
            return 0;
        default:
            return 1;
    }
#else
    return 0;
#endif
}

#if defined(INCLUDE_CN1_VISION) && !TARGET_OS_WATCH && !TARGET_OS_TV
static uint8_t cn1VisionClamp(int value) {
    return (uint8_t)(value < 0 ? 0 : (value > 255 ? 255 : value));
}

/*
 * Converts the two raw CameraFrame formats to an uncompressed CGImage so
 * Vision and ML Kit avoid a lossy JPEG encode/decode cycle on every frame.
 * format mirrors FrameFormat: 0=JPEG/encoded, 1=NV21, 2=RGBA8888.
 */
static CGImageRef cn1VisionCreateRawImage(NSData *data, int width, int height,
                                         int format) {
    if (format == 0) {
        return NULL;
    }
    if (width <= 0 || height <= 0) {
        return NULL;
    }
    if (format != 1 && format != 2) {
        return NULL;
    }
    if (format == 1 && ((width & 1) != 0 || (height & 1) != 0)) {
        return NULL;
    }
    NSUInteger imageWidth = (NSUInteger)width;
    NSUInteger imageHeight = (NSUInteger)height;
    if (imageWidth > NSUIntegerMax / imageHeight) {
        return NULL;
    }
    NSUInteger pixelCount = imageWidth * imageHeight;
    NSUInteger requiredLength;
    if (format == 2) {
        if (pixelCount > NSUIntegerMax / 4) {
            return NULL;
        }
        requiredLength = pixelCount * 4;
    } else {
        if (pixelCount > NSUIntegerMax - pixelCount / 2) {
            return NULL;
        }
        requiredLength = pixelCount + pixelCount / 2;
    }
    if (data.length < requiredLength) {
        return NULL;
    }
    const uint8_t *source = data.bytes;
    NSMutableData *rgba = [NSMutableData dataWithLength:pixelCount * 4];
    uint8_t *dest = rgba.mutableBytes;
    if (format == 2) {
        memcpy(dest, source, pixelCount * 4);
    } else {
        for (int y = 0; y < height; y++) {
            NSUInteger uvRow = pixelCount
                    + (NSUInteger)(y >> 1) * imageWidth;
            for (int x = 0; x < width; x++) {
                int yy = source[(NSUInteger)y * imageWidth
                        + (NSUInteger)x] & 255;
                NSUInteger uv = uvRow + (NSUInteger)(x & ~1);
                int v = (source[uv] & 255) - 128;
                int u = (source[uv + 1] & 255) - 128;
                int c = yy - 16;
                if (c < 0) c = 0;
                NSUInteger p = ((NSUInteger)y * imageWidth
                        + (NSUInteger)x) * 4;
                dest[p] = cn1VisionClamp((298 * c + 409 * v + 128) >> 8);
                dest[p + 1] = cn1VisionClamp(
                        (298 * c - 100 * u - 208 * v + 128) >> 8);
                dest[p + 2] = cn1VisionClamp(
                        (298 * c + 516 * u + 128) >> 8);
                dest[p + 3] = 255;
            }
        }
    }
    CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
    CGDataProviderRef provider = CGDataProviderCreateWithCFData(
            (CFDataRef)rgba);
    CGImageRef cgImage = CGImageCreate(imageWidth, imageHeight, 8, 32,
            imageWidth * 4,
            colorSpace, kCGBitmapByteOrderDefault | kCGImageAlphaLast,
            provider, NULL, false, kCGRenderingIntentDefault);
    CGDataProviderRelease(provider);
    CGColorSpaceRelease(colorSpace);
    return cgImage;
}
#endif

JAVA_OBJECT com_codename1_impl_ios_IOSNative_cn1VisionAnalyze___byte_1ARRAY_int_boolean_int_int_int_int_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject,
        JAVA_OBJECT encodedImage, JAVA_INT feature, JAVA_BOOLEAN mlKit,
        JAVA_INT rotation, JAVA_INT width, JAVA_INT height,
        JAVA_INT frameFormat) {
#if defined(INCLUDE_CN1_VISION) && !TARGET_OS_WATCH && !TARGET_OS_TV
    if (encodedImage == JAVA_NULL) {
        return fromNSString(CN1_THREAD_GET_STATE_PASS_ARG
                @"{\"error\":\"Image data is null\"}");
    }
    JAVA_ARRAY bytes = (JAVA_ARRAY) encodedImage;
    NSData *data = [NSData dataWithBytes:bytes->data length:(NSUInteger) bytes->length];
    CGImageRef rawImage = cn1VisionCreateRawImage(
            data, width, height, frameFormat);
    if (frameFormat != 0 && rawImage == NULL) {
        return fromNSString(CN1_THREAD_GET_STATE_PASS_ARG
                @"{\"error\":\"Invalid raw vision image\"}");
    }
    NSString *result = mlKit
            ? cn1MLKitVisionPerform(data, rawImage, feature, rotation)
            : cn1VisionPerform(data, rawImage, feature, rotation);
    if (rawImage != NULL) CGImageRelease(rawImage);
    return fromNSString(CN1_THREAD_GET_STATE_PASS_ARG result);
#else
    return JAVA_NULL;
#endif
}
