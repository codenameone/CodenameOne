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

// Native side of the com.codename1.ar API: an ARKit ARSession composited
// through an ARSCNView (SceneKit renders the camera background, syncs anchor
// transforms and applies the light estimate automatically). Anchored content
// arrives from Java as raw interleaved vertex buffers (parsed from glTF or
// built by com.codename1.gpu) and becomes SCNGeometry.
//
// The whole class is gated on INCLUDE_CN1_AR (uncommented by IPhoneBuilder
// only for apps that reference com.codename1.ar) and compiled out on
// tvOS/watchOS where ARKit does not exist. The IOSNative bridge symbols in
// CN1AR.m exist unconditionally so linking always succeeds.

#import "CodenameOne_GLViewController.h"

#if defined(INCLUDE_CN1_AR) && !TARGET_OS_TV && !TARGET_OS_WATCH

#import <UIKit/UIKit.h>
#import <ARKit/ARKit.h>
#import <SceneKit/SceneKit.h>

@interface CN1AR : NSObject <ARSCNViewDelegate, ARSessionDelegate>

@property (nonatomic, strong) ARSCNView *sceneView;
@property (nonatomic, strong) NSMutableArray<ARReferenceImage *> *referenceImages;
// anchorId (UUID string) -> the SCNNode ARSCNView created for that anchor.
@property (nonatomic, strong) NSMutableDictionary<NSString *, SCNNode *> *anchorNodes;
// anchorId -> the content container node built from Java meshes.
@property (nonatomic, strong) NSMutableDictionary<NSString *, SCNNode *> *contentNodes;
// Recent raycast results kept alive so createAnchorFromHit can anchor to the
// exact native hit. Cleared on every new hit test.
@property (nonatomic, strong) NSMutableDictionary<NSNumber *, ARRaycastResult *> *recentHits;
@property (nonatomic, assign) int nextHitId;
@property (nonatomic, assign) int configType;
@property (nonatomic, assign) int planeMask;
@property (nonatomic, assign) BOOL lightEstimation;
@property (nonatomic, assign) int frameCounter;
@property (nonatomic, assign) BOOL closed;

- (void)addReferenceImage:(NSData *)encoded name:(NSString *)name width:(float)widthMeters;
- (BOOL)startWithConfigType:(int)configType planeMask:(int)planeMask
            lightEstimation:(BOOL)lightEstimation;
- (UIView *)createView;
- (NSString *)hitTestX:(float)xNorm y:(float)yNorm;
- (NSString *)createAnchorTx:(float)tx ty:(float)ty tz:(float)tz
                          qx:(float)qx qy:(float)qy qz:(float)qz qw:(float)qw;
- (NSString *)createAnchorFromHit:(int)hitId;
- (void)removeAnchorById:(NSString *)anchorId;
- (void)clearAnchorContent:(NSString *)anchorId;
- (void)addAnchorMesh:(NSString *)anchorId
          interleaved:(const float *)interleaved
          vertexCount:(int)vertexCount
              indices:(const int *)indices
           indexCount:(int)indexCount
                 argb:(int)argb
       encodedTexture:(NSData *)encodedTexture
       localTransform:(const float *)localTransform16;
- (void)pauseSession;
- (void)resumeSession;
- (void)closeSession;

@end

#endif // INCLUDE_CN1_AR
