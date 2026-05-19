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
#import <Foundation/Foundation.h>
#import "ExecutableOp.h"
#import "CN1ES2compat.h"

#ifdef CN1_USE_METAL

#import "CN1Metalcompat.h"

// ExecutableOp wrapper around CN1MetalFillGradient. Carries up to
// CN1_METAL_GRAD_MAX_STOPS premultiplied stops + geometry; ownership
// of the arrays passed to the initializer is copied into the op.
@interface DrawMultiStopGradient : ExecutableOp {
    int kind;
    int stopCount;
    float positions[CN1_METAL_GRAD_MAX_STOPS];
    float colors[CN1_METAL_GRAD_MAX_STOPS * 4];
    int cycleMethod;
    float angleOrFromAngle;
    float cx;
    float cy;
    float rx;
    float ry;
    int shape;
    int x;
    int y;
    int width;
    int height;
}
- (id)initWithKind:(int)kindA
         stopCount:(int)stopCountA
         positions:(const float *)positionsA
            colors:(const float *)colorsA
       cycleMethod:(int)cycleMethodA
  angleOrFromAngle:(float)angleOrFromAngleA
                cx:(float)cxA
                cy:(float)cyA
                rx:(float)rxA
                ry:(float)ryA
             shape:(int)shapeA
                 x:(int)xA
                 y:(int)yA
             width:(int)widthA
            height:(int)heightA;
- (void)execute;
@end

#endif
