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

static NSString *cn1MLKitVisionPerform(NSData *data, int feature, int rotation) {
#if defined(CN1_HAS_MLKIT_VISION)
    UIImage *image = [UIImage imageWithData:data];
    if (image == nil) {
        return cn1VisionJSON(@{@"error": @"Could not decode ML Kit image"});
    }
    MLKVisionImage *vision = cn1MLKitImage(image, rotation);
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
                    dictionaryWithDictionary:cn1MLKitRect(block.frame, image.size)];
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
                    dictionaryWithDictionary:cn1MLKitRect(barcode.frame, image.size)];
            item[@"value"] = barcode.rawValue ?: [NSNull null];
            item[@"format"] = [NSString stringWithFormat:@"%ld",
                    (long)barcode.format];
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
                    dictionaryWithDictionary:cn1MLKitRect(face.frame, image.size)];
            item[@"yaw"] = @(face.headEulerAngleY);
            item[@"pitch"] = @(face.headEulerAngleX);
            item[@"roll"] = @(face.headEulerAngleZ);
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
                @"name": [NSString stringWithFormat:@"%ld",
                        (long)landmark.type],
                @"x": @(landmark.position.x / image.size.width),
                @"y": @(landmark.position.y / image.size.height),
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
            @"data": [packed base64EncodedStringWithOptions:0]
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

static NSString *cn1VisionPerform(NSData *data, int feature, int rotation) {
    NSError *error = nil;
    VNImageRequestHandler *handler =
            [[VNImageRequestHandler alloc] initWithData:data
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
        if (![handler performRequests:@[request] error:&error]) {
            return cn1VisionError(error);
        }
        NSMutableArray *items = [NSMutableArray array];
        for (VNBarcodeObservation *observation in request.results) {
            NSMutableDictionary *item =
                    [NSMutableDictionary dictionaryWithDictionary:
                            cn1VisionRect(observation.boundingBox)];
            item[@"value"] = observation.payloadStringValue ?: [NSNull null];
            item[@"format"] = observation.symbology ?: @"UNKNOWN";
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
            item[@"yaw"] = @((observation.yaw ?: @0).doubleValue
                    * 180.0 / M_PI);
            item[@"pitch"] = @0;
            item[@"roll"] = @((observation.roll ?: @0).doubleValue
                    * 180.0 / M_PI);
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
                    @"name": name,
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
                @"data": [packed base64EncodedStringWithOptions:0]
            });
        }
    } else if (feature == 6) {
        UIImage *image = [UIImage imageWithData:data];
        if (image == nil) {
            return cn1VisionJSON(@{@"error": @"Could not decode document image"});
        }
        CIImage *source = [CIImage imageWithCGImage:image.CGImage];
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
            @"data": [jpeg base64EncodedStringWithOptions:0]
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
 * Converts the two raw CameraFrame formats to JPEG so both Apple Vision and
 * the optional ML Kit path consume identical, orientation-neutral data.
 * format mirrors FrameFormat: 0=JPEG/encoded, 1=NV21, 2=RGBA8888.
 */
static NSData *cn1VisionEncodeInput(NSData *data, int width, int height,
                                    int format) {
    if (format == 0) {
        return data;
    }
    if (width <= 0 || height <= 0) {
        return nil;
    }
    NSUInteger pixelCount = (NSUInteger)width * (NSUInteger)height;
    const uint8_t *source = data.bytes;
    NSMutableData *rgba = [NSMutableData dataWithLength:pixelCount * 4];
    uint8_t *dest = rgba.mutableBytes;
    if (format == 2) {
        if (data.length < pixelCount * 4) {
            return nil;
        }
        memcpy(dest, source, pixelCount * 4);
    } else if (format == 1) {
        if (data.length < pixelCount + pixelCount / 2) {
            return nil;
        }
        for (int y = 0; y < height; y++) {
            int uvRow = width * height + (y >> 1) * width;
            for (int x = 0; x < width; x++) {
                int yy = source[y * width + x] & 255;
                int uv = uvRow + (x & ~1);
                int v = (source[uv] & 255) - 128;
                int u = (source[uv + 1] & 255) - 128;
                int c = yy - 16;
                if (c < 0) c = 0;
                NSUInteger p = ((NSUInteger)y * width + x) * 4;
                dest[p] = cn1VisionClamp((298 * c + 409 * v + 128) >> 8);
                dest[p + 1] = cn1VisionClamp(
                        (298 * c - 100 * u - 208 * v + 128) >> 8);
                dest[p + 2] = cn1VisionClamp(
                        (298 * c + 516 * u + 128) >> 8);
                dest[p + 3] = 255;
            }
        }
    } else {
        return nil;
    }
    CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
    CGDataProviderRef provider = CGDataProviderCreateWithCFData(
            (CFDataRef)rgba);
    CGImageRef cgImage = CGImageCreate(width, height, 8, 32, width * 4,
            colorSpace, kCGBitmapByteOrderDefault | kCGImageAlphaLast,
            provider, NULL, false, kCGRenderingIntentDefault);
    UIImage *image = cgImage == NULL ? nil
            : [UIImage imageWithCGImage:cgImage];
    NSData *encoded = image == nil ? nil
            : UIImageJPEGRepresentation(image, 0.95);
    if (cgImage != NULL) CGImageRelease(cgImage);
    CGDataProviderRelease(provider);
    CGColorSpaceRelease(colorSpace);
    return encoded;
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
    data = cn1VisionEncodeInput(data, width, height, frameFormat);
    if (data == nil) {
        return fromNSString(CN1_THREAD_GET_STATE_PASS_ARG
                @"{\"error\":\"Invalid raw vision image\"}");
    }
    return fromNSString(CN1_THREAD_GET_STATE_PASS_ARG
            mlKit ? cn1MLKitVisionPerform(data, feature, rotation)
                  : cn1VisionPerform(data, feature, rotation));
#else
    return JAVA_NULL;
#endif
}
