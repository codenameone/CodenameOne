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
#import <Foundation/Foundation.h>

#ifdef INCLUDE_CAMERA_USAGE
#import <UIKit/UIKit.h>
#import <AVFoundation/AVFoundation.h>

/// AVFoundation-backed camera session owned by `IOSCameraImpl` on the Java
/// side. One instance per `CameraSession`. The Java native methods on
/// `IOSNative` create / mutate / destroy instances and read frames out
/// through the AVCaptureVideoDataOutputSampleBufferDelegate.
///
/// Compiled in only when `INCLUDE_CAMERA_USAGE` is defined (i.e. the app
/// has `NSCameraUsageDescription` set; `AiDependencyTable` does this
/// automatically when any class in `com.codename1.camera.*` is referenced).
/// Works on iOS, iPadOS and Mac Catalyst -- AVFoundation has the same
/// surface across all three.
@interface CN1Camera : NSObject <AVCaptureVideoDataOutputSampleBufferDelegate,
                                  AVCapturePhotoCaptureDelegate,
                                  AVCaptureFileOutputRecordingDelegate>

@property (nonatomic, strong) AVCaptureSession *session;
@property (nonatomic, strong) AVCaptureDevice *device;
@property (nonatomic, strong) AVCaptureDeviceInput *input;
@property (nonatomic, strong) AVCaptureVideoDataOutput *videoDataOutput;
@property (nonatomic, strong) AVCapturePhotoOutput *photoOutput;
@property (nonatomic, strong) AVCaptureMovieFileOutput *movieOutput;
@property (nonatomic, strong) AVCaptureVideoPreviewLayer *previewLayer;
@property (nonatomic, strong) UIView *previewView;
@property (nonatomic, strong) dispatch_queue_t videoQueue;
@property (nonatomic, assign) BOOL frameDeliveryEnabled;
@property (nonatomic, assign) int frameMaxFps;
@property (nonatomic, assign) NSTimeInterval lastFrameTime;
@property (nonatomic, assign) BOOL frameInFlight;
@property (nonatomic, assign) int pendingVideoCallback;
@property (nonatomic, strong) NSString *pendingVideoPath;

- (BOOL)openWithCameraId:(NSString *)cameraId
                previewW:(int)previewW
                previewH:(int)previewH
            captureAudio:(BOOL)captureAudio;
- (UIView *)createPreviewView;
- (void)takePhotoWithWidth:(int)width height:(int)height
                   quality:(int)quality filePath:(NSString *)filePath
                callbackId:(int)callbackId;
- (BOOL)startVideoToPath:(NSString *)path captureAudio:(BOOL)audio;
- (void)stopVideoWithCallback:(int)callbackId;
- (void)setFrameDeliveryEnabled:(BOOL)enabled maxFps:(int)maxFps;
- (void)setFlashMode:(int)mode;
- (void)setZoomRatio:(float)ratio;
- (void)focusAtX:(float)xNorm y:(float)yNorm;
- (void)pauseSession;
- (void)resumeSession;
- (void)closeSession;

+ (NSString *)enumerateCameras;
+ (BOOL)hasCameraSupport;

@end

#endif // INCLUDE_CAMERA_USAGE
