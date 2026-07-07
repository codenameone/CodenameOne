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

#import "CN1AR.h"
#import "xmlvm.h"

#if defined(INCLUDE_CN1_AR) && !TARGET_OS_TV && !TARGET_OS_WATCH
#import <simd/simd.h>
#import "java_lang_String.h"
#import "com_codename1_impl_ios_IOSARImpl.h"

// Tracking state / failure reason codes shared with IOSARImpl.java.
#define CN1AR_STATE_NOT_TRACKING 0
#define CN1AR_STATE_LIMITED 1
#define CN1AR_STATE_TRACKING 2
#define CN1AR_REASON_NONE 0
#define CN1AR_REASON_INITIALIZING 1
#define CN1AR_REASON_EXCESSIVE_MOTION 2
#define CN1AR_REASON_INSUFFICIENT_LIGHT 3
#define CN1AR_REASON_INSUFFICIENT_FEATURES 4

// Extracts translation + rotation quaternion from a rigid transform into a
// 7-float pose (tx ty tz qx qy qz qw).
static void cn1arPose(simd_float4x4 m, float *out7) {
    out7[0] = m.columns[3].x;
    out7[1] = m.columns[3].y;
    out7[2] = m.columns[3].z;
    simd_quatf q = simd_quaternion(m);
    out7[3] = q.vector.x;
    out7[4] = q.vector.y;
    out7[5] = q.vector.z;
    out7[6] = q.vector.w;
}

static void cn1arRunOnMain(void (^block)(void)) {
    if ([NSThread isMainThread]) {
        block();
    } else {
        dispatch_sync(dispatch_get_main_queue(), block);
    }
}

@implementation CN1AR

- (instancetype)init {
    self = [super init];
    if (self) {
        _referenceImages = [NSMutableArray array];
        _anchorNodes = [NSMutableDictionary dictionary];
        _contentNodes = [NSMutableDictionary dictionary];
        _recentHits = [NSMutableDictionary dictionary];
        _nextHitId = 1;
        cn1arRunOnMain(^{
            self.sceneView = [[ARSCNView alloc] initWithFrame:CGRectMake(0, 0, 320, 480)];
            self.sceneView.delegate = self;
            self.sceneView.session.delegate = self;
            self.sceneView.automaticallyUpdatesLighting = YES;
            self.sceneView.autoenablesDefaultLighting = YES;
        });
    }
    return self;
}

+ (BOOL)isConfigSupported:(int)configType {
    if (configType == 1) {
        return [ARFaceTrackingConfiguration isSupported];
    }
    return [ARWorldTrackingConfiguration isSupported];
}

- (void)addReferenceImage:(NSData *)encoded name:(NSString *)name width:(float)widthMeters {
    UIImage *img = [UIImage imageWithData:encoded];
    if (img == nil || img.CGImage == nil) {
        return;
    }
    ARReferenceImage *ref = [[ARReferenceImage alloc]
            initWithCGImage:img.CGImage
                orientation:kCGImagePropertyOrientationUp
              physicalWidth:widthMeters];
    ref.name = name;
    [self.referenceImages addObject:ref];
}

- (BOOL)startWithConfigType:(int)configType planeMask:(int)planeMask
            lightEstimation:(BOOL)lightEstimation {
    if (![CN1AR isConfigSupported:configType]) {
        return NO;
    }
    self.configType = configType;
    self.planeMask = planeMask;
    self.lightEstimation = lightEstimation;
    cn1arRunOnMain(^{
        [self runConfigurationWithReset:YES];
    });
    return YES;
}

- (void)runConfigurationWithReset:(BOOL)reset {
    ARConfiguration *config;
    if (self.configType == 1) {
        ARFaceTrackingConfiguration *face = [[ARFaceTrackingConfiguration alloc] init];
        config = face;
    } else {
        ARWorldTrackingConfiguration *world = [[ARWorldTrackingConfiguration alloc] init];
        ARPlaneDetection detection = ARPlaneDetectionNone;
        if (self.planeMask & 1) {
            detection |= ARPlaneDetectionHorizontal;
        }
        if (self.planeMask & 2) {
            detection |= ARPlaneDetectionVertical;
        }
        world.planeDetection = detection;
        if (self.referenceImages.count > 0) {
            world.detectionImages = [NSSet setWithArray:self.referenceImages];
            if (@available(iOS 12.0, *)) {
                world.maximumNumberOfTrackedImages = (NSInteger) self.referenceImages.count;
            }
        }
        config = world;
    }
    config.lightEstimationEnabled = self.lightEstimation;
    ARSessionRunOptions options = reset
            ? (ARSessionRunOptionResetTracking | ARSessionRunOptionRemoveExistingAnchors)
            : 0;
    [self.sceneView.session runWithConfiguration:config options:options];
}

- (UIView *)createView {
    return self.sceneView;
}

- (NSString *)hitTestX:(float)xNorm y:(float)yNorm {
    NSMutableString * __block out = [NSMutableString string];
    cn1arRunOnMain(^{
        [self.recentHits removeAllObjects];
        CGSize size = self.sceneView.bounds.size;
        CGPoint point = CGPointMake(xNorm * size.width, yNorm * size.height);
        NSArray<ARRaycastResult *> *results = @[];
        if (@available(iOS 13.0, *)) {
            ARRaycastQuery *query = [self.sceneView
                    raycastQueryFromPoint:point
                         allowingTarget:ARRaycastTargetExistingPlaneGeometry
                              alignment:ARRaycastTargetAlignmentAny];
            if (query != nil) {
                results = [self.sceneView.session raycast:query];
            }
            if (results.count == 0) {
                query = [self.sceneView
                        raycastQueryFromPoint:point
                             allowingTarget:ARRaycastTargetEstimatedPlane
                                  alignment:ARRaycastTargetAlignmentAny];
                if (query != nil) {
                    results = [self.sceneView.session raycast:query];
                }
            }
        }
        simd_float4x4 camTransform = self.sceneView.session.currentFrame != nil
                ? self.sceneView.session.currentFrame.camera.transform
                : matrix_identity_float4x4;
        simd_float3 camPos = simd_make_float3(camTransform.columns[3].x,
                camTransform.columns[3].y, camTransform.columns[3].z);
        for (ARRaycastResult *r in results) {
            int hitId = self.nextHitId++;
            self.recentHits[@(hitId)] = r;
            float pose[7];
            cn1arPose(r.worldTransform, pose);
            simd_float3 hitPos = simd_make_float3(pose[0], pose[1], pose[2]);
            float distance = simd_distance(camPos, hitPos);
            int type = r.target == ARRaycastTargetExistingPlaneGeometry ? 0 : 1;
            NSString *planeId = @"";
            if ([r.anchor isKindOfClass:[ARPlaneAnchor class]]) {
                planeId = r.anchor.identifier.UUIDString;
                type = 0;
            }
            if (out.length > 0) {
                [out appendString:@";"];
            }
            [out appendFormat:@"%d|%d|%f|%f|%f|%f|%f|%f|%f|%f|%@",
                    hitId, type, pose[0], pose[1], pose[2],
                    pose[3], pose[4], pose[5], pose[6], distance, planeId];
        }
    });
    return out;
}

- (NSString *)createAnchorTx:(float)tx ty:(float)ty tz:(float)tz
                          qx:(float)qx qy:(float)qy qz:(float)qz qw:(float)qw {
    simd_quatf q = simd_quaternion(qx, qy, qz, qw);
    simd_float4x4 m = simd_matrix4x4(q);
    m.columns[3] = simd_make_float4(tx, ty, tz, 1);
    ARAnchor *anchor = [[ARAnchor alloc] initWithTransform:m];
    NSString * __block anchorId = anchor.identifier.UUIDString;
    cn1arRunOnMain(^{
        [self.sceneView.session addAnchor:anchor];
    });
    return anchorId;
}

- (NSString *)createAnchorFromHit:(int)hitId {
    ARRaycastResult * __block result = nil;
    NSString * __block anchorId = nil;
    cn1arRunOnMain(^{
        result = self.recentHits[@(hitId)];
        if (result != nil) {
            ARAnchor *anchor = [[ARAnchor alloc] initWithTransform:result.worldTransform];
            anchorId = anchor.identifier.UUIDString;
            [self.sceneView.session addAnchor:anchor];
        }
    });
    return anchorId;
}

- (ARAnchor *)findAnchorById:(NSString *)anchorId {
    ARFrame *frame = self.sceneView.session.currentFrame;
    for (ARAnchor *a in frame.anchors) {
        if ([a.identifier.UUIDString isEqualToString:anchorId]) {
            return a;
        }
    }
    return nil;
}

- (void)removeAnchorById:(NSString *)anchorId {
    cn1arRunOnMain(^{
        ARAnchor *a = [self findAnchorById:anchorId];
        if (a != nil) {
            [self.sceneView.session removeAnchor:a];
        }
        [self.contentNodes removeObjectForKey:anchorId];
        [self.anchorNodes removeObjectForKey:anchorId];
    });
}

- (void)clearAnchorContent:(NSString *)anchorId {
    cn1arRunOnMain(^{
        SCNNode *content = self.contentNodes[anchorId];
        if (content != nil) {
            [content removeFromParentNode];
            [self.contentNodes removeObjectForKey:anchorId];
        }
    });
}

- (void)addAnchorMesh:(NSString *)anchorId
          interleaved:(const float *)interleaved
          vertexCount:(int)vertexCount
              indices:(const int *)indices
           indexCount:(int)indexCount
                 argb:(int)argb
       encodedTexture:(NSData *)encodedTexture
       localTransform:(const float *)localTransform16 {
    // Interleaved layout: position(3) + normal(3) + texcoord(2), 32-byte
    // stride -- the VertexFormat.POSITION_NORMAL_TEXCOORD convention every
    // com.codename1.gpu mesh producer follows.
    int strideBytes = 8 * sizeof(float);
    NSData *vertexData = [NSData dataWithBytes:interleaved
                                        length:(NSUInteger) vertexCount * strideBytes];
    SCNGeometrySource *positions = [SCNGeometrySource
            geometrySourceWithData:vertexData
                          semantic:SCNGeometrySourceSemanticVertex
                       vectorCount:vertexCount
                   floatComponents:YES
               componentsPerVector:3
                 bytesPerComponent:sizeof(float)
                        dataOffset:0
                        dataStride:strideBytes];
    SCNGeometrySource *normals = [SCNGeometrySource
            geometrySourceWithData:vertexData
                          semantic:SCNGeometrySourceSemanticNormal
                       vectorCount:vertexCount
                   floatComponents:YES
               componentsPerVector:3
                 bytesPerComponent:sizeof(float)
                        dataOffset:3 * sizeof(float)
                        dataStride:strideBytes];
    SCNGeometrySource *uvs = [SCNGeometrySource
            geometrySourceWithData:vertexData
                          semantic:SCNGeometrySourceSemanticTexcoord
                       vectorCount:vertexCount
                   floatComponents:YES
               componentsPerVector:2
                 bytesPerComponent:sizeof(float)
                        dataOffset:6 * sizeof(float)
                        dataStride:strideBytes];
    NSData *indexData = [NSData dataWithBytes:indices
                                       length:(NSUInteger) indexCount * sizeof(int)];
    SCNGeometryElement *element = [SCNGeometryElement
            geometryElementWithData:indexData
                      primitiveType:SCNGeometryPrimitiveTypeTriangles
                     primitiveCount:indexCount / 3
                      bytesPerIndex:sizeof(int)];
    SCNGeometry *geometry = [SCNGeometry geometryWithSources:@[positions, normals, uvs]
                                                    elements:@[element]];
    SCNMaterial *material = [SCNMaterial material];
    material.lightingModelName = SCNLightingModelBlinn;
    if (encodedTexture != nil && encodedTexture.length > 0) {
        UIImage *tex = [UIImage imageWithData:encodedTexture];
        if (tex != nil) {
            material.diffuse.contents = tex;
        }
    } else {
        float a = ((argb >> 24) & 0xff) / 255.0f;
        float r = ((argb >> 16) & 0xff) / 255.0f;
        float g = ((argb >> 8) & 0xff) / 255.0f;
        float b = (argb & 0xff) / 255.0f;
        material.diffuse.contents = [UIColor colorWithRed:r green:g blue:b alpha:a];
    }
    material.doubleSided = YES;
    geometry.materials = @[material];

    SCNNode *meshNode = [SCNNode nodeWithGeometry:geometry];
    simd_float4x4 local;
    for (int c = 0; c < 4; c++) {
        local.columns[c] = simd_make_float4(localTransform16[c * 4],
                localTransform16[c * 4 + 1], localTransform16[c * 4 + 2],
                localTransform16[c * 4 + 3]);
    }
    meshNode.simdTransform = local;

    cn1arRunOnMain(^{
        SCNNode *content = self.contentNodes[anchorId];
        if (content == nil) {
            content = [SCNNode node];
            self.contentNodes[anchorId] = content;
            SCNNode *anchorNode = self.anchorNodes[anchorId];
            if (anchorNode != nil) {
                [anchorNode addChildNode:content];
            }
            // When the ARSCNView has not yet created the node for this
            // anchor, the content stays parked in contentNodes and is
            // attached from renderer:didAddNode:forAnchor:.
        }
        [content addChildNode:meshNode];
    });
}

- (void)pauseSession {
    cn1arRunOnMain(^{
        [self.sceneView.session pause];
    });
}

- (void)resumeSession {
    cn1arRunOnMain(^{
        [self runConfigurationWithReset:NO];
    });
}

- (void)closeSession {
    self.closed = YES;
    cn1arRunOnMain(^{
        [self.sceneView.session pause];
        self.sceneView.delegate = nil;
        self.sceneView.session.delegate = nil;
        [self.recentHits removeAllObjects];
        [self.contentNodes removeAllObjects];
        [self.anchorNodes removeAllObjects];
    });
}

#pragma mark - Pose helpers

- (int)planeTypeFor:(ARPlaneAnchor *)plane {
    if (plane.alignment == ARPlaneAnchorAlignmentVertical) {
        return 2;
    }
    // ARKit reports a single "horizontal" alignment; distinguish up/down
    // facing surfaces (table vs ceiling) from the plane's world Y axis.
    simd_float4 yAxis = plane.transform.columns[1];
    return yAxis.y >= 0 ? 0 : 1;
}

- (void)firePlaneEvent:(int)kind anchor:(ARPlaneAnchor *)plane {
    // The plane's center is expressed relative to the anchor transform.
    simd_float4x4 centerM = plane.transform;
    simd_float4 center = simd_make_float4(plane.center.x, plane.center.y, plane.center.z, 1);
    simd_float4 world = simd_mul(centerM, center);
    float pose[7];
    cn1arPose(centerM, pose);
    pose[0] = world.x;
    pose[1] = world.y;
    pose[2] = world.z;
    float extentX = plane.extent.x;
    float extentZ = plane.extent.z;
    JAVA_OBJECT planeId = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG plane.identifier.UUIDString);
    com_codename1_impl_ios_IOSARImpl_onPlaneEvent___int_java_lang_String_int_float_float_float_float_float_float_float_float_float(
            CN1_THREAD_GET_STATE_PASS_ARG kind, planeId, [self planeTypeFor:plane],
            pose[0], pose[1], pose[2], pose[3], pose[4], pose[5], pose[6],
            extentX, extentZ);
}

- (void)fireAnchorUpdated:(ARAnchor *)anchor {
    float pose[7];
    cn1arPose(anchor.transform, pose);
    int state = CN1AR_STATE_TRACKING;
    if ([anchor respondsToSelector:@selector(isTracked)]) {
        // ARImageAnchor / ARFaceAnchor expose isTracked.
        state = ((BOOL) [(id) anchor isTracked]) ? CN1AR_STATE_TRACKING
                : CN1AR_STATE_LIMITED;
    }
    JAVA_OBJECT anchorId = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG anchor.identifier.UUIDString);
    com_codename1_impl_ios_IOSARImpl_onAnchorUpdated___java_lang_String_float_float_float_float_float_float_float_int(
            CN1_THREAD_GET_STATE_PASS_ARG anchorId,
            pose[0], pose[1], pose[2], pose[3], pose[4], pose[5], pose[6], state);
}

- (void)fireFaceRegions:(ARFaceAnchor *)face {
    // World transforms for the eye regions; forehead/nose are not supplied
    // by ARKit so the Java side leaves those regions null.
    simd_float4x4 left = simd_mul(face.transform, face.leftEyeTransform);
    simd_float4x4 right = simd_mul(face.transform, face.rightEyeTransform);
    float lp[7];
    float rp[7];
    cn1arPose(left, lp);
    cn1arPose(right, rp);
    NSString *packed = [NSString stringWithFormat:
            @"%f,%f,%f,%f,%f,%f,%f;%f,%f,%f,%f,%f,%f,%f",
            lp[0], lp[1], lp[2], lp[3], lp[4], lp[5], lp[6],
            rp[0], rp[1], rp[2], rp[3], rp[4], rp[5], rp[6]];
    JAVA_OBJECT anchorId = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG face.identifier.UUIDString);
    JAVA_OBJECT packedJ = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG packed);
    com_codename1_impl_ios_IOSARImpl_onFaceRegionsUpdated___java_lang_String_java_lang_String(
            CN1_THREAD_GET_STATE_PASS_ARG anchorId, packedJ);
}

#pragma mark - ARSCNViewDelegate

- (void)renderer:(id<SCNSceneRenderer>)renderer didAddNode:(SCNNode *)node
       forAnchor:(ARAnchor *)anchor {
    if (self.closed) {
        return;
    }
    NSString *anchorId = anchor.identifier.UUIDString;
    dispatch_async(dispatch_get_main_queue(), ^{
        self.anchorNodes[anchorId] = node;
        SCNNode *content = self.contentNodes[anchorId];
        if (content != nil && content.parentNode == nil) {
            [node addChildNode:content];
        }
    });
    if ([anchor isKindOfClass:[ARPlaneAnchor class]]) {
        [self firePlaneEvent:0 anchor:(ARPlaneAnchor *) anchor];
    } else if ([anchor isKindOfClass:[ARImageAnchor class]]) {
        ARImageAnchor *img = (ARImageAnchor *) anchor;
        float pose[7];
        cn1arPose(img.transform, pose);
        JAVA_OBJECT idj = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG anchorId);
        JAVA_OBJECT namej = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG
                img.referenceImage.name == nil ? @"" : img.referenceImage.name);
        com_codename1_impl_ios_IOSARImpl_onImageAnchorAdded___java_lang_String_java_lang_String_float_float_float_float_float_float_float_float(
                CN1_THREAD_GET_STATE_PASS_ARG idj, namej,
                pose[0], pose[1], pose[2], pose[3], pose[4], pose[5], pose[6],
                (float) img.referenceImage.physicalSize.width);
    } else if ([anchor isKindOfClass:[ARFaceAnchor class]]) {
        float pose[7];
        cn1arPose(anchor.transform, pose);
        JAVA_OBJECT idj = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG anchorId);
        com_codename1_impl_ios_IOSARImpl_onFaceAnchorAdded___java_lang_String_float_float_float_float_float_float_float(
                CN1_THREAD_GET_STATE_PASS_ARG idj,
                pose[0], pose[1], pose[2], pose[3], pose[4], pose[5], pose[6]);
        [self fireFaceRegions:(ARFaceAnchor *) anchor];
    }
}

- (void)renderer:(id<SCNSceneRenderer>)renderer didUpdateNode:(SCNNode *)node
       forAnchor:(ARAnchor *)anchor {
    if (self.closed) {
        return;
    }
    if ([anchor isKindOfClass:[ARPlaneAnchor class]]) {
        [self firePlaneEvent:1 anchor:(ARPlaneAnchor *) anchor];
    } else {
        [self fireAnchorUpdated:anchor];
        if ([anchor isKindOfClass:[ARFaceAnchor class]]) {
            [self fireFaceRegions:(ARFaceAnchor *) anchor];
        }
    }
}

- (void)renderer:(id<SCNSceneRenderer>)renderer didRemoveNode:(SCNNode *)node
       forAnchor:(ARAnchor *)anchor {
    if (self.closed) {
        return;
    }
    NSString *anchorId = anchor.identifier.UUIDString;
    dispatch_async(dispatch_get_main_queue(), ^{
        [self.anchorNodes removeObjectForKey:anchorId];
        [self.contentNodes removeObjectForKey:anchorId];
    });
    if ([anchor isKindOfClass:[ARPlaneAnchor class]]) {
        [self firePlaneEvent:2 anchor:(ARPlaneAnchor *) anchor];
    } else {
        JAVA_OBJECT idj = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG anchorId);
        com_codename1_impl_ios_IOSARImpl_onAnchorRemoved___java_lang_String(
                CN1_THREAD_GET_STATE_PASS_ARG idj);
    }
}

#pragma mark - ARSessionDelegate

- (void)session:(ARSession *)session didUpdateFrame:(ARFrame *)frame {
    if (self.closed) {
        return;
    }
    // Throttle the per-frame pose + light delivery to roughly 10 Hz; the
    // Java side only caches the latest value for polling.
    self.frameCounter++;
    if (self.frameCounter % 6 != 0) {
        return;
    }
    float pose[7];
    cn1arPose(frame.camera.transform, pose);
    float intensity = 1.0f;
    float r = 1.0f;
    float g = 1.0f;
    float b = 1.0f;
    JAVA_BOOLEAN lightValid = 0;
    if (frame.lightEstimate != nil) {
        // ARKit reports about 1000 lumens in neutral lighting; normalize so
        // 1.0 is neutral (the ARLightEstimate convention).
        intensity = (float) (frame.lightEstimate.ambientIntensity / 1000.0);
        lightValid = 1;
    }
    com_codename1_impl_ios_IOSARImpl_onCameraFrame___float_float_float_float_float_float_float_float_float_float_float_boolean(
            CN1_THREAD_GET_STATE_PASS_ARG
            pose[0], pose[1], pose[2], pose[3], pose[4], pose[5], pose[6],
            intensity, r, g, b, lightValid);
}

- (void)session:(ARSession *)session cameraDidChangeTrackingState:(ARCamera *)camera {
    if (self.closed) {
        return;
    }
    int state;
    int reason = CN1AR_REASON_NONE;
    switch (camera.trackingState) {
        case ARTrackingStateNotAvailable:
            state = CN1AR_STATE_NOT_TRACKING;
            break;
        case ARTrackingStateLimited:
            state = CN1AR_STATE_LIMITED;
            switch (camera.trackingStateReason) {
                case ARTrackingStateReasonInitializing:
                case ARTrackingStateReasonRelocalizing:
                    reason = CN1AR_REASON_INITIALIZING;
                    break;
                case ARTrackingStateReasonExcessiveMotion:
                    reason = CN1AR_REASON_EXCESSIVE_MOTION;
                    break;
                case ARTrackingStateReasonInsufficientFeatures:
                    reason = CN1AR_REASON_INSUFFICIENT_FEATURES;
                    break;
                default:
                    reason = CN1AR_REASON_INITIALIZING;
                    break;
            }
            break;
        default:
            state = CN1AR_STATE_TRACKING;
            break;
    }
    com_codename1_impl_ios_IOSARImpl_onTrackingStateChanged___int_int(
            CN1_THREAD_GET_STATE_PASS_ARG state, reason);
}

@end

#endif // INCLUDE_CN1_AR

#pragma mark - IOSNative bridge functions
// Each `native ... cn1Ar*` declared on IOSNative.java has a matching C
// function below. Naming follows the standard ParparVM mangling:
//   com_codename1_impl_ios_IOSNative_<methodName>___<argType>..._R_<returnType>
// Sessions are referenced by their CN1AR Objective-C pointer cast to
// JAVA_LONG; we retain in cn1ArCreate and release in cn1ArClose. When AR
// support is compiled out (INCLUDE_CN1_AR undefined, or tvOS/watchOS) the
// bridge symbols still exist so ParparVM links cleanly -- they just return
// null/0 and the Java side reports AR as unsupported.

#if defined(INCLUDE_CN1_AR) && !TARGET_OS_TV && !TARGET_OS_WATCH
#define CN1AR_PEER(sessionPeer) ((__bridge CN1AR *)(void *)(sessionPeer))
#ifndef CN1_USE_ARC
#undef CN1AR_PEER
#define CN1AR_PEER(sessionPeer) ((CN1AR *)(sessionPeer))
#endif
#endif

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_cn1ArIsSupported___int_R_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT configType) {
#if defined(INCLUDE_CN1_AR) && !TARGET_OS_TV && !TARGET_OS_WATCH
    return [CN1AR isConfigSupported:configType] ? 1 : 0;
#else
    return 0;
#endif
}

JAVA_LONG com_codename1_impl_ios_IOSNative_cn1ArCreate___R_long(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#if defined(INCLUDE_CN1_AR) && !TARGET_OS_TV && !TARGET_OS_WATCH
    CN1AR *ar = [[CN1AR alloc] init];
#ifndef CN1_USE_ARC
    return (JAVA_LONG)[ar retain];
#else
    return (JAVA_LONG)(__bridge_retained void *)ar;
#endif
#else
    return 0;
#endif
}

void com_codename1_impl_ios_IOSNative_cn1ArAddReferenceImage___long_byte_1ARRAY_java_lang_String_float(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG sessionPeer,
        JAVA_OBJECT encodedImage, JAVA_OBJECT name, JAVA_FLOAT widthMeters) {
#if defined(INCLUDE_CN1_AR) && !TARGET_OS_TV && !TARGET_OS_WATCH
    JAVA_ARRAY arr = (JAVA_ARRAY) encodedImage;
    NSData *data = [NSData dataWithBytes:arr->data length:(NSUInteger) arr->length];
    NSString *nameStr = toNSString(CN1_THREAD_GET_STATE_PASS_ARG name);
    [CN1AR_PEER(sessionPeer) addReferenceImage:data name:nameStr width:widthMeters];
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_cn1ArStart___long_int_int_boolean_R_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG sessionPeer,
        JAVA_INT configType, JAVA_INT planeMask, JAVA_BOOLEAN lightEstimation) {
#if defined(INCLUDE_CN1_AR) && !TARGET_OS_TV && !TARGET_OS_WATCH
    return [CN1AR_PEER(sessionPeer) startWithConfigType:configType planeMask:planeMask
                                        lightEstimation:lightEstimation] ? 1 : 0;
#else
    return 0;
#endif
}

JAVA_LONG com_codename1_impl_ios_IOSNative_cn1ArCreateView___long_R_long(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG sessionPeer) {
#if defined(INCLUDE_CN1_AR) && !TARGET_OS_TV && !TARGET_OS_WATCH
    UIView *v = [CN1AR_PEER(sessionPeer) createView];
#ifndef CN1_USE_ARC
    return (JAVA_LONG)[v retain];
#else
    return (JAVA_LONG)(__bridge_retained void *)v;
#endif
#else
    return 0;
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_cn1ArHitTest___long_float_float_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG sessionPeer,
        JAVA_FLOAT xNorm, JAVA_FLOAT yNorm) {
#if defined(INCLUDE_CN1_AR) && !TARGET_OS_TV && !TARGET_OS_WATCH
    NSString *s = [CN1AR_PEER(sessionPeer) hitTestX:xNorm y:yNorm];
    return fromNSString(CN1_THREAD_GET_STATE_PASS_ARG s);
#else
    return JAVA_NULL;
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_cn1ArCreateAnchor___long_float_float_float_float_float_float_float_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG sessionPeer,
        JAVA_FLOAT tx, JAVA_FLOAT ty, JAVA_FLOAT tz,
        JAVA_FLOAT qx, JAVA_FLOAT qy, JAVA_FLOAT qz, JAVA_FLOAT qw) {
#if defined(INCLUDE_CN1_AR) && !TARGET_OS_TV && !TARGET_OS_WATCH
    NSString *s = [CN1AR_PEER(sessionPeer) createAnchorTx:tx ty:ty tz:tz
                                                       qx:qx qy:qy qz:qz qw:qw];
    return fromNSString(CN1_THREAD_GET_STATE_PASS_ARG s);
#else
    return JAVA_NULL;
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_cn1ArCreateAnchorFromHit___long_int_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG sessionPeer,
        JAVA_INT hitId) {
#if defined(INCLUDE_CN1_AR) && !TARGET_OS_TV && !TARGET_OS_WATCH
    NSString *s = [CN1AR_PEER(sessionPeer) createAnchorFromHit:hitId];
    if (s == nil) {
        return JAVA_NULL;
    }
    return fromNSString(CN1_THREAD_GET_STATE_PASS_ARG s);
#else
    return JAVA_NULL;
#endif
}

void com_codename1_impl_ios_IOSNative_cn1ArRemoveAnchor___long_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG sessionPeer,
        JAVA_OBJECT anchorId) {
#if defined(INCLUDE_CN1_AR) && !TARGET_OS_TV && !TARGET_OS_WATCH
    NSString *idStr = toNSString(CN1_THREAD_GET_STATE_PASS_ARG anchorId);
    [CN1AR_PEER(sessionPeer) removeAnchorById:idStr];
#endif
}

void com_codename1_impl_ios_IOSNative_cn1ArClearAnchorContent___long_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG sessionPeer,
        JAVA_OBJECT anchorId) {
#if defined(INCLUDE_CN1_AR) && !TARGET_OS_TV && !TARGET_OS_WATCH
    NSString *idStr = toNSString(CN1_THREAD_GET_STATE_PASS_ARG anchorId);
    [CN1AR_PEER(sessionPeer) clearAnchorContent:idStr];
#endif
}

void com_codename1_impl_ios_IOSNative_cn1ArAddAnchorMesh___long_java_lang_String_float_1ARRAY_int_int_1ARRAY_int_int_byte_1ARRAY_float_1ARRAY(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG sessionPeer,
        JAVA_OBJECT anchorId, JAVA_OBJECT interleaved, JAVA_INT vertexCount,
        JAVA_OBJECT indices, JAVA_INT indexCount, JAVA_INT argbColor,
        JAVA_OBJECT encodedTexture, JAVA_OBJECT localTransform16) {
#if defined(INCLUDE_CN1_AR) && !TARGET_OS_TV && !TARGET_OS_WATCH
    NSString *idStr = toNSString(CN1_THREAD_GET_STATE_PASS_ARG anchorId);
    JAVA_ARRAY vArr = (JAVA_ARRAY) interleaved;
    JAVA_ARRAY iArr = (JAVA_ARRAY) indices;
    JAVA_ARRAY tArr = (JAVA_ARRAY) localTransform16;
    NSData *texture = nil;
    if (encodedTexture != JAVA_NULL) {
        JAVA_ARRAY texArr = (JAVA_ARRAY) encodedTexture;
        texture = [NSData dataWithBytes:texArr->data length:(NSUInteger) texArr->length];
    }
    [CN1AR_PEER(sessionPeer) addAnchorMesh:idStr
                               interleaved:(const float *) vArr->data
                               vertexCount:vertexCount
                                   indices:(const int *) iArr->data
                                indexCount:indexCount
                                      argb:argbColor
                            encodedTexture:texture
                            localTransform:(const float *) tArr->data];
#endif
}

void com_codename1_impl_ios_IOSNative_cn1ArPause___long(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG sessionPeer) {
#if defined(INCLUDE_CN1_AR) && !TARGET_OS_TV && !TARGET_OS_WATCH
    [CN1AR_PEER(sessionPeer) pauseSession];
#endif
}

void com_codename1_impl_ios_IOSNative_cn1ArResume___long(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG sessionPeer) {
#if defined(INCLUDE_CN1_AR) && !TARGET_OS_TV && !TARGET_OS_WATCH
    [CN1AR_PEER(sessionPeer) resumeSession];
#endif
}

void com_codename1_impl_ios_IOSNative_cn1ArClose___long(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG sessionPeer) {
#if defined(INCLUDE_CN1_AR) && !TARGET_OS_TV && !TARGET_OS_WATCH
    if (sessionPeer == 0) {
        return;
    }
    [CN1AR_PEER(sessionPeer) closeSession];
#ifndef CN1_USE_ARC
    [CN1AR_PEER(sessionPeer) release];
#else
    CN1AR *ar = (__bridge_transfer CN1AR *)(void *) sessionPeer;
    ar = nil;
#endif
#endif
}
