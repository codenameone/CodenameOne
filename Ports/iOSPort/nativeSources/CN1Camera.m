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

#import "CN1Camera.h"
#import "xmlvm.h"

#ifdef INCLUDE_CN1_CAMERA
#import "java_lang_String.h"
#import "com_codename1_impl_ios_IOSCameraImpl.h"

// Defined in IOSNative.m; converts an NSData into a Java byte[]. It pulls the
// thread state via getThreadLocalData() internally, so it takes no thread arg.
extern JAVA_OBJECT nsDataToByteArr(NSData *data);

// A preview host whose capture layer always fills its bounds. The simulator
// (and device peer layout) resizes the host view after creation; a bare
// CALayer does not autoresize with its view, so without this the preview
// stays pinned at its initial frame and the view shows only its black
// background.
@interface CN1CameraPreviewView : UIView
@property (nonatomic, readonly) AVCaptureVideoPreviewLayer *previewLayer;
@end

@implementation CN1CameraPreviewView
// Make the capture layer the view's OWN backing layer: it then resizes with
// the view automatically, no layoutSubviews needed (the simulator floats this
// view and never drives a normal UIKit layout pass, so layoutSubviews was
// never called and the preview stayed black).
+ (Class)layerClass { return [AVCaptureVideoPreviewLayer class]; }
- (AVCaptureVideoPreviewLayer *)previewLayer {
    return (AVCaptureVideoPreviewLayer *)self.layer;
}
@end

@interface CN1Camera ()
@property (nonatomic, copy) NSString *pendingPhotoFilePath;
@property (nonatomic, assign) int pendingPhotoCallbackId;
@property (nonatomic, assign) int pendingPhotoQuality;
@end

@implementation CN1Camera

#pragma mark - Lifecycle

- (instancetype)init {
    self = [super init];
    if (self) {
        _frameMaxFps = 15;
        _videoQueue = dispatch_queue_create("com.codename1.camera.video",
                                            DISPATCH_QUEUE_SERIAL);
    }
    return self;
}

#pragma mark - Enumeration

+ (NSString *)enumerateCameras {
    NSMutableArray *parts = [NSMutableArray array];
    NSArray<AVCaptureDevice *> *devs;
    if (@available(iOS 10.0, *)) {
        AVCaptureDeviceDiscoverySession *sess =
            [AVCaptureDeviceDiscoverySession discoverySessionWithDeviceTypes:
                @[ AVCaptureDeviceTypeBuiltInWideAngleCamera ]
                                                                 mediaType:AVMediaTypeVideo
                                                                  position:AVCaptureDevicePositionUnspecified];
        devs = sess.devices;
    } else {
        devs = [AVCaptureDevice devicesWithMediaType:AVMediaTypeVideo];
    }
    for (AVCaptureDevice *d in devs) {
        NSString *facing = (d.position == AVCaptureDevicePositionFront) ? @"front"
                         : (d.position == AVCaptureDevicePositionBack)  ? @"back"
                         : @"external";
        NSString *flash = d.hasFlash ? @"1" : @"0";
        NSString *focus = d.isFocusPointOfInterestSupported ? @"1" : @"0";
        [parts addObject:[NSString stringWithFormat:@"%@|%@|%@|%@",
                          d.uniqueID, facing, flash, focus]];
    }
    return [parts componentsJoinedByString:@";"];
}

+ (BOOL)hasCameraSupport {
    return [UIImagePickerController isSourceTypeAvailable:UIImagePickerControllerSourceTypeCamera];
}

#pragma mark - Open / close

- (BOOL)openWithCameraId:(NSString *)cameraId
                previewW:(int)previewW
                previewH:(int)previewH
            captureAudio:(BOOL)captureAudio {
    AVCaptureDevice *dev = nil;
    if (cameraId.length > 0) {
        dev = [AVCaptureDevice deviceWithUniqueID:cameraId];
    }
    if (!dev) {
        dev = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
    }
    if (!dev) return NO;
    self.device = dev;

    NSError *err = nil;
    AVCaptureDeviceInput *vin = [AVCaptureDeviceInput deviceInputWithDevice:dev error:&err];
    if (!vin) return NO;
    self.input = vin;

    self.session = [[AVCaptureSession alloc] init];
    [self.session beginConfiguration];

    // Preset selection: prefer 1280x720 for previewW > 800, 640x480 otherwise.
    if (previewW >= 1280 && [self.session canSetSessionPreset:AVCaptureSessionPreset1280x720]) {
        self.session.sessionPreset = AVCaptureSessionPreset1280x720;
    } else if ([self.session canSetSessionPreset:AVCaptureSessionPreset640x480]) {
        self.session.sessionPreset = AVCaptureSessionPreset640x480;
    } else {
        self.session.sessionPreset = AVCaptureSessionPresetMedium;
    }

    if ([self.session canAddInput:vin]) {
        [self.session addInput:vin];
    } else {
        [self.session commitConfiguration];
        return NO;
    }

    if (captureAudio) {
        AVCaptureDevice *mic = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeAudio];
        if (mic) {
            AVCaptureDeviceInput *ain = [AVCaptureDeviceInput deviceInputWithDevice:mic error:nil];
            if (ain && [self.session canAddInput:ain]) {
                [self.session addInput:ain];
            }
        }
    }

    self.videoDataOutput = [[AVCaptureVideoDataOutput alloc] init];
    self.videoDataOutput.alwaysDiscardsLateVideoFrames = YES;
    self.videoDataOutput.videoSettings = @{
        (id)kCVPixelBufferPixelFormatTypeKey :
            @(kCVPixelFormatType_32BGRA)
    };
    [self.videoDataOutput setSampleBufferDelegate:self queue:self.videoQueue];
    if ([self.session canAddOutput:self.videoDataOutput]) {
        [self.session addOutput:self.videoDataOutput];
    }

    self.photoOutput = [[AVCapturePhotoOutput alloc] init];
    if ([self.session canAddOutput:self.photoOutput]) {
        [self.session addOutput:self.photoOutput];
    }

    self.movieOutput = [[AVCaptureMovieFileOutput alloc] init];
    if ([self.session canAddOutput:self.movieOutput]) {
        [self.session addOutput:self.movieOutput];
    }

    [self.session commitConfiguration];
    [self.session startRunning];
    return YES;
}

- (UIView *)createPreviewView {
    if (self.previewView) return self.previewView;
    CN1CameraPreviewView *v =
        [[CN1CameraPreviewView alloc] initWithFrame:CGRectMake(0, 0, 320, 480)];
    v.backgroundColor = [UIColor blackColor];
    // v.layer IS the AVCaptureVideoPreviewLayer (see +layerClass)
    AVCaptureVideoPreviewLayer *layer = v.previewLayer;
    layer.session = self.session;
    layer.videoGravity = AVLayerVideoGravityResizeAspectFill;
    self.previewLayer = layer;
    self.previewView = v;
    return v;
}

- (void)pauseSession   { if (self.session.isRunning)  [self.session stopRunning]; }
- (void)resumeSession  { if (!self.session.isRunning) [self.session startRunning]; }

- (void)closeSession {
    if (self.session.isRunning) [self.session stopRunning];
    if (self.input) [self.session removeInput:self.input];
    if (self.videoDataOutput) [self.session removeOutput:self.videoDataOutput];
    if (self.photoOutput) [self.session removeOutput:self.photoOutput];
    if (self.movieOutput) [self.session removeOutput:self.movieOutput];
    self.previewLayer = nil;
    self.previewView = nil;
    self.input = nil;
    self.device = nil;
    self.videoDataOutput = nil;
    self.photoOutput = nil;
    self.movieOutput = nil;
    self.session = nil;
}

#pragma mark - Photo

- (void)takePhotoWithWidth:(int)width height:(int)height
                   quality:(int)quality filePath:(NSString *)filePath
                callbackId:(int)callbackId {
    self.pendingPhotoFilePath = filePath.length > 0 ? filePath : nil;
    self.pendingPhotoCallbackId = callbackId;
    self.pendingPhotoQuality = quality;

    AVCapturePhotoSettings *settings = [AVCapturePhotoSettings photoSettings];
    if (self.device.hasFlash && self.photoOutput.supportedFlashModes.count > 0) {
        settings.flashMode = AVCaptureFlashModeAuto;
    }
    [self.photoOutput capturePhotoWithSettings:settings delegate:self];
}

- (void)captureOutput:(AVCapturePhotoOutput *)output
didFinishProcessingPhoto:(AVCapturePhoto *)photo
                error:(NSError *)error API_AVAILABLE(ios(11.0)) {
    int cbId = self.pendingPhotoCallbackId;
    self.pendingPhotoCallbackId = 0;
    if (error) {
        NSString *msg = error.localizedDescription;
        [self firePhotoFailed:cbId withMessage:msg];
        return;
    }
    NSData *jpeg = [photo fileDataRepresentation];
    if (!jpeg) {
        [self firePhotoFailed:cbId withMessage:@"No data from AVCapturePhoto"];
        return;
    }
    UIImage *img = [UIImage imageWithData:jpeg];
    int w = (int)img.size.width;
    int h = (int)img.size.height;
    NSString *path = self.pendingPhotoFilePath;
    if (!path) {
        NSString *dir = NSTemporaryDirectory();
        path = [dir stringByAppendingPathComponent:
                [NSString stringWithFormat:@"cn1_photo_%llu.jpg",
                                  (unsigned long long)([[NSDate date] timeIntervalSince1970] * 1000)]];
    }
    [jpeg writeToFile:path atomically:YES];
    self.pendingPhotoFilePath = nil;
    [self firePhotoCaptured:cbId bytes:jpeg path:path width:w height:h];
}

#pragma mark - Video

- (BOOL)startVideoToPath:(NSString *)path captureAudio:(BOOL)audio {
    if (self.movieOutput.isRecording) return NO;
    // Pre-empt any prior file at this path.
    NSFileManager *fm = [NSFileManager defaultManager];
    if ([fm fileExistsAtPath:path]) {
        [fm removeItemAtPath:path error:nil];
    }
    // AVCaptureMovieFileOutput writes .mov; honor whatever extension caller supplied.
    NSURL *url = [NSURL fileURLWithPath:path];
    self.pendingVideoPath = path;
    [self.movieOutput startRecordingToOutputFileURL:url recordingDelegate:self];
    return YES;
}

- (void)stopVideoWithCallback:(int)callbackId {
    self.pendingVideoCallback = callbackId;
    if (self.movieOutput.isRecording) {
        [self.movieOutput stopRecording];
    } else {
        // No active recording -- still notify the callback with the last
        // known path so Java's AsyncResource completes.
        [self fireVideoStopped:callbackId path:self.pendingVideoPath];
    }
}

- (void)captureOutput:(AVCaptureFileOutput *)output
didFinishRecordingToOutputFileAtURL:(NSURL *)outputFileURL
       fromConnections:(NSArray<AVCaptureConnection *> *)connections
                error:(NSError *)error {
    NSString *path = outputFileURL.path;
    int cbId = self.pendingVideoCallback;
    self.pendingVideoCallback = 0;
    self.pendingVideoPath = nil;
    [self fireVideoStopped:cbId path:path];
}

#pragma mark - Frame delivery

- (void)setFrameDeliveryEnabled:(BOOL)enabled maxFps:(int)maxFps {
    self.frameDeliveryEnabled = enabled;
    if (maxFps > 0) self.frameMaxFps = maxFps;
}

- (void)captureOutput:(AVCaptureOutput *)output
didOutputSampleBuffer:(CMSampleBufferRef)sampleBuffer
       fromConnection:(AVCaptureConnection *)connection {
    if (!self.frameDeliveryEnabled) return;
    if (self.frameInFlight) return;
    NSTimeInterval now = CACurrentMediaTime();
    NSTimeInterval minDelta = 1.0 / MAX(1, self.frameMaxFps);
    if ((now - self.lastFrameTime) < minDelta) return;
    self.lastFrameTime = now;

    CVImageBufferRef pix = CMSampleBufferGetImageBuffer(sampleBuffer);
    if (!pix) return;
    CIImage *ci = [CIImage imageWithCVPixelBuffer:pix];
    CGRect rect = ci.extent;
    int w = (int)rect.size.width;
    int h = (int)rect.size.height;

    static CIContext *ctx;
    if (!ctx) ctx = [CIContext contextWithOptions:nil];
    CGImageRef cg = [ctx createCGImage:ci fromRect:rect];
    if (!cg) return;
    UIImage *img = [UIImage imageWithCGImage:cg];
    CGImageRelease(cg);
    NSData *jpeg = UIImageJPEGRepresentation(img, 0.8);
    if (!jpeg) return;

    int rotation = 0;
    if (connection.isVideoOrientationSupported) {
        switch (connection.videoOrientation) {
            case AVCaptureVideoOrientationPortrait:           rotation = 90;  break;
            case AVCaptureVideoOrientationPortraitUpsideDown: rotation = 270; break;
            case AVCaptureVideoOrientationLandscapeLeft:      rotation = 180; break;
            case AVCaptureVideoOrientationLandscapeRight:     rotation = 0;   break;
        }
    }

    int64_t ts = (int64_t)(now * 1e9);
    self.frameInFlight = YES;
    [self fireFrame:jpeg width:w height:h rotation:rotation timestamp:ts];
    self.frameInFlight = NO;
}

#pragma mark - Flash / zoom / focus

- (void)setFlashMode:(int)mode {
    if (!self.device.hasTorch) return;
    if (![self.device lockForConfiguration:nil]) return;
    switch (mode) {
        case 0: self.device.torchMode = AVCaptureTorchModeOff;  break;
        case 1: self.device.torchMode = AVCaptureTorchModeOn;   break;
        case 2: self.device.torchMode = AVCaptureTorchModeAuto; break;
        case 3:
            if ([self.device isTorchModeSupported:AVCaptureTorchModeOn]) {
                [self.device setTorchModeOnWithLevel:1.0 error:nil];
            }
            break;
    }
    [self.device unlockForConfiguration];
}

- (void)setZoomRatio:(float)ratio {
    if (!self.device) return;
    if (![self.device lockForConfiguration:nil]) return;
    CGFloat r = MAX(1.0, MIN((CGFloat)ratio, self.device.activeFormat.videoMaxZoomFactor));
    self.device.videoZoomFactor = r;
    [self.device unlockForConfiguration];
}

- (void)focusAtX:(float)xNorm y:(float)yNorm {
    if (!self.device.isFocusPointOfInterestSupported) return;
    if (![self.device lockForConfiguration:nil]) return;
    CGPoint point = CGPointMake(xNorm, yNorm);
    self.device.focusPointOfInterest = point;
    if ([self.device isFocusModeSupported:AVCaptureFocusModeAutoFocus]) {
        self.device.focusMode = AVCaptureFocusModeAutoFocus;
    }
    if (self.device.isExposurePointOfInterestSupported) {
        self.device.exposurePointOfInterest = point;
        if ([self.device isExposureModeSupported:AVCaptureExposureModeAutoExpose]) {
            self.device.exposureMode = AVCaptureExposureModeAutoExpose;
        }
    }
    [self.device unlockForConfiguration];
}

#pragma mark - Java callback bridging

- (void)fireFrame:(NSData *)jpeg width:(int)w height:(int)h
         rotation:(int)rotation timestamp:(int64_t)ts {
    JAVA_OBJECT bytes = nsDataToByteArr(jpeg);
    com_codename1_impl_ios_IOSCameraImpl_onFrameDelivered___byte_1ARRAY_int_int_int_long(
        CN1_THREAD_GET_STATE_PASS_ARG bytes, w, h, rotation, (JAVA_LONG)ts);
}

- (void)firePhotoCaptured:(int)cbId bytes:(NSData *)jpeg path:(NSString *)path
                    width:(int)w height:(int)h {
    JAVA_OBJECT b = nsDataToByteArr(jpeg);
    JAVA_OBJECT p = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG path);
    com_codename1_impl_ios_IOSCameraImpl_onPhotoCaptured___int_byte_1ARRAY_java_lang_String_int_int(
        CN1_THREAD_GET_STATE_PASS_ARG cbId, b, p, w, h);
}

- (void)firePhotoFailed:(int)cbId withMessage:(NSString *)msg {
    JAVA_OBJECT m = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG msg);
    com_codename1_impl_ios_IOSCameraImpl_onPhotoFailed___int_java_lang_String(
        CN1_THREAD_GET_STATE_PASS_ARG cbId, m);
}

- (void)fireVideoStopped:(int)cbId path:(NSString *)path {
    if (cbId == 0) return;
    JAVA_OBJECT p = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG path);
    com_codename1_impl_ios_IOSCameraImpl_onVideoStopped___int_java_lang_String(
        CN1_THREAD_GET_STATE_PASS_ARG cbId, p);
}

@end

#endif // INCLUDE_CN1_CAMERA

#pragma mark - IOSNative bridge functions
// Each `native void cn1Camera*` declared on IOSNative.java has a matching
// C function below. Naming follows the standard ParparVM mangling:
//   com_codename1_impl_ios_IOSNative_<methodName>___<argType>_<argType>
// Sessions are referenced by their CN1Camera Objective-C pointer cast to
// JAVA_LONG; we retain in open* and release in close. When camera support
// is compiled out (INCLUDE_CN1_CAMERA undefined) the bridge symbols
// still exist so ParparVM links cleanly -- they just return null/0 and
// the Java side reports the platform as unsupported.

JAVA_OBJECT com_codename1_impl_ios_IOSNative_cn1CameraEnumerate___R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#ifdef INCLUDE_CN1_CAMERA
    NSString *s = [CN1Camera enumerateCameras];
    return fromNSString(CN1_THREAD_GET_STATE_PASS_ARG s);
#else
    return JAVA_NULL;
#endif
}

JAVA_LONG com_codename1_impl_ios_IOSNative_cn1CameraOpen___java_lang_String_int_int_boolean_R_long(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject,
        JAVA_OBJECT cameraId, JAVA_INT previewW, JAVA_INT previewH,
        JAVA_BOOLEAN captureAudio) {
#ifdef INCLUDE_CN1_CAMERA
    NSString *idStr = toNSString(CN1_THREAD_GET_STATE_PASS_ARG cameraId);
    CN1Camera *cam = [[CN1Camera alloc] init];
    BOOL ok = [cam openWithCameraId:idStr previewW:previewW previewH:previewH
                       captureAudio:captureAudio];
    if (!ok) return 0;
#ifndef CN1_USE_ARC
    return (JAVA_LONG)[cam retain];
#else
    return (JAVA_LONG)(__bridge_retained void *)cam;
#endif
#else
    return 0;
#endif
}

JAVA_LONG com_codename1_impl_ios_IOSNative_cn1CameraCreatePreviewView___long_R_long(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG sessionPeer) {
#ifdef INCLUDE_CN1_CAMERA
#ifndef CN1_USE_ARC
    CN1Camera *cam = (CN1Camera *)sessionPeer;
#else
    CN1Camera *cam = (__bridge CN1Camera *)(void *)sessionPeer;
#endif
    UIView *v = [cam createPreviewView];
#ifndef CN1_USE_ARC
    return (JAVA_LONG)[v retain];
#else
    return (JAVA_LONG)(__bridge_retained void *)v;
#endif
#else
    return 0;
#endif
}

void com_codename1_impl_ios_IOSNative_cn1CameraTakePhoto___long_int_int_int_java_lang_String_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG sessionPeer,
        JAVA_INT width, JAVA_INT height, JAVA_INT quality,
        JAVA_OBJECT filePath, JAVA_INT callbackId) {
#ifdef INCLUDE_CN1_CAMERA
#ifndef CN1_USE_ARC
    CN1Camera *cam = (CN1Camera *)sessionPeer;
#else
    CN1Camera *cam = (__bridge CN1Camera *)(void *)sessionPeer;
#endif
    NSString *path = toNSString(CN1_THREAD_GET_STATE_PASS_ARG filePath);
    dispatch_async(dispatch_get_main_queue(), ^{
        [cam takePhotoWithWidth:width height:height quality:quality
                       filePath:path callbackId:callbackId];
    });
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_cn1CameraStartVideo___long_java_lang_String_boolean_R_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG sessionPeer,
        JAVA_OBJECT filePath, JAVA_BOOLEAN captureAudio) {
#ifdef INCLUDE_CN1_CAMERA
#ifndef CN1_USE_ARC
    CN1Camera *cam = (CN1Camera *)sessionPeer;
#else
    CN1Camera *cam = (__bridge CN1Camera *)(void *)sessionPeer;
#endif
    NSString *path = toNSString(CN1_THREAD_GET_STATE_PASS_ARG filePath);
    __block BOOL result = NO;
    dispatch_sync(dispatch_get_main_queue(), ^{
        result = [cam startVideoToPath:path captureAudio:captureAudio];
    });
    return result;
#else
    return JAVA_FALSE;
#endif
}

void com_codename1_impl_ios_IOSNative_cn1CameraStopVideo___long_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject,
        JAVA_LONG sessionPeer, JAVA_INT callbackId) {
#ifdef INCLUDE_CN1_CAMERA
#ifndef CN1_USE_ARC
    CN1Camera *cam = (CN1Camera *)sessionPeer;
#else
    CN1Camera *cam = (__bridge CN1Camera *)(void *)sessionPeer;
#endif
    dispatch_async(dispatch_get_main_queue(), ^{
        [cam stopVideoWithCallback:callbackId];
    });
#endif
}

void com_codename1_impl_ios_IOSNative_cn1CameraSetFrameDelivery___long_boolean_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject,
        JAVA_LONG sessionPeer, JAVA_BOOLEAN enabled, JAVA_INT maxFps) {
#ifdef INCLUDE_CN1_CAMERA
#ifndef CN1_USE_ARC
    CN1Camera *cam = (CN1Camera *)sessionPeer;
#else
    CN1Camera *cam = (__bridge CN1Camera *)(void *)sessionPeer;
#endif
    [cam setFrameDeliveryEnabled:enabled maxFps:maxFps];
#endif
}

void com_codename1_impl_ios_IOSNative_cn1CameraSetFlash___long_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject,
        JAVA_LONG sessionPeer, JAVA_INT mode) {
#ifdef INCLUDE_CN1_CAMERA
#ifndef CN1_USE_ARC
    CN1Camera *cam = (CN1Camera *)sessionPeer;
#else
    CN1Camera *cam = (__bridge CN1Camera *)(void *)sessionPeer;
#endif
    dispatch_async(dispatch_get_main_queue(), ^{ [cam setFlashMode:mode]; });
#endif
}

void com_codename1_impl_ios_IOSNative_cn1CameraSetZoom___long_float(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject,
        JAVA_LONG sessionPeer, JAVA_FLOAT ratio) {
#ifdef INCLUDE_CN1_CAMERA
#ifndef CN1_USE_ARC
    CN1Camera *cam = (CN1Camera *)sessionPeer;
#else
    CN1Camera *cam = (__bridge CN1Camera *)(void *)sessionPeer;
#endif
    dispatch_async(dispatch_get_main_queue(), ^{ [cam setZoomRatio:ratio]; });
#endif
}

void com_codename1_impl_ios_IOSNative_cn1CameraFocus___long_float_float(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject,
        JAVA_LONG sessionPeer, JAVA_FLOAT xNorm, JAVA_FLOAT yNorm) {
#ifdef INCLUDE_CN1_CAMERA
#ifndef CN1_USE_ARC
    CN1Camera *cam = (CN1Camera *)sessionPeer;
#else
    CN1Camera *cam = (__bridge CN1Camera *)(void *)sessionPeer;
#endif
    dispatch_async(dispatch_get_main_queue(), ^{ [cam focusAtX:xNorm y:yNorm]; });
#endif
}

void com_codename1_impl_ios_IOSNative_cn1CameraPause___long(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG sessionPeer) {
#ifdef INCLUDE_CN1_CAMERA
#ifndef CN1_USE_ARC
    CN1Camera *cam = (CN1Camera *)sessionPeer;
#else
    CN1Camera *cam = (__bridge CN1Camera *)(void *)sessionPeer;
#endif
    dispatch_async(dispatch_get_main_queue(), ^{ [cam pauseSession]; });
#endif
}

void com_codename1_impl_ios_IOSNative_cn1CameraResume___long(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG sessionPeer) {
#ifdef INCLUDE_CN1_CAMERA
#ifndef CN1_USE_ARC
    CN1Camera *cam = (CN1Camera *)sessionPeer;
#else
    CN1Camera *cam = (__bridge CN1Camera *)(void *)sessionPeer;
#endif
    dispatch_async(dispatch_get_main_queue(), ^{ [cam resumeSession]; });
#endif
}

void com_codename1_impl_ios_IOSNative_cn1CameraClose___long(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG sessionPeer) {
    if (sessionPeer == 0) return;
#ifdef INCLUDE_CN1_CAMERA
#ifndef CN1_USE_ARC
    CN1Camera *cam = (CN1Camera *)sessionPeer;
    dispatch_async(dispatch_get_main_queue(), ^{
        [cam closeSession];
        [cam release];
    });
#else
    CN1Camera *cam = (__bridge_transfer CN1Camera *)(void *)sessionPeer;
    dispatch_async(dispatch_get_main_queue(), ^{
        [cam closeSession];
    });
#endif
#endif
}
