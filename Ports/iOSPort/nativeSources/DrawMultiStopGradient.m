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
#import "DrawMultiStopGradient.h"

#ifdef CN1_USE_METAL

@implementation DrawMultiStopGradient

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
            height:(int)heightA {
    self = [super init];
    if (self == nil) return nil;
    kind = kindA;
    if (stopCountA < 2) stopCountA = 2;
    if (stopCountA > CN1_METAL_GRAD_MAX_STOPS) stopCountA = CN1_METAL_GRAD_MAX_STOPS;
    stopCount = stopCountA;
    for (int i = 0; i < stopCount; i++) {
        positions[i] = positionsA[i];
        colors[i * 4 + 0] = colorsA[i * 4 + 0];
        colors[i * 4 + 1] = colorsA[i * 4 + 1];
        colors[i * 4 + 2] = colorsA[i * 4 + 2];
        colors[i * 4 + 3] = colorsA[i * 4 + 3];
    }
    cycleMethod = cycleMethodA;
    angleOrFromAngle = angleOrFromAngleA;
    cx = cxA;
    cy = cyA;
    rx = rxA;
    ry = ryA;
    shape = shapeA;
    x = xA;
    y = yA;
    width = widthA;
    height = heightA;
    return self;
}

- (void)execute {
    CN1MetalFillGradient(kind, stopCount, positions, colors,
                         cycleMethod, angleOrFromAngle,
                         cx, cy, rx, ry, shape,
                         x, y, width, height);
}

- (NSString *)getName {
    return @"DrawMultiStopGradient";
}

@end

#endif
