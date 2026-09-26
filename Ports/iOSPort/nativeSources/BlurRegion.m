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
#import "BlurRegion.h"
#import "CodenameOne_GLViewController.h"
#import "GLUIImage.h"
#include "TargetConditionals.h"
#ifdef CN1_USE_METAL
#import "METALView.h"
#endif

@implementation BlurRegion

-(id)initWithArgs:(int)xpos ypos:(int)ypos w:(int)w h:(int)h r:(float)r {
    x = xpos;
    y = ypos;
    width = w;
    height = h;
    radius = r;
    glass = NO;
    return self;
}

-(id)initWithGlassArgs:(int)xpos ypos:(int)ypos w:(int)w h:(int)h r:(float)r
          cornerRadius:(float)cr sat:(float)st scale:(float)sc offset:(float)of
               refract:(float)rf specular:(float)sp curve:(float)cv curveMid:(float)cm outline:(float)ol {
    x = xpos;
    y = ypos;
    width = w;
    height = h;
    radius = r;
    glass = YES;
    cornerRadius = cr;
    sat = st;
    scale = sc;
    offset = of;
    refract = rf;
    specular = sp;
    curve = cv;
    curveMid = cm;
    outline = ol;
    return self;
}

-(id)initWithLensArgs:(int)xpos ypos:(int)ypos w:(int)w h:(int)h
         cornerRadius:(float)cr magnify:(float)mg aberration:(float)ab
            tintColor:(int)tc tintStrength:(float)ts {
    x = xpos;
    y = ypos;
    width = w;
    height = h;
    glass = NO;
    lens = YES;
    cornerRadius = cr;
    magnify = mg;
    aberration = ab;
    tintColor = tc;
    tintStrength = ts;
    return self;
}

-(id)initWithGlassLensArgs:(int)xpos ypos:(int)ypos w:(int)w h:(int)h cornerRadius:(float)cr
                     optics:(const float*)o count:(int)count amount:(float)am {
    x = xpos;
    y = ypos;
    width = w;
    height = h;
    glass = NO;
    lens = NO;
    colorMatrix = NO;
    glassLens = YES;
    for (int i = 0; i < 16; i++) {
        optics[i] = i < count ? o[i] : 0.0f;
    }
    cornerRadius = cr;
    amount = am;
    return self;
}

-(id)initWithColorMatrixArgs:(int)xpos ypos:(int)ypos w:(int)w h:(int)h
                      matrix:(const float*)m mask:(GLUIImage*)mask
                cornerRadius:(float)cr amount:(float)am {
    x = xpos;
    y = ypos;
    width = w;
    height = h;
    glass = NO;
    lens = NO;
    colorMatrix = YES;
    for (int i = 0; i < 12; i++) {
        matrix[i] = m[i];
    }
    maskImage = mask;
#ifndef CN1_USE_ARC
    [maskImage retain];
#endif
    cornerRadius = cr;
    amount = am;
    return self;
}

-(void)execute {
#if defined(CN1_USE_METAL) && !TARGET_OS_WATCH
    id view = [[CodenameOne_GLViewController instance] renderingView];
    if ([view isKindOfClass:[METALView class]]) {
        if (glassLens) {
            [(METALView*)view glassLensScreenRegionX:x y:y w:width h:height cornerRadius:cornerRadius
                                              optics:optics amount:amount];
        } else if (colorMatrix) {
            [(METALView*)view colorMatrixScreenRegionX:x y:y w:width h:height matrix:matrix
                                                 mask:(maskImage == nil ? nil : [maskImage getMTLTexture])
                                         cornerRadius:cornerRadius amount:amount];
        } else if (lens) {
            [(METALView*)view lensScreenRegionX:x y:y w:width h:height
                                  cornerRadius:cornerRadius magnify:magnify
                                   aberration:aberration tintColor:tintColor tintStrength:tintStrength];
        } else if (glass) {
            [(METALView*)view glassScreenRegionX:x y:y w:width h:height radius:radius
                                   cornerRadius:cornerRadius sat:sat scale:scale
                                         offset:offset refract:refract specular:specular
                                          curve:curve curveMid:curveMid outline:outline];
        } else {
            [(METALView*)view blurScreenRegionX:x y:y w:width h:height radius:radius];
        }
    }
#endif
    // GL / watchOS: no live-screen blur (the component still paints its
    // translucent fill, just without the backdrop blur).
}

-(NSString*)getName {
    return @"BlurRegion";
}

#ifndef CN1_USE_ARC
-(void)dealloc {
    [maskImage release];
    [super dealloc];
}
#endif

@end
